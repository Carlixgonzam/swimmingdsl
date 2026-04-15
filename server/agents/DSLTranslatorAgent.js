const LLMService = require('../services/LLMService');
const RascalService = require('../services/RascalService');

const MAX_RETRIES = 3;

const SYSTEM_PROMPT = `You are a Swimming DSL code generator. You receive a natural language description of a swimming training session and generate ONLY valid DSL code. No explanations, no markdown, no comments — just the raw DSL code.

Here is the COMPLETE grammar of the DSL:

PROGRAM:
  One or more sessions.

SESSION:
  session <name> { <blocks or sections> }
  Sections (optional structure): warmup { ... } main { ... } cooldown { ... }

BLOCKS:
  Exercise — a single exercise
  Interval — N x Exercise rest N s

EXERCISES:
  swim <distance> m [<style>] [<intensity>] [pace <number>] [with <equipment>] [target <min>:<sec>]
  kick <distance> m [<intensity>] [with <equipment>]
  drill <drillType> <distance> m [<intensity>]

STYLES: freestyle | backstroke | breaststroke | butterfly
INTENSITIES: easy | moderate | hard
EQUIPMENT: fins | paddles | board | pullbuoy | snorkel
DRILL TYPES: catchup | onesided | fingertip | sixKick | sculling

INTERVALS:
  <N> x <exercise> rest <seconds> s

EXAMPLES OF VALID CODE:

session morning {
  swim 400 m freestyle easy pace 120
  4 x swim 100 m freestyle hard pace 90 rest 20 s
  swim 200 m backstroke moderate pace 130
  3 x kick 50 m hard rest 15 s
}

session structured {
  warmup {
    swim 400 m freestyle easy pace 120
  }
  main {
    8 x swim 100 m freestyle hard pace 75 rest 15 s
    4 x swim 200 m backstroke moderate pace 110 rest 30 s
  }
  cooldown {
    swim 200 m easy pace 140
  }
}

IMPORTANT RULES:
- Distance is always in meters with the "m" unit
- Pace is seconds per 100m
- Rest in intervals uses "s" for seconds
- Session names must be a single word (alphanumeric, no spaces)
- "swim" requires at least distance and "m"; style, intensity, pace, equipment are optional
- "kick" requires at least distance and "m"
- "drill" requires drillType, distance and "m"
- Intervals use "N x <exercise> rest N s" syntax
- Output ONLY the DSL code, nothing else`;

function stripFences(text) {
  return text.trim()
    .replace(/^```swim\s*/i, '').replace(/^```\w*\s*/i, '').replace(/```\s*$/, '')
    .trim();
}

/**
 * Translate natural language to DSL using the hybrid validation loop.
 * @param {string} userRequest
 * @returns {Promise<{success: boolean, dslCode?: string, error?: string, attempts: number}>}
 */
async function translate(userRequest) {
  const messages = [{ role: 'user', content: userRequest }];

  for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
    let dslCode;
    try {
      const response = await LLMService.chat(SYSTEM_PROMPT, messages, 0.2);
      dslCode = stripFences(response);
    } catch (e) {
      return { success: false, error: `LLM error: ${e.message}`, attempts: attempt };
    }

    let analysis;
    try {
      analysis = await RascalService.analyze(dslCode);
    } catch (e) {
      return { success: false, dslCode, error: `Rascal error: ${e.message}`, attempts: attempt };
    }

    if (analysis.success) {
      return { success: true, dslCode, analysis, attempts: attempt };
    }

    // Last attempt — return failure
    if (attempt === MAX_RETRIES) {
      return {
        success: false, dslCode,
        error: `Failed after ${MAX_RETRIES} attempts. Last error: ${analysis.error}`,
        attempts: attempt
      };
    }

    // Feed error back into conversation for self-correction
    messages.push({ role: 'assistant', content: dslCode });
    messages.push({
      role: 'user',
      content: `Este código generó el error: ${analysis.error}. Corrígelo respetando la gramática.`
    });
  }

  return { success: false, error: 'Unexpected error', attempts: MAX_RETRIES };
}

module.exports = { translate };
