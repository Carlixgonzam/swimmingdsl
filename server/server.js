const express = require('express');
const { exec } = require('child_process');
const path = require('path');
const cors = require('cors');
const fs = require('fs');
const os = require('os');

const app = express();
const PORT = process.env.PORT || 3000;
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, '../web')));
const RASCAL_JAR = path.join(__dirname, '../rascal-shell-stable.jar');
const PROJECT_PATH = path.dirname(__dirname);

function executeRascal(command, args) {
  return new Promise((resolve, reject) => {
    // Build command line arguments with proper escaping
    const allArgs = [command, ...args];
    const escapedArgs = allArgs.map(arg => {
      // Escape for shell: single quotes, with escaped single quotes inside
      return `'${arg.replace(/'/g, "'\\''")}' `;
    }).join(' ');
    
    console.log('Executing Rascal command:', command, 'with', args.length, 'arg(s)');
    
    // Execute Rascal: change to src directory and run Runner module
    // The -Dproject.root sets where Rascal looks for modules
    const srcPath = path.join(PROJECT_PATH, 'src');
    const fullCmd = `cd "${srcPath}" && java -Dfile.encoding=UTF-8 -Drascal.projectPath="${srcPath}" -jar "${RASCAL_JAR}" Runner.rsc ${escapedArgs} < /dev/null`;
    
    exec(fullCmd, {
      cwd: srcPath, // Execute from src directory so Rascal can find modules
      maxBuffer: 10 * 1024 * 1024, // 10MB buffer
      timeout: 30000 // 30 second timeout
    }, (error, stdout, stderr) => {
      console.log('Rascal callback received!');
      console.log('Error:', error);
      console.log('Stdout length:', stdout ? stdout.length : 0);
      console.log('Stderr length:', stderr ? stderr.length : 0);
      if (error) {
        console.error('Rascal execution error:', error);
        console.error('stderr:', stderr);
        reject(new Error(`Rascal execution failed: ${error.message}`));
        return;
      }
      
      // extraigo JSON - handle ANSI codes and embedded JSON
      console.log('===== FULL RASCAL OUTPUT =====');
      console.log(stdout);
      console.log('===== END OUTPUT =====');
      
      // Strip ANSI escape codes first
      const cleanOutput = stdout.replace(/\x1b\[[0-9;]*[a-zA-Z]/g, '');
      
      // Find JSON object by properly matching braces (respecting strings)
      let jsonStr = null;
      let lastValidJson = null;
      let startIdx = 0;
      
      while (startIdx < cleanOutput.length) {
        const braceIdx = cleanOutput.indexOf('{', startIdx);
        if (braceIdx === -1) break;
        
        // Find matching closing brace, respecting string contents
        let depth = 0;
        let inString = false;
        let escaped = false;
        let endIdx = -1;
        
        for (let i = braceIdx; i < cleanOutput.length; i++) {
          const char = cleanOutput[i];
          
          if (escaped) {
            escaped = false;
            continue;
          }
          
          if (char === '\\' && inString) {
            escaped = true;
            continue;
          }
          
          if (char === '"') {
            inString = !inString;
            continue;
          }
          
          if (!inString) {
            if (char === '{') depth++;
            else if (char === '}') {
              depth--;
              if (depth === 0) {
                endIdx = i;
                break;
              }
            }
          }
        }
        
        if (endIdx !== -1) {
          const candidate = cleanOutput.substring(braceIdx, endIdx + 1);
          try {
            const parsed = JSON.parse(candidate);
            // Prefer JSON with 'success' property (our API response)
            if (parsed.success !== undefined) {
              lastValidJson = candidate;
            } else if (!lastValidJson) {
              lastValidJson = candidate;
            }
          } catch (e) {
            // Not valid JSON, continue searching
          }
        }
        
        startIdx = braceIdx + 1;
      }
      
      jsonStr = lastValidJson;
      
      if (!jsonStr) {
        console.error('===== NO JSON FOUND =====');
        console.error('Clean output:', cleanOutput);
        console.error('Stderr:', stderr);
        reject(new Error('No JSON output from Rascal'));
        return;
      }
      
      try {
        const result = JSON.parse(jsonStr);
        resolve(result);
      } catch (parseError) {
        console.error('JSON parse error:', parseError);
        console.error('Attempted to parse:', jsonStr);
        reject(new Error(`Failed to parse Rascal output: ${parseError.message}`));
      }
    });
  });
}
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Swimming DSL API is running' });
});

app.post('/api/analyze', async (req, res) => {
  try {
    const { code } = req.body;
    
    if (!code) {
      return res.status(400).json({ success: false, error: 'No code provided' });
    }
    
    console.log('Analyzing code:', code.substring(0, 100) + '...');
    
    const result = await executeRascal('analyze', [code]);
    
    if (result.success && result.time) {
      result.time.swimFormatted = formatTime(result.time.swimSeconds);
      result.time.restFormatted = formatTime(result.time.restSeconds);
      result.time.totalFormatted = formatTime(result.time.totalSeconds);
    }
    
    if (result.success && result.rest) {
      result.rest.totalFormatted = formatTime(result.rest.totalSeconds);
    }
    
    res.json(result);
  } catch (error) {
    console.error('Analysis error:', error);
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

app.post('/api/generate', async (req, res) => {
  try {
    const { goal, distance, styles, duration } = req.body;
    
    if (!goal || !distance || !styles || !duration) {
      return res.status(400).json({ 
        success: false, 
        error: 'Missing required parameters: goal, distance, styles, duration' 
      });
    }
    
    console.log('Generating session:', { goal, distance, styles, duration });
    
    const stylesStr = Array.isArray(styles) ? styles.join(',') : styles;
    const result = await executeRascal('generate', [
      goal,
      distance.toString(),
      stylesStr,
      duration.toString()
    ]);
    
    res.json(result);
  } catch (error) {
    console.error('Generation error:', error);
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

function formatTime(seconds) {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, '../web/index-server.html'));
});

app.listen(PORT, () => {
  console.log(`🏊‍♀️ Swimming DSL Server running on http://localhost:${PORT}`);
  console.log(`📂 Project path: ${PROJECT_PATH}`);
  console.log(`☕ Rascal JAR: ${RASCAL_JAR}`);
  console.log('\nAPI Endpoints:');
  console.log(`  GET  /api/health - Health check`);
  console.log(`  POST /api/analyze - Analyze DSL code`);
  console.log(`  POST /api/generate - Generate session`);
});

module.exports = app;
