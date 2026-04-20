# Swimming DSL — Desktop App

Aplicación de escritorio para diseñar, analizar y optimizar sesiones de entrenamiento de natación usando un **lenguaje de dominio específico (DSL)** propio, parseado por **Rascal**, y potenciado por **3 agentes de IA** sobre Gemini 2.5 Flash.

---

## Tabla de contenidos

- [Requisitos](#requisitos)
- [Configuración](#configuración)
- [Cómo correr](#cómo-correr)
- [El DSL de natación](#el-dsl-de-natación)
- [Arquitectura del sistema](#arquitectura-del-sistema)
- [Los 3 agentes de IA](#los-3-agentes-de-ia)
- [Servicios base](#servicios-base)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Pantallas de la app](#pantallas-de-la-app)

---

## Requisitos

| Herramienta | Versión mínima |
|---|---|
| Java (JDK) | 17+ |
| Gradle | incluido via wrapper |
| Gemini API Key | [Obtener aquí](https://aistudio.google.com/app/apikey) |

Los archivos `rascal-shell-stable.jar` y los fuentes `.rsc` deben estar en `../` relativo a `kotlin-app/` (es decir, en la raíz de `swimmingdsl/`).

---

## Configuración

Antes de correr la app, exporta tu API key de Gemini:

```bash
export GEMINI_API_KEY="tu_api_key_aqui"
```

> Sin esta variable, la app arranca pero los 3 agentes de IA lanzarán error al usarse.

---

## Cómo correr

```bash
cd kotlin-app
./gradlew run
```

O con limpieza previa:

```bash
./gradlew clean run
```

---

## El DSL de natación

El DSL permite describir sesiones de entrenamiento de manera estructurada. Es parseado por Rascal (gramática libre de contexto) y analizado para extraer métricas.

### Estructura básica

```
session <nombre> {
  <bloques>
}
```

### Con secciones estructuradas (warmup / main / cooldown)

```
session <nombre> {
  warmup {
    <bloques>
  }
  main {
    <bloques>
  }
  cooldown {
    <bloques>
  }
}
```

### Tipos de ejercicio

```
# Natación
sim <distancia> m [<estilo>] [<intensidad>] [pace <número>] [with <equipamiento>] [target <min>:<seg>]

# Pataleo
kick <distancia> m [<intensidad>] [with <equipamiento>]

# Drill técnico
drill <tipoDrill> <distancia> m [<intensidad>]
```

### Intervalos

```
<repeticiones> x <ejercicio> rest <segundos> s
```

### Valores permitidos

| Categoría | Valores |
|---|---|
| **Estilos** | `freestyle` `backstroke` `breaststroke` `butterfly` |
| **Intensidades** | `easy` `moderate` `hard` |
| **Equipamiento** | `fins` `paddles` `board` `pullbuoy` `snorkel` |
| **Drill types** | `catchup` `onesided` `fingertip` `sixKick` `sculling` |
| **Pace** | entero (segundos por 100m) |
| **Rest** | entero (segundos) |

### Ejemplo completo

```
session semana1_martes {
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

---

## Arquitectura del sistema

```
SwimmingDslApp (Compose Desktop)
│
├── RascalService          ← parsea y analiza código DSL via Rascal jar
│     ├── Modo REPL        ← proceso Java persistente (más rápido)
│     └── Modo fallback    ← un proceso Java por llamada (más robusto)
│
├── LLMService             ← cliente HTTP hacia Gemini 2.5 Flash API
│
├── DSLTranslatorAgent     ← lenguaje natural → DSL válido (con reintentos)
├── CoachAgent             ← chat conversacional con contexto de análisis
└── OptimizerAgent         ← planes de entrenamiento multi-semana progresivos
```

### Flujo de análisis (doAnalyze)

Cuando el usuario presiona "Analizar":

```
código DSL (string)
      ↓
RascalService.analyze()
      ↓
Runner.rsc analyze <código>    ← proceso Rascal
      ↓
WebAPI::analyzeToJSON()         ← gramática + regex en Rascal
      ↓
JSON con métricas:
  { success, totalDistance, distanceKm, sessionCount,
    sessionNames, styles, intensities, equipment, drills,
    rest: { periods, totalSeconds, average },
    time: { swimSeconds, restSeconds, totalSeconds } }
      ↓
AnalysisResult (data class Kotlin)
      ↓
UI + historial persistido en ~/.swimmingdsl/history.json
```

---

## Los 3 agentes de IA

### 1. DSLTranslatorAgent — Traductor de lenguaje natural

**Propósito:** Convertir una descripción en lenguaje natural a código DSL sintácticamente válido.

**Lógica (loop de validación con reintentos):**

```
translate(userRequest):
  for attempt in 1..3:
    1. LLM genera DSL (temperatura=0.2, determinista)
    2. rascalService.analyze(dslCode)  ← valida con el parser real
    3. ¿success? → retorna dslCode ✓
       ¿error?   → añade al historial del chat:
                   "Este código generó el error: <X>. Corrígelo."
                   → reintenta con el contexto del error
  si 3 intentos fallan → retorna error con último código generado
```

**Por qué funciona:** El LLM recibe su propio error de Rascal y puede auto-corregirse en la siguiente iteración. La temperatura baja (0.2) reduce la aleatoriedad para generar código estructurado preciso. El system prompt contiene la gramática completa con ejemplos válidos, y pide solo código crudo sin markdown.

---

### 2. CoachAgent — Entrenador personal conversacional

**Propósito:** Chat con un entrenador experto que tiene acceso al análisis de la sesión actual.

**Lógica (chat con contexto dinámico):**

```
chat(userMessage, analysisResult, currentCode):
  1. Construye systemPrompt dinámico:
       BASE_PROMPT (rol, pautas por nivel: principiante/intermedio/avanzado)
       + análisis actual de Rascal inyectado como texto:
         distancia, estilos, intensidades, tiempos, descansos, equipamiento
       + código DSL actual del editor
  2. Añade userMessage a conversationHistory
  3. llmService.chat(systemPrompt, conversationHistory)
  4. Añade respuesta al historial → mantiene contexto entre mensajes
  5. Si error → removeLastOrNull() del historial → no corrompe la conversación
```

**Por qué funciona:** El análisis de Rascal se inyecta como contexto en cada llamada, dándole al LLM datos concretos (no inferidos) para dar feedback específico. El historial en memoria permite conversaciones multi-turno coherentes.

**Estado en memoria:** el historial vive mientras la app está abierta. Se limpia con `resetConversation()`.

---

### 3. OptimizerAgent — Planificador de entrenamiento multi-semana

**Propósito:** Generar planes progresivos de N semanas con M sesiones por semana, validadas y analizadas por Rascal.

**Lógica (generación progresiva con doble fallback):**

```
optimize(config, onProgress):
  rascalCallCount = 0

  for week in 1..weeks:
    for session in 1..sessionsPerWeek:

      # Progresión automática: +7% de distancia por semana
      progressionFactor = 1.0 + (week - 1) × 0.07
      targetDistance = baseDistance × progressionFactor

      # Prompt contextual con historial
      prompt incluye:
        - goal, week/total, session/total
        - targetDistance, maxMinutes, styles
        - distancia de la sesión anterior (para progresión coherente)

      # Intento principal: LLM genera DSL
      dslCode = llmService.chat(SYSTEM_PROMPT, [prompt])
      analysis = rascalService.analyze(dslCode)   ← rascalCallCount++

      if analysis.success:
        sessions.add(SessionPlan(week, sessionNum, dslCode, analysis))
      else:
        # Fallback nivel 1: generador nativo de Rascal
        fallback = rascalService.generate(goal, targetDistance, styles, maxMinutes)
        adjustedCode = adjustGeneratedDistance(fallback.code, targetDistance)
        fallbackAnalysis = rascalService.analyze(adjustedCode)   ← rascalCallCount++
        if fallbackAnalysis.success: sessions.add(...)

      # Protección de recursos
      if rascalCallCount >= 10:
        return OptimizationResult(success=true, sessions=parcial)

  return OptimizationResult(success=true, sessions=completo)
```

**Detalles clave:**

- **+7% semanal:** `progressionFactor = 1.0 + (week-1) * 0.07`. Semana 1 = 100%, semana 4 = 121%, semana 8 = 149%.
- **Contexto acumulativo:** el LLM recibe la distancia de la sesión anterior para mantener progresión coherente.
- **`adjustGeneratedDistance`:** cuando Rascal genera código con distancias afectadas por división entera, este util recalcula la distancia real del DSL y ajusta el último `swim N m` para compensar la diferencia.
- **Límite de 10 llamadas Rascal:** evita tiempos de espera excesivos en planes largos. Si se alcanza, se retorna el plan parcial generado hasta ese punto.

---

## Servicios base

### `LLMService`

Cliente HTTP (Ktor + CIO engine) que comunica con Gemini 2.5 Flash.

```
chat(systemPrompt, messages, temperature=0.7):
  POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent
  Headers: Content-Type: application/json
  Body:
    systemInstruction: { parts: [{ text: systemPrompt }] }
    contents: [ { role: "user"|"model", parts: [{ text }] }, ... ]
    generationConfig: { maxOutputTokens: 8192, temperature }
  → retorna text del primer candidate
```

- API key desde `GEMINI_API_KEY` (variable de entorno)
- Timeout: 60 segundos
- Si `finishReason == "MAX_TOKENS"`: log de advertencia (respuesta truncada)
- Si la respuesta contiene clave `"error"`: lanza excepción con el mensaje de la API

### `RascalService`

Gestiona la ejecución del parser Rascal. Tiene dos modos con fallback automático.

**Modo REPL (persistente):**

```
initRepl():
  1. Arranca: java -Drascal.projectPath=<src/> -jar rascal-shell-stable.jar
  2. Envía: import WebAPI;   ← compila módulos una vez
  3. Prueba: println("__RASCAL_START__"); println("ok"); println("__RASCAL_END__");
  4. ¿Responde en 15s? → REPL listo
     ¿No responde?    → fallback mode

executeViaRepl(expression):
  escribe: println("__RASCAL_START__"); println(<expression>); println("__RASCAL_END__");
  lee líneas entre START y END markers
  timeout: 30s
```

**Modo fallback (process-per-call):**

```
executeRascalProcess(command, args):
  java -jar rascal-shell-stable.jar Runner.rsc <command> <args...>
  timeout: 30s
  ~5-6 segundos por llamada
```

**Extracción de JSON:** `extractJson()` escanea el stdout en busca del objeto JSON válido que contenga la clave `"success"`. Ignora cualquier output extra de progreso de Rascal.

**Escapado:** `escapeForRascal()` escapa `\`, `"`, `\n`, `\r`, `\t`, `<` y `>` antes de embeber el código DSL en una expresión Rascal (los `<>` son interpolación en strings de Rascal).

---

## Estructura del proyecto

```
swimmingdsl/
│
├── rascal-shell-stable.jar         ← Rascal runtime (v0.40.17)
│
├── src/                            ← Módulos Rascal del DSL
│   ├── Lexer.rsc                   ← tokens: INT, ID, Keywords, layout WS
│   ├── SwimSyntax.rsc              ← gramática completa del DSL
│   ├── AST.rsc                     ← tipos de datos: Program, Session, Block, Exercise...
│   ├── Semantics.rsc               ← lógica: cálculo de distancias, tiempos, generadores
│   ├── WebAPI.rsc                  ← analyzeToJSON + generateToJSON (interfaz principal)
│   └── Runner.rsc                  ← entry point CLI (invocado por process-per-call)
│
└── kotlin-app/
    └── src/main/kotlin/swimming/
        ├── Main.kt                 ← entry point, SwimmingDslApp, AppTab enum
        ├── agent/
        │   ├── DSLTranslatorAgent.kt   ← NL→DSL con reintentos automáticos
        │   ├── CoachAgent.kt           ← chat conversacional con contexto
        │   └── OptimizerAgent.kt       ← planes multi-semana progresivos
        ├── service/
        │   ├── LLMService.kt           ← cliente Gemini API
        │   └── RascalService.kt        ← interfaz Rascal (REPL + fallback)
        ├── model/
        │   ├── AnalysisResult.kt       ← AnalysisResult, GenerateResult, TimeInfo, RestInfo
        │   └── UserProfile.kt          ← nivel, estilos preferidos, minutos disponibles
        ├── util/
        │   └── DslDistanceAdjuster.kt  ← ajusta distancias en DSL generado por Rascal
        └── ui/
            ├── DashboardPanel.kt       ← historial y estadísticas globales
            ├── EditorPanel.kt          ← editor de código DSL
            ├── TranslatorPanel.kt      ← UI del DSLTranslatorAgent
            ├── CoachPanel.kt           ← UI del CoachAgent (chat)
            ├── OptimizerPanel.kt       ← UI del OptimizerAgent
            ├── AnalysisPanel.kt        ← panel de métricas de Rascal (sidebar derecho)
            ├── OnboardingScreen.kt     ← pantalla inicial de configuración de perfil
            └── SidebarNav.kt           ← navegación lateral
```

---

## Pantallas de la app

| Pantalla | Agente/Servicio | Descripción |
|---|---|---|
| **Dashboard** | — | Historial de sesiones, estadísticas globales, perfil |
| **Editor** | RascalService | Editor DSL con análisis manual |
| **Traductor IA** | DSLTranslatorAgent | Descripción en lenguaje natural → DSL válido |
| **Coach IA** | CoachAgent | Chat con entrenador que conoce la sesión actual |
| **Optimizador IA** | OptimizerAgent | Genera plan completo multi-semana |

En todas las pestañas excepto Dashboard, el panel de análisis de Rascal se muestra a la derecha mostrando distancia, estilos, intensidades, tiempos y descansos de la sesión actual.

---

## Datos persistidos

```
~/.swimmingdsl/
  ├── profile.json    ← perfil del usuario (nivel, estilos preferidos, minutos disponibles)
  └── history.json    ← lista de AnalysisResult de sesiones analizadas exitosamente
```

El perfil se crea en el onboarding inicial. Si el archivo no existe al iniciar, se muestra la pantalla de bienvenida para configurarlo.

Un DSL (Domain-Specific Language) para programar y analizar sesiones de entrenamiento de natación, construido con Rascal MPL.

## Características

### 1. **Sintaxis Básica**
```swim
session morning {
  swim 400 m freestyle easy pace 120
  8 x swim 100 m freestyle hard pace 75 rest 15 s
  kick 100 m easy
}
```

### 2. **Estructura con Secciones**
Organiza tus sesiones en warmup, main y cooldown:

```swim
session advanced {
  warmup {
    swim 400 m freestyle easy pace 120
    swim 200 m backstroke easy pace 130
  }
  
  main {
    8 x swim 100 m freestyle hard pace 75 rest 15 s
    4 x swim 200 m backstroke moderate pace 110 rest 30 s
  }
  
  cooldown {
    swim 200 m easy pace 140
    kick 100 m easy
  }
}
```

### 3. **Estilos de Nado**
- `freestyle` - Crol/estilo libre
- `backstroke` - Espalda
- `breaststroke` - Pecho/braza
- `butterfly` - Mariposa

### 4. **Niveles de Intensidad**
- `easy` - Fácil / recuperación
- `moderate` - Moderado
- `hard` - Difícil / intenso

### 5. **Equipamiento**
```swim
session withEquipment {
  swim 300 m easy pace 120 with fins
  swim 200 m freestyle moderate with paddles
  kick 100 m hard with board
  swim 150 m easy with pullbuoy
  swim 200 m easy with snorkel
}
```

Equipamiento disponible:
- `fins` - Aletas
- `paddles` - Palas/manoplas
- `board` - Tabla
- `pullbuoy` - Pull buoy
- `snorkel` - Snorkel frontal

### 6. **Ejercicios de Técnica (Drills)**
```swim
session techniqueWork {
  drill catchup 200 m easy
  drill fingertip 200 m easy
  4 x drill sculling 50 m easy rest 20 s
  drill onesided 200 m moderate
}
```

Drills disponibles:
- `catchup` - Catch-up
- `onesided` - Un solo brazo
- `fingertip` - Punta de dedos
- `6kick` - 6 patadas
- `sculling` - Sculling/remadas

### 7. **Metas de Tiempo (Targets)**
```swim
session withTargets {
  swim 100 m freestyle hard pace 70 target 1:10
  4 x swim 50 m butterfly hard pace 50 target 0:40 rest 30 s
  swim 200 m backstroke moderate pace 100 target 2:00
}
```

### 8. **Generador Automático de Sesiones** 
```swim
generate session {
  goal: endurance
  distance: 3000
  styles: [freestyle, backstroke]
  duration: 60 minutes
}
```

Tipos de objetivos (goals):
- `endurance` - Resistencia aeróbica (series largas, pace moderado)
- `speed` - Velocidad (series cortas, pace rápido)
- `technique` - Técnica (énfasis en drills)
- `recovery` - Recuperación (nado fácil continuo)

## Análisis Automático

El DSL proporciona análisis detallado de tus sesiones:

```
═══════════════════════════════════════════════════════
  SWIMMING DSL - Session Analysis
  File: simple_advanced.swim
═══════════════════════════════════════════════════════

PARSING...
✓ Parse successful!

BASIC ANALYSIS:
───────────────────────────────────────────────────────
Total sessions: 1
Session names: morning

DISTANCE CALCULATION:
───────────────────────────────────────────────────────
Total distance: 1700 meters (1.7 km)

STROKE ANALYSIS:
───────────────────────────────────────────────────────
  • freestyle: 3 set(s)
  • backstroke: 1 set(s)

INTENSITY ANALYSIS:
───────────────────────────────────────────────────────
  • easy: 3 set(s)
  • moderate: 1 set(s)
  • hard: 3 set(s)

REST ANALYSIS:
───────────────────────────────────────────────────────
  Total rest periods: 1
  Total rest time: 105 seconds (1:45)
  Average rest: 15 seconds

TIME ESTIMATION:
───────────────────────────────────────────────────────
  Estimated swim time: 25:30
  Rest time: 1:45
  Total session time: 27:15

═══════════════════════════════════════════════════════
✓ Analysis complete!
═══════════════════════════════════════════════════════
```

## Cómo Usar

### Requisitos
- Java 11+
- Rascal MPL (incluido en `rascal.jar`)

### Instalación
1. Clona este repositorio
2. Asegúrate de tener `rascal.jar` en el directorio principal

### Ejecutar
```bash
java -jar rascal.jar
```

En el REPL de Rascal:
```rascal
rascal> import Main;
rascal> main();
```

### Analizar un archivo específico
Edita `Main.rsc` y cambia el archivo a analizar:
```rascal
void main() {
  analyzeFile(|project://swimmingdsl/tu_archivo.swim|);
}
```

### Generar una sesión
En el REPL de Rascal:
```rascal
rascal> import Semantics;
rascal> import AST;
rascal> import IO;

// Generar sesión de resistencia
rascal> Session s = generateSession(generatorConfig(
           endurance(),
           3000,
           [freestyle(), backstroke()],
           60
        ));
rascal> println(s);

// Generar sesión de velocidad
rascal> Session s = generateSession(generatorConfig(
           speed(),
           2000,
           [freestyle()],
           45
        ));
rascal> println(s);
```

## Estructura del Proyecto

```
swimmingdsl/
├── src/
│   ├── Lexer.rsc          # Tokens y keywords
│   ├── SwimSyntax.rsc     # Gramática del DSL
│   ├── AST.rsc            # Árbol de sintaxis abstracta
│   ├── Semantics.rsc      # Análisis semántico y generador
│   ├── Main.rsc           # Punto de entrada y análisis
│   ├── TypeChecker.rsc    # (futuro) Validaciones
│   └── Visitors.rsc       # (futuro) Visitadores del AST
├── example.swim           # Ejemplo básico
├── simple_advanced.swim   # Ejemplo con todas las features
├── advanced.swim          # Ejemplo con secciones
├── generator.swim         # Ejemplo de generador
├── file.swim             # Archivo de prueba original
├── rascal.jar            # Rascal MPL runtime
├── pom.xml               # Configuración Maven
└── README.md             # Este archivo
```

## Ejemplos

### Ejemplo 1: Sesión de Resistencia
```swim
session endurance {
  warmup {
    swim 800 m freestyle easy pace 110
  }
  
  main {
    8 x swim 400 m freestyle moderate pace 100 rest 45 s
  }
  
  cooldown {
    swim 400 m easy pace 120
  }
}
```

### Ejemplo 2: Sesión de Velocidad
```swim
session speed {
  warmup {
    swim 400 m easy pace 120
    4 x swim 50 m hard pace 50 rest 30 s
  }
  
  main {
    16 x swim 25 m butterfly hard pace 30 rest 20 s
    8 x swim 50 m freestyle hard pace 45 rest 30 s
  }
  
  cooldown {
    swim 200 m easy pace 130
  }
}
```

### Ejemplo 3: Sesión de Técnica
```swim
session technique {
  warmup {
    swim 600 m easy pace 120
  }
  
  main {
    drill catchup 300 m easy
    drill fingertip 300 m easy
    drill onesided 300 m moderate
    4 x drill sculling 50 m easy rest 20 s
  }
  
  cooldown {
    swim 200 m easy with snorkel
  }
}
```

### Ejemplo 4: Sesión Mixta con Equipamiento
```swim
session mixed {
  warmup {
    swim 400 m easy pace 120 with fins
  }
  
  main {
    swim 300 m freestyle moderate with paddles
    kick 200 m hard with board
    swim 200 m easy with pullbuoy
    4 x swim 100 m freestyle hard pace 75 rest 20 s
  }
  
  cooldown {
    swim 200 m easy with snorkel
  }
}
```

## Características Futuras

- [ ] Exportación a JSON/CSV
- [ ] Visualización de gráficas
- [ ] Cálculo de calorías quemadas
- [ ] Training Stress Score 
- [ ] Validaciones avanzadas (warnings)
- [ ] Comparación de sesiones
- [ ] Exportación a formatos de dispositivos (TCX/FIT)
- [ ] Historial de entrenamientos



---
