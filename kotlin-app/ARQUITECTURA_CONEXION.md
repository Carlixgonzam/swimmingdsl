# Connection Architecture: Kotlin Compose to Rascal

## Index

1. [Overview](#overview)
2. [What is Rascal and how does it run?](#what-is-rascal-and-how-does-it-run)
3. [What is Kotlin Compose for Desktop?](#what-is-kotlin-compose-for-desktop)
4. [Connection mechanism: process-based communication](#connection-mechanism-process-based-communication)
5. [Detailed flow: code analysis](#detailed-flow-code-analysis)
6. [Detailed flow: session generation](#detailed-flow-session-generation)
7. [Extracting JSON from Rascal's output](#extracting-json-from-rascals-output)
8. [Deserialization: from JSON to Kotlin objects](#deserialization-from-json-to-kotlin-objects)
9. [Handling concurrency](#handling-concurrency)
10. [Post-generation distance adjustment](#post-generation-distance-adjustment)
11. [Comparison with the web version](#comparison-with-the-web-version)
12. [Full data flow diagram](#full-data-flow-diagram)

## Overview

The Kotlin Compose desktop application talks to the Rascal backend through **Inter-Process Communication (IPC)**. There is no network connection (HTTP) involved, and no Rascal classes are imported directly into Kotlin. Instead, Kotlin launches an **independent JVM process** that runs Rascal, passes it arguments on the command line, reads the JSON response Rascal prints to **stdout**, and deserializes it into Kotlin objects.

```
   JVM process #1                          JVM process #2

   Kotlin/Compose            spawn          Rascal Shell
   (user interface)      ----------->       (interprets .rsc files)

   RascalService              args[]
   (reads the response)  ----------->

                              stdout
                        <-----------        WebAPI.rsc
                                             (prints JSON)

   The user's app                           Created and destroyed
   (always running)                         for every operation
```

## What is Rascal and how does it run?

**Rascal** is a metaprogramming language designed for analyzing and transforming programs. It runs on the JVM through an interpreter packaged in `rascal-shell-stable.jar`.

Rascal is **not** a conventional Java/Kotlin library. It does not expose public classes with an API you can `import`. It is an interpreted language: its jar contains the interpreter that reads and executes `.rsc` files.

```bash
java -Drascal.projectPath=/path/to/swimmingdsl/src \
     -jar rascal-shell-stable.jar \
     Runner.rsc \
     analyze \
     "session morning { swim 400 m freestyle easy pace 120 }"
```

Rascal interprets `Runner.rsc`, which delegates to `WebAPI.rsc`, runs the corresponding function (`analyzeToJSON` or `generateToJSON`), and prints the result as JSON to stdout.

### Rascal files in the project

| File | Responsibility |
|---|---|
| `Runner.rsc` | Entry point, delegates to WebAPI |
| `WebAPI.rsc` | CLI with `analyze` and `generate` commands, JSON conversion |
| `SwimSyntax.rsc` | DSL grammar (defines what is valid syntax) |
| `AST.rsc` | Algebraic data types (Session, Block, Exercise, etc.) |
| `Semantics.rsc` | Distance/time calculations and session generators |

## What is Kotlin Compose for Desktop?

**Jetpack Compose for Desktop** is JetBrains' declarative UI framework for building desktop applications with Kotlin. It runs on the JVM, which means our desktop application is itself a Java process.

### Key project dependencies

- **Compose Desktop**: UI framework (`org.jetbrains.compose`)
- **kotlinx-serialization-json**: deserializes JSON into data classes
- **kotlinx-coroutines**: asynchronous programming so the UI never blocks

## Connection mechanism: process-based communication

`RascalService.kt` is the bridge between Kotlin and Rascal. It uses Java's `ProcessBuilder` class to spawn child processes.

### What is ProcessBuilder?

`ProcessBuilder` is a standard Java class (`java.lang.ProcessBuilder`) that lets you:

1. **Create an operating-system process** (as if running a command in a terminal)
2. **Pass arguments** to that process
3. **Read its standard output (stdout) and standard error (stderr)**
4. **Wait** for the process to finish
5. **Read its exit code** (0 means success)

### Building the command

```kotlin
private fun executeRascal(command: String, args: List<String>): String {
    val shellCmd = listOf(
        "java",                                              // executable: the JVM
        "-Dfile.encoding=UTF-8",                             // UTF-8 encoding
        "-Drascal.projectPath=${srcDir.absolutePath}",       // property: where the .rsc files are
        "-jar", rascalJar.absolutePath,                      // the Rascal jar
        "Runner.rsc",                                        // entry module
        command                                              // "analyze" or "generate"
    ) + args                                                 // additional arguments

    val process = ProcessBuilder(shellCmd)
        .directory(srcDir)           // working directory
        .redirectErrorStream(false)  // keep stderr separate
        .start()                     // launch the process

    process.outputStream.close()     // close stdin, we don't send any input

    val stdout = process.inputStream.bufferedReader().readText()  // read all of stdout
    val stderr = process.errorStream.bufferedReader().readText()  // read any errors

    val finished = process.waitFor(30, TimeUnit.SECONDS)  // wait at most 30 seconds
    if (!finished) {
        process.destroyForcibly()  // kill it if it hangs
        throw RuntimeException("Rascal timeout (30s)")
    }

    return stdout  // return the output so the JSON can be extracted
}
```

### What happens at the operating-system level

1. `ProcessBuilder.start()` triggers the OS to create a new process (fork plus exec on Unix)
2. That process runs `java -jar rascal-shell-stable.jar ...`
3. This starts a **second, fully independent JVM instance**
4. Rascal loads inside that JVM, interprets the `.rsc` files, and runs the requested command
5. The result is printed to stdout
6. The process exits (the second JVM is destroyed)
7. Kotlin reads stdout and continues

### Path resolution

`RascalService` locates the files it needs automatically:

```
swimmingdsl/                          projectRoot
  rascal-shell-stable.jar             rascalJar
  src/                                srcDir, Rascal's working directory
    Runner.rsc
    WebAPI.rsc
    SwimSyntax.rsc
    AST.rsc
    Semantics.rsc
  kotlin-app/                         the app runs from here
    src/main/kotlin/swimming/
      service/RascalService.kt
```

Resolution is relative: the Kotlin app runs from `kotlin-app/`, and `RascalService` walks up one level (`..`) to find the jar and the `src/` directory.

## Detailed flow: code analysis

When the user writes DSL code and presses "Analyze":

### Step 1: the UI triggers the action

```
EditorPanel.kt -> "Analyze" button -> onAnalyze(code)
Main.kt -> doAnalyze(code) -> launches a coroutine
```

### Step 2: Kotlin invokes Rascal

```kotlin
// RascalService.kt
suspend fun analyze(code: String): AnalysisResult = withContext(Dispatchers.IO) {
    val output = executeRascal("analyze", listOf(code))
    // ...
}
```

Which runs:

```bash
java -Dfile.encoding=UTF-8 \
     -Drascal.projectPath=/path/to/swimmingdsl/src \
     -jar /path/to/swimmingdsl/rascal-shell-stable.jar \
     Runner.rsc \
     analyze \
     "session morning { swim 400 m freestyle easy pace 120 }"
```

### Step 3: Rascal processes the code

Inside the second JVM:

```
Runner.rsc -> main(args)
  -> WebAPI.rsc -> main(["analyze", "session morning {...}"])
    -> analyzeToJSON(code)
      -> parse(#start[Program], code)     parses against the grammar
      -> extracts metrics with regular expressions:
          distance:  /<d:[0-9]+>\s*m/
          intervals: /<reps:[0-9]+>\s*x\s*\w+\s*<d:[0-9]+>\s*m/
          styles, intensities, equipment, drills, rest, time
      -> builds a result map
      -> toJSON(result)                    serializes to JSON
      -> println(jsonStr)                  prints to stdout
```

### Step 4: Rascal prints JSON to stdout

```json
{"success":true,"sessionCount":1,"sessionNames":["morning"],"totalDistance":400,
 "distanceKm":0.40,"styles":{"freestyle":1},"intensities":{"easy":1},
 "equipment":{},"drills":{},"rest":{"periods":0,"totalSeconds":0,"average":0},
 "time":{"swimSeconds":480,"restSeconds":0,"totalSeconds":480}}
```

### Step 5: Kotlin extracts and deserializes it

```
stdout (a string that may contain extra Rascal output)
  -> extractJson(output)      finds the valid JSON
  -> Json.decodeFromString<AnalysisResult>(jsonStr)
  -> AnalysisResult(success=true, totalDistance=400, ...)
```

### Step 6: the UI updates

```
AnalysisResult -> Main.kt updates state
  -> AnalysisPanel.kt recomposes
  -> shows: 400 meters, 1 session, 8:00 total time, etc.
```

## Detailed flow: session generation

When the user sets parameters and presses "Generate Session":

### Step 1: the UI collects the parameters

```
GeneratorPanel.kt:
  goal = "speed"
  distance = 2500
  styles = ["freestyle", "backstroke", "breaststroke", "butterfly"]
  duration = 60
```

### Step 2: Kotlin invokes Rascal

```bash
java -jar rascal-shell-stable.jar Runner.rsc generate speed 2500 freestyle,backstroke,breaststroke,butterfly 60
```

### Step 3: Rascal generates the session

```
WebAPI.rsc -> generateToJSON("speed", 2500, ["freestyle",...], 60)
  -> builds a GeneratorConfig
  -> Semantics.rsc -> generateSpeedSession(config)
    -> warmupDist = (2500 * 20) / 100 = 500
    -> mainDist = (2500 * 60) / 100 = 1500
    -> distPerStyle = 1500 / 4 = 375   integer division
    -> repsPerStyle = 375 / 50 = 7     integer division, loses 25m x 4 = 100m
    -> cooldownDist = (2500 * 20) / 100 = 500
    -> returns the AST: structuredSession("generated_speed", [warmup, main, cooldown])
  -> sessionToDSL(session)    converts the AST back to DSL code
  -> toJSON(result)            JSON with the generated code
  -> println(jsonStr)
```

### Step 4: Rascal returns JSON

```json
{
  "success": true,
  "code": "session generated_speed {\n  warmup {\n    swim 500 m easy pace 120\n  }\n  main {\n    7 x swim 50 m freestyle hard pace 60 rest 45 s\n    7 x swim 50 m backstroke hard pace 60 rest 45 s\n    7 x swim 50 m breaststroke hard pace 60 rest 45 s\n    7 x swim 50 m butterfly hard pace 60 rest 45 s\n  }\n  cooldown {\n    swim 500 m easy pace 130\n  }\n}",
  "goal": "speed",
  "distance": 2500
}
```

### Step 5: Kotlin adjusts the distance

```kotlin
// GeneratorPanel.kt
val adjustedCode = adjustGeneratedDistance(result.code, dist)
// detects: the code adds up to 2400m, 2500m were requested, a 100m gap
// adjusts the cooldown: swim 500 m -> swim 600 m
// new total: 500 + 1400 + 600 = 2500m
```

### Step 6: automatic analysis runs

```
onCodeGenerated(adjustedCode)   places the adjusted code in the editor
onAnalyze(adjustedCode)         runs analysis (repeats the analysis flow)
                                 Rascal confirms: totalDistance = 2500
```

## Extracting JSON from Rascal's output

Rascal's stdout is not pure JSON. It can contain interpreter messages, warnings, ANSI color codes, and so on. This is why an algorithm is needed to **find the valid JSON** inside that output.

### The `extractJson()` algorithm

```
1. Strip ANSI escape codes (terminal colors)
2. Scan the string looking for '{'
3. For each '{' found:
   a. Track brace depth (handles nesting)
   b. Respect string literals (don't count braces inside "...")
   c. Handle escaped characters inside strings
   d. When depth reaches 0, a JSON candidate has been found
4. Try to parse the candidate as JSON
5. If it has a "success" field, it is our result, keep it
6. Keep scanning (there may be multiple JSON objects)
7. Return the LAST valid JSON object that has a "success" field
```

This algorithm is identical to the one used in the web version's `server.js`, translated from JavaScript to Kotlin.

## Deserialization: from JSON to Kotlin objects

`kotlinx-serialization-json` converts the JSON into Kotlin data classes.

### Serializable data classes

```kotlin
@Serializable
data class AnalysisResult(
    val success: Boolean = false,
    val error: String? = null,
    val sessionCount: Int = 0,
    val sessionNames: List<String> = emptyList(),
    val totalDistance: Int = 0,
    val distanceKm: Double = 0.0,
    val styles: Map<String, Int> = emptyMap(),
    val intensities: Map<String, Int> = emptyMap(),
    val equipment: Map<String, Int> = emptyMap(),
    val drills: Map<String, Int> = emptyMap(),
    val rest: RestInfo = RestInfo(),
    val time: TimeInfo = TimeInfo()
)

@Serializable
data class GenerateResult(
    val success: Boolean = false,
    val error: String? = null,
    val code: String? = null,
    val goal: String? = null,
    val distance: Int = 0
)
```

The `@Serializable` annotation lets `Json.decodeFromString<T>()` convert the JSON into instances of these classes automatically. `ignoreUnknownKeys = true` keeps this working even if Rascal adds new fields to the JSON later.

## Handling concurrency

Running Rascal takes a few seconds (JVM startup plus interpretation). To keep the UI from freezing:

### Kotlin coroutines

```kotlin
// Main.kt, launches in the background
val doAnalyze: (String) -> Unit = { code ->
    scope.launch {                                    // coroutine in the Compose scope
        isLoading = true
        analysisResult = rascalService.analyze(code)  // suspends here
        isLoading = false
    }
}

// RascalService.kt, runs on the IO dispatcher
suspend fun analyze(code: String): AnalysisResult =
    withContext(Dispatchers.IO) {            // switches to the IO thread pool
        // ProcessBuilder, reading stdout, etc.
        // this code runs on a background thread
    }
```

- `Dispatchers.IO` moves execution to a thread pool tuned for blocking operations (I/O, processes)
- The UI keeps responding while Rascal is working
- When the coroutine finishes, state updates and Compose recomposes the UI automatically

## Post-generation distance adjustment

Rascal uses **integer division** when distributing distance across styles, which can lose a few meters along the way. The `adjustGeneratedDistance()` function in Kotlin compensates for this.

### Concrete example: speed, 2500m, 4 styles

```
Rascal generates:
  mainDist = 2500 * 60% = 1500
  distPerStyle = 1500 / 4 = 375     exact division
  repsPerStyle = 375 / 50 = 7       loses 375 - (7*50) = 25m per style
  total loss: 25 * 4 = 100m

Generated code adds up to: 500 + (7*50*4) + 500 = 2400m (100m short)
```

### Adjustment algorithm (in Kotlin)

```
1. Compute the code's distance with the SAME regular expressions Rascal uses:
   - sum every "N m"
   - for every "N x WORD N m", add (N-1) * distance
   result: 2400

2. Difference: 2500 - 2400 = 100m

3. Find the last "swim N m" block (the cooldown)
   "swim 500 m"

4. Adjust it: 500 + 100 = 600
   "swim 600 m"

5. Adjusted total: 500 + 1400 + 600 = 2500m
```

This correction happens entirely in Kotlin and never touches the Rascal backend.

## Comparison with the web version

Both versions use the **same strategy** to talk to Rascal (external process plus stdout), through a different intermediate layer.

### Web version

```
Browser (HTML/JS)
    down to  HTTP (fetch)
Node.js / Express (server.js)          process #1
    down to  child_process.spawn("java", [...])
JVM + Rascal                           process #2
    down to  stdout -> JSON
Node.js parses the JSON, adds formatted time fields
    down to  HTTP response
Browser renders the results
```

### Kotlin desktop version

```
Kotlin Compose (JVM)                   process #1
    down to  ProcessBuilder("java", [...])
JVM + Rascal                           process #2
    down to  stdout -> JSON
Kotlin extracts the JSON, deserializes it, adjusts the distance
    down to  state update (recomposition)
Compose renders the results
```

### Key differences

- **Web**: needs an HTTP server running (`node server.js`) as an intermediary
- **Desktop**: talks to Rascal directly, no intermediate server
- **Web**: time formatting happens on the Node.js server
- **Desktop**: time formatting happens through computed properties on the Kotlin data classes
- **Desktop**: includes post-generation distance adjustment, which the web version does not have

## Full data flow diagram

```
                          USER

              writes code and presses         sets generation parameters
              "Analyze"                       and presses "Generate Session"

              EditorPanel.kt                  GeneratorPanel.kt

                                               RascalService.generate()

                                               ProcessBuilder:
                                               java -jar rascal-shell-stable.jar
                                               Runner.rsc generate
                                               speed 2500 freestyle,... 60

                                               stdout: JSON {code, goal, distance}

                                               extractJson()
                                               deserialize -> GenerateResult

                                               adjustGeneratedDistance()
                                               (compensates for integer division)

                                               adjusted code
                                               onCodeGenerated()
                                               (placed in the editor)

              RascalService.analyze()

              ProcessBuilder:
              java -jar rascal-shell-stable.jar
              Runner.rsc analyze "session ... {}"

              stdout: JSON {totalDistance, styles, time, ...}

              extractJson()
              deserialize -> AnalysisResult

              AnalysisPanel.kt
              (renders the results)
```
