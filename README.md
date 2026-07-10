# Swimming DSL — Desktop App

Desktop application for designing, analyzing, and optimizing swimming training sessions using a purpose-built domain-specific language, parsed by Rascal and backed by three AI agents running on Gemini 2.5 Flash.

## Table of contents

- [Requirements](#requirements)
- [Setup](#setup)
- [Running the app](#running-the-app)
- [The swimming DSL](#the-swimming-dsl)
- [System architecture](#system-architecture)
- [The three AI agents](#the-three-ai-agents)
- [Core services](#core-services)
- [Project structure](#project-structure)
- [App screens](#app-screens)
- [Persisted data](#persisted-data)
- [Roadmap](#roadmap)

## Requirements

| Tool | Minimum version |
|---|---|
| Java (JDK) | 17+ |
| Gradle | bundled via the wrapper |
| Gemini API key | [Get one here](https://aistudio.google.com/app/apikey) |

`rascal-shell-stable.jar` and the `.rsc` source files must live in `../` relative to `kotlin-app/` (that is, at the root of `swimmingdsl/`).

## Setup

Before running the app, export your Gemini API key:

```bash
export GEMINI_API_KEY="your_api_key_here"
```

Without this variable, the app starts normally, but all three AI agents will fail as soon as they are used.

## Running the app

```bash
cd kotlin-app
./gradlew run
```

Or with a clean build first:

```bash
./gradlew clean run
```

## The swimming DSL

The DSL describes training sessions in a structured, declarative way. It is parsed by Rascal against a context-free grammar and analyzed to extract training metrics.

### Basic structure

```
session <name> {
  <blocks>
}
```

### With structured sections (warmup / main / cooldown)

```
session <name> {
  warmup {
    <blocks>
  }
  main {
    <blocks>
  }
  cooldown {
    <blocks>
  }
}
```

### Exercise types

```
# Swimming
swim <distance> m [<style>] [<intensity>] [pace <number>] [with <equipment>] [target <min>:<sec>]

# Kicking
kick <distance> m [<intensity>] [with <equipment>]

# Technique drill
drill <drillType> <distance> m [<intensity>]
```

### Intervals

```
<reps> x <exercise> rest <seconds> s
```

### Allowed values

| Category | Values |
|---|---|
| **Styles** | `freestyle` `backstroke` `breaststroke` `butterfly` |
| **Intensities** | `easy` `moderate` `hard` |
| **Equipment** | `fins` `paddles` `board` `pullbuoy` `snorkel` |
| **Drill types** | `catchup` `onesided` `fingertip` `sixKick` `sculling` |
| **Pace** | integer (seconds per 100m) |
| **Rest** | integer (seconds) |

### Full example

```
session week1_tuesday {
  warmup {
    swim 400 m freestyle easy pace 120
    2 x drill catchup 50 m easy rest 15 s
  }
  main {
    8 x swim 100 m freestyle hard pace 75 rest 15 s
    4 x swim 200 m backstroke moderate pace 110 rest 30 s
    3 x kick 50 m hard with fins rest 20 s
  }
  cooldown {
    swim 300 m easy pace 140
  }
}
```

## System architecture

```
SwimmingDslApp (Compose Desktop)

  RascalService          parses and analyzes DSL code through the Rascal jar
                          spawns one Java process per call

  LLMService              HTTP client for the Gemini 2.5 Flash API

  DSLTranslatorAgent      natural language to valid DSL, with automatic retries
  CoachAgent              conversational chat with live analysis context
  OptimizerAgent          progressive multi-week training plans
```

### Analysis flow (doAnalyze)

When the user presses "Analyze":

```
DSL code (string)
      down to
RascalService.analyze()
      down to
Runner.rsc analyze <code>       Rascal process
      down to
WebAPI::analyzeToJSON()          grammar plus regex extraction in Rascal
      down to
JSON with metrics:
  { success, totalDistance, distanceKm, sessionCount,
    sessionNames, styles, intensities, equipment, drills,
    rest: { periods, totalSeconds, average },
    time: { swimSeconds, restSeconds, totalSeconds } }
      down to
AnalysisResult (Kotlin data class)
      down to
UI update, history persisted to ~/.swimmingdsl/history.json
```

## The three AI agents

### 1. DSLTranslatorAgent — natural language translator

**Purpose:** turn a natural-language description into syntactically valid DSL code.

**Logic (validation loop with retries):**

```
translate(userRequest):
  for attempt in 1..3:
    1. LLM generates DSL (temperature=0.2, low randomness)
    2. rascalService.analyze(dslCode)   validates against the real parser
    3. success?  return dslCode
       error?     append to the chat history:
                  "This code produced the error: <X>. Fix it."
                  retry with the error as context
  if all 3 attempts fail: return an error with the last generated code
```

**Why it works:** the LLM receives its own Rascal error and can self-correct on the next iteration. The low temperature (0.2) reduces randomness to produce precise, structured code. The system prompt embeds the full grammar with valid examples and asks for raw code only, no markdown.

### 2. CoachAgent — conversational personal coach

**Purpose:** a chat interface with a coaching persona that has access to the current session's analysis.

**Logic (chat with dynamic context):**

```
chat(userMessage, analysisResult, currentCode):
  1. Build a dynamic system prompt:
       BASE_PROMPT (role, guidance by level: beginner/intermediate/advanced)
       + the current Rascal analysis injected as text:
         distance, styles, intensities, times, rest, equipment
       + the current DSL code from the editor
  2. Append userMessage to conversationHistory
  3. llmService.chat(systemPrompt, conversationHistory)
  4. Append the response to history, preserving context across turns
  5. On error: removeLastOrNull() from history so the conversation is not corrupted
```

**Why it works:** the Rascal analysis is injected as context on every call, giving the LLM concrete, non-inferred data to give specific feedback. The in-memory history keeps multi-turn conversations coherent.

**State:** the history lives for as long as the app is open, and is cleared with `resetConversation()`.

### 3. OptimizerAgent — multi-week training planner

**Purpose:** generate progressive plans of N weeks with M sessions per week, each validated and analyzed by Rascal.

**Logic (progressive generation with a two-tier fallback):**

```
optimize(config, onProgress):
  rascalCallCount = 0

  for week in 1..weeks:
    for session in 1..sessionsPerWeek:

      # Automatic progression: +7% distance per week
      progressionFactor = 1.0 + (week - 1) * 0.07
      targetDistance = baseDistance * progressionFactor

      # Contextual prompt with history
      prompt includes:
        - goal, week/total, session/total
        - targetDistance, maxMinutes, styles
        - previous session's distance, to keep progression coherent

      # Primary attempt: LLM generates DSL
      dslCode = llmService.chat(SYSTEM_PROMPT, [prompt])
      analysis = rascalService.analyze(dslCode)    rascalCallCount++

      if analysis.success:
        sessions.add(SessionPlan(week, sessionNum, dslCode, analysis))
      else:
        # Fallback tier 1: Rascal's native generator
        fallback = rascalService.generate(goal, targetDistance, styles, maxMinutes)
        adjustedCode = adjustGeneratedDistance(fallback.code, targetDistance)
        fallbackAnalysis = rascalService.analyze(adjustedCode)    rascalCallCount++
        if fallbackAnalysis.success: sessions.add(...)

      # Resource guard
      if rascalCallCount >= 10:
        return OptimizationResult(success=true, sessions=partial)

  return OptimizationResult(success=true, sessions=complete)
```

**Key details:**

- **+7% per week:** `progressionFactor = 1.0 + (week-1) * 0.07`. Week 1 = 100%, week 4 = 121%, week 8 = 149%.
- **Cumulative context:** the LLM receives the previous session's distance to keep progression coherent.
- **`adjustGeneratedDistance`:** when Rascal generates code whose distances are affected by integer division, this utility recomputes the real distance of the DSL and adjusts the last `swim N m` block to compensate for the difference.
- **10-call limit:** avoids excessive wait times on long plans. If reached, the partial plan generated so far is returned.

## Core services

### `LLMService`

An HTTP client (Ktor with the CIO engine) that talks to Gemini 2.5 Flash.

```
chat(systemPrompt, messages, temperature=0.7):
  POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent
  Headers: Content-Type: application/json
  Body:
    systemInstruction: { parts: [{ text: systemPrompt }] }
    contents: [ { role: "user"|"model", parts: [{ text }] }, ... ]
    generationConfig: { maxOutputTokens: 8192, temperature }
  returns the text of the first candidate
```

- API key read from `GEMINI_API_KEY` (environment variable)
- Timeout: 60 seconds
- If `finishReason == "MAX_TOKENS"`: logs a warning (truncated response)
- If the response contains an `"error"` key: throws with the API's own message

### `RascalService`

Manages execution of the Rascal parser. Each call spawns a dedicated Java process.

```
executeRascalProcess(command, args):
  java -jar rascal-shell-stable.jar Runner.rsc <command> <args...>
  timeout: 30s
  roughly 5-6 seconds per call (JVM startup plus interpretation)
```

**JSON extraction:** `extractJson()` scans stdout for the valid JSON object that contains a `"success"` key, ignoring any extra progress output printed by Rascal.

**Escaping:** `escapeForRascal()` escapes `\`, `"`, `\n`, `\r`, `\t`, `<`, and `>` before embedding the DSL code inside a Rascal expression (`<>` is string interpolation syntax in Rascal).

## Project structure

```
swimmingdsl/

  rascal-shell-stable.jar          Rascal runtime (v0.40.17)

  src/                             Rascal modules for the DSL
    Lexer.rsc                      tokens: INT, ID, keywords, whitespace layout
    SwimSyntax.rsc                 the full DSL grammar
    AST.rsc                        data types: Program, Session, Block, Exercise...
    Semantics.rsc                  distance/time calculations, session generators
    WebAPI.rsc                     analyzeToJSON + generateToJSON (main interface)
    Runner.rsc                     CLI entry point (invoked per call)

  kotlin-app/
    src/main/kotlin/swimming/
      Main.kt                      entry point, SwimmingDslApp, AppTab enum
      agent/
        DSLTranslatorAgent.kt      NL to DSL with automatic retries
        CoachAgent.kt              conversational chat with context
        OptimizerAgent.kt          progressive multi-week plans
      service/
        LLMService.kt              Gemini API client
        RascalService.kt           Rascal interface (process per call)
      model/
        AnalysisResult.kt          AnalysisResult, GenerateResult, TimeInfo, RestInfo
        UserProfile.kt             level, preferred styles, available minutes
      util/
        DslDistanceAdjuster.kt     adjusts distances in Rascal-generated DSL
      ui/
        DashboardPanel.kt          history and overall statistics
        EditorPanel.kt             DSL code editor
        TranslatorPanel.kt         UI for DSLTranslatorAgent
        CoachPanel.kt              UI for CoachAgent (chat)
        OptimizerPanel.kt          UI for OptimizerAgent
        AnalysisPanel.kt           Rascal metrics panel (right sidebar)
        OnboardingScreen.kt        initial profile setup screen
        SidebarNav.kt              side navigation
```

## App screens

| Screen | Agent / service | Description |
|---|---|---|
| **Dashboard** | none | Session history, overall statistics, profile |
| **Editor** | RascalService | DSL editor with manual analysis |
| **AI Translator** | DSLTranslatorAgent | Natural-language description to valid DSL |
| **AI Coach** | CoachAgent | Chat with a coach that knows the current session |
| **AI Optimizer** | OptimizerAgent | Generates a full multi-week plan |

On every tab except Dashboard, the Rascal analysis panel is shown on the right, displaying distance, styles, intensities, times, and rest for the current session.

## Persisted data

```
~/.swimmingdsl/
  profile.json     user profile (level, preferred styles, available minutes)
  history.json     list of AnalysisResult entries for successfully analyzed sessions
```

The profile is created during initial onboarding. If the file does not exist on startup, the welcome screen is shown to configure it.

## Roadmap

- Export to JSON/CSV
- Chart visualizations
- Calorie estimation
- Training Stress Score
- Advanced validation (warnings, not just hard errors)
- Session comparison
- Export to device formats (TCX/FIT)
- Long-term training history
