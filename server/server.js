const express = require('express');
const path = require('path');
const cors = require('cors');

// Services
const RascalService = require('./services/RascalService');

// Agents
const DSLTranslatorAgent = require('./agents/DSLTranslatorAgent');
const CoachAgent = require('./agents/CoachAgent');
const OptimizerAgent = require('./agents/OptimizerAgent');

const app = express();
const PORT = process.env.PORT || 3000;
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, '../web')));

// ═══════════════════════════════════════════════════════════════
//  Existing endpoints (Editor + Generator)
// ═══════════════════════════════════════════════════════════════

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'Swimming DSL API is running' });
});

app.post('/api/analyze', async (req, res) => {
  try {
    const { code } = req.body;
    if (!code) return res.status(400).json({ success: false, error: 'No code provided' });

    console.log('[analyze] code length:', code.length);
    const result = await RascalService.analyze(code);
    res.json(result);
  } catch (error) {
    console.error('Analysis error:', error.message);
    res.status(500).json({ success: false, error: error.message });
  }
});

app.post('/api/generate', async (req, res) => {
  try {
    const { goal, distance, styles, duration } = req.body;
    if (!goal || !distance || !styles || !duration) {
      return res.status(400).json({ success: false, error: 'Missing required parameters' });
    }

    console.log('[generate]', { goal, distance, styles, duration });
    const result = await RascalService.generate(goal, distance, styles, duration);
    res.json(result);
  } catch (error) {
    console.error('Generation error:', error.message);
    res.status(500).json({ success: false, error: error.message });
  }
});

// ═══════════════════════════════════════════════════════════════
//  AI Agent endpoints
// ═══════════════════════════════════════════════════════════════

app.post('/api/translate', async (req, res) => {
  try {
    const { prompt } = req.body;
    if (!prompt) return res.status(400).json({ success: false, error: 'No prompt provided' });

    console.log('[translate] prompt:', prompt.substring(0, 80) + '...');
    const result = await DSLTranslatorAgent.translate(prompt);
    res.json(result);
  } catch (error) {
    console.error('Translation error:', error.message);
    res.status(500).json({ success: false, error: error.message });
  }
});

app.post('/api/coach', async (req, res) => {
  try {
    const { sessionId, message, analysisResult, currentCode } = req.body;
    if (!message) return res.status(400).json({ error: 'No message provided' });

    const sid = sessionId || 'default';
    console.log('[coach] session:', sid);
    const response = await CoachAgent.chat(sid, message, analysisResult, currentCode);
    res.json({ success: true, response });
  } catch (error) {
    console.error('Coach error:', error.message);
    res.status(500).json({ success: false, error: error.message });
  }
});

app.post('/api/coach/reset', (req, res) => {
  CoachAgent.resetSession(req.body.sessionId || 'default');
  res.json({ success: true });
});

app.post('/api/optimize', async (req, res) => {
  try {
    const { code, goal, distance, styles, duration } = req.body;
    if (!code) return res.status(400).json({ success: false, error: 'No code provided' });

    console.log('[optimize] goal:', goal || 'endurance');
    const result = await OptimizerAgent.optimize(code, goal, { distance, styles, duration });
    res.json(result);
  } catch (error) {
    console.error('Optimization error:', error.message);
    res.status(500).json({ success: false, error: error.message });
  }
});

// ═══════════════════════════════════════════════════════════════
//  Static files
// ═══════════════════════════════════════════════════════════════

app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, '../web/index-server.html'));
});

app.listen(PORT, () => {
  console.log(`\nSwimming DSL Server running on http://localhost:${PORT}`);
  console.log(`\nEndpoints:`);
  console.log(`  POST /api/analyze    - Analyze DSL (Rascal)`);
  console.log(`  POST /api/generate   - Generate session (Rascal)`);
  console.log(`  POST /api/translate  - NL → DSL (AI + Rascal loop)`);
  console.log(`  POST /api/coach      - Coaching feedback (AI)`);
  console.log(`  POST /api/optimize   - Optimize session (AI + Rascal)`);
  console.log(`\nGEMINI_API_KEY: ${process.env.GEMINI_API_KEY ? 'set' : 'NOT SET'}`);
});

module.exports = app;
