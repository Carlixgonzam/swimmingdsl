const https = require('https');

const MODEL = 'gemini-2.5-flash';

function getApiKey() {
  const key = process.env.GEMINI_API_KEY;
  if (!key) throw new Error('GEMINI_API_KEY environment variable is not set');
  return key;
}

/**
 * Send a chat request to Gemini.
 * @param {string} systemPrompt
 * @param {Array<{role: string, content: string}>} messages
 * @param {number} temperature
 * @returns {Promise<string>} The model's text response
 */
async function chat(systemPrompt, messages, temperature = 0.7) {
  const apiKey = getApiKey();

  const body = {
    systemInstruction: { parts: [{ text: systemPrompt }] },
    contents: messages.map(msg => ({
      role: msg.role === 'assistant' ? 'model' : 'user',
      parts: [{ text: msg.content }]
    })),
    generationConfig: { maxOutputTokens: 8192, temperature }
  };

  const url = `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent?key=${apiKey}`;
  const response = await postJSON(url, body);

  if (response.error) {
    throw new Error(`Gemini API error: ${response.error.message || JSON.stringify(response.error)}`);
  }

  if (!response.candidates || response.candidates.length === 0) {
    throw new Error('No candidates in Gemini response');
  }

  const parts = response.candidates[0].content?.parts;
  if (!parts || parts.length === 0) {
    throw new Error('No text in Gemini response parts');
  }

  return parts[0].text;
}

function postJSON(url, body) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const data = JSON.stringify(body);

    const req = https.request({
      hostname: parsed.hostname,
      path: parsed.pathname + parsed.search,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data)
      },
      timeout: 60000
    }, res => {
      let chunks = '';
      res.on('data', chunk => chunks += chunk);
      res.on('end', () => {
        try { resolve(JSON.parse(chunks)); }
        catch (e) { reject(new Error(`Failed to parse Gemini response: ${chunks.substring(0, 300)}`)); }
      });
    });

    req.on('error', reject);
    req.on('timeout', () => { req.destroy(); reject(new Error('Gemini request timeout (60s)')); });
    req.write(data);
    req.end();
  });
}

module.exports = { chat };
