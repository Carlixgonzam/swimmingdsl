const LLMService = require('../services/LLMService');
const RascalService = require('../services/RascalService');

const SYSTEM_PROMPT = `You are a swimming training plan optimizer. Given a DSL session and optimization parameters, improve the session while keeping it valid.

The DSL grammar:
- session <name> { <blocks or sections> }
- Sections: warmup { ... } main { ... } cooldown { ... }
- swim <distance> m [<style>] [<intensity>] [pace <number>] [with <equipment>]
- kick <distance> m [<intensity>] [with <equipment>]
- drill <drillType> <distance> m [<intensity>]
- Intervals: <N> x <exercise> rest <N> s
- Styles: freestyle | backstroke | breaststroke | butterfly
- Intensities: easy | moderate | hard
- Equipment: fins | paddles | board | pullbuoy | snorkel
- Drills: catchup | onesided | fingertip | sixKick | sculling

Optimization guidelines:
- endurance: longer distances, moderate pace, warmup/main/cooldown structure
- speed: shorter intervals, hard intensity, more rest between sets
- technique: include drills and equipment, moderate distances
- recovery: easy intensity, varied styles, shorter distances

Output ONLY the improved DSL code, nothing else.`;

function stripFences(text) {
  return text.trim()
    .replace(/^```swim\s*/i, '').replace(/^```\w*\s*/i, '').replace(/```\s*$/, '')
    .trim();
}

/**
 * Optimize a DSL session.
 * @param {string} currentCode - existing DSL code
 * @param {string} goal - endurance | speed | technique | recovery
 * @param {object} params - { distance, styles, duration } (optional hints)
 * @returns {Promise<{success: boolean, code?: string, method?: string, error?: string}>}
 */
async function optimize(currentCode, goal = 'endurance', params = {}) {
  const prompt = `Optimize this swimming session for the goal "${goal}".
${params.distance ? `Target distance: ~${params.distance}m` : ''}
${params.duration ? `Target duration: ${params.duration} minutes` : ''}

Current DSL code:
${currentCode}

Generate an improved version. Output ONLY the DSL code.`;

  // Step 1: try LLM optimization
  try {
    const response = await LLMService.chat(SYSTEM_PROMPT, [{ role: 'user', content: prompt }], 0.3);
    const optimized = stripFences(response);
    const analysis = await RascalService.analyze(optimized);

    if (analysis.success) {
      return { success: true, code: optimized, analysis, method: 'llm' };
    }
  } catch (e) {
    console.error('OptimizerAgent LLM attempt failed:', e.message);
  }

  // Step 2: fallback to Rascal native generator
  try {
    const distance = params.distance || 2000;
    const styles = params.styles || ['freestyle'];
    const duration = params.duration || 60;

    const generated = await RascalService.generate(goal, distance, styles, duration);
    if (generated.success && generated.code) {
      const analysis = await RascalService.analyze(generated.code);
      return {
        success: true,
        code: generated.code,
        analysis: analysis.success ? analysis : undefined,
        method: 'rascal_fallback'
      };
    }
  } catch (e) {
    console.error('OptimizerAgent Rascal fallback failed:', e.message);
  }

  return { success: false, error: 'Optimization failed after all attempts' };
}

module.exports = { optimize };
