const LLMService = require('../services/LLMService');

const BASE_SYSTEM_PROMPT = `Eres un entrenador experto de natación con años de experiencia entrenando nadadores de todos los niveles, desde principiantes hasta competidores.

Tu rol:
- Dar feedback práctico y específico sobre sesiones de entrenamiento
- Mencionar métricas concretas del análisis (distancia, tiempos, estilos, intensidades, descansos)
- Sugerir ajustes concretos cuando sea necesario
- Adaptar tus consejos al nivel del nadador según lo que se observa en la sesión
- Responder en el mismo idioma que el usuario (español o inglés)

Pautas de entrenamiento que conoces:
- Una sesión balanceada tiene calentamiento (15-20%), parte principal (60-70%) y vuelta a la calma (10-15%)
- Los descansos deben ser proporcionales a la intensidad: más descanso para series rápidas
- La variedad de estilos es buena para evitar lesiones y trabajar diferentes grupos musculares
- El pace (segundos por 100m) indica la velocidad: menor pace = más rápido
- Para principiantes: 1500-2500m total, pace 120-150s/100m
- Para intermedios: 2500-4000m total, pace 90-120s/100m
- Para avanzados: 4000-6000m total, pace 70-95s/100m

Si el usuario pide cambios al código, describe exactamente qué cambiarías en el DSL.
Sé conciso pero completo en tus respuestas.`;

// Per-session conversation histories keyed by session ID
const sessions = new Map();

function getHistory(sessionId) {
  if (!sessions.has(sessionId)) sessions.set(sessionId, []);
  return sessions.get(sessionId);
}

function resetSession(sessionId) {
  sessions.delete(sessionId);
}

/**
 * Chat with the coach agent.
 * @param {string} sessionId - client session identifier
 * @param {string} userMessage
 * @param {object|null} analysisResult - Rascal analysis JSON (optional)
 * @param {string|null} currentCode - current DSL code (optional)
 * @returns {Promise<string>} coach response text
 */
async function chat(sessionId, userMessage, analysisResult, currentCode) {
  let systemPrompt = BASE_SYSTEM_PROMPT;

  if (analysisResult && analysisResult.success) {
    systemPrompt += `\n\n--- ANÁLISIS DE LA SESIÓN ACTUAL ---
Distancia total: ${analysisResult.totalDistance}m (${analysisResult.distanceKm}km)
Sesiones: ${analysisResult.sessionCount}
Estilos: ${JSON.stringify(analysisResult.styles)}
Intensidades: ${JSON.stringify(analysisResult.intensities)}
Tiempo descanso: ${analysisResult.time?.restFormatted || '—'}
Tiempo total: ${analysisResult.time?.totalFormatted || '—'}
Descansos: ${analysisResult.rest?.periods || 0} períodos, promedio ${analysisResult.rest?.average || 0}s`;

    if (analysisResult.equipment && Object.keys(analysisResult.equipment).length > 0) {
      systemPrompt += `\nEquipamiento: ${JSON.stringify(analysisResult.equipment)}`;
    }
    if (analysisResult.drills && Object.keys(analysisResult.drills).length > 0) {
      systemPrompt += `\nDrills: ${JSON.stringify(analysisResult.drills)}`;
    }
  }

  if (currentCode) {
    systemPrompt += `\n\n--- CÓDIGO DSL ACTUAL ---\n${currentCode}\n`;
  }

  const history = getHistory(sessionId);
  history.push({ role: 'user', content: userMessage });

  const response = await LLMService.chat(systemPrompt, history);
  history.push({ role: 'assistant', content: response });

  return response;
}

module.exports = { chat, resetSession };
