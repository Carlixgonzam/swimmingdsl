const { exec } = require('child_process');
const path = require('path');

const PROJECT_PATH = path.resolve(__dirname, '../..');
const RASCAL_JAR = path.join(PROJECT_PATH, 'rascal-shell-stable.jar');
const SRC_PATH = path.join(PROJECT_PATH, 'src');

function executeRascal(command, args) {
  return new Promise((resolve, reject) => {
    const escapedArgs = [command, ...args]
      .map(arg => `'${arg.replace(/'/g, "'\\''")}'`)
      .join(' ');

    const fullCmd = `cd "${SRC_PATH}" && java -Dfile.encoding=UTF-8 -Drascal.projectPath="${SRC_PATH}" -jar "${RASCAL_JAR}" Runner.rsc ${escapedArgs} < /dev/null`;

    exec(fullCmd, { cwd: SRC_PATH, maxBuffer: 10 * 1024 * 1024, timeout: 30000 }, (error, stdout, stderr) => {
      if (error && (!stdout || stdout.trim().length === 0)) {
        reject(new Error(`Rascal execution failed: ${error.message}`));
        return;
      }

      const cleanOutput = stdout.replace(/\x1b\[[0-9;]*[a-zA-Z]/g, '');
      let lastValidJson = null;
      let startIdx = 0;

      while (startIdx < cleanOutput.length) {
        const braceIdx = cleanOutput.indexOf('{', startIdx);
        if (braceIdx === -1) break;

        let depth = 0, inString = false, escaped = false, endIdx = -1;
        for (let i = braceIdx; i < cleanOutput.length; i++) {
          const ch = cleanOutput[i];
          if (escaped) { escaped = false; continue; }
          if (ch === '\\' && inString) { escaped = true; continue; }
          if (ch === '"') { inString = !inString; continue; }
          if (!inString) {
            if (ch === '{') depth++;
            else if (ch === '}') { depth--; if (depth === 0) { endIdx = i; break; } }
          }
        }

        if (endIdx !== -1) {
          const candidate = cleanOutput.substring(braceIdx, endIdx + 1);
          try {
            const parsed = JSON.parse(candidate);
            if (parsed.success !== undefined) lastValidJson = candidate;
            else if (!lastValidJson) lastValidJson = candidate;
          } catch (_) { /* skip */ }
        }
        startIdx = braceIdx + 1;
      }

      if (!lastValidJson) {
        reject(new Error('No JSON output from Rascal'));
        return;
      }

      try { resolve(JSON.parse(lastValidJson)); }
      catch (e) { reject(new Error(`Failed to parse Rascal output: ${e.message}`)); }
    });
  });
}

function formatTime(seconds) {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

async function analyze(code) {
  const result = await executeRascal('analyze', [code]);
  if (result.success && result.time) {
    result.time.swimFormatted = formatTime(result.time.swimSeconds);
    result.time.restFormatted = formatTime(result.time.restSeconds);
    result.time.totalFormatted = formatTime(result.time.totalSeconds);
  }
  if (result.success && result.rest) {
    result.rest.totalFormatted = formatTime(result.rest.totalSeconds);
  }
  return result;
}

async function generate(goal, distance, styles, duration) {
  const stylesStr = Array.isArray(styles) ? styles.join(',') : styles;
  return executeRascal('generate', [goal, distance.toString(), stylesStr, duration.toString()]);
}

module.exports = { analyze, generate, formatTime };
