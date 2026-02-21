# Arquitectura de Conexión: Kotlin Compose ↔ Rascal

## Índice

1. [Visión General](#visión-general)
2. [¿Qué es Rascal y cómo se ejecuta?](#qué-es-rascal-y-cómo-se-ejecuta)
3. [¿Qué es Kotlin Compose for Desktop?](#qué-es-kotlin-compose-for-desktop)
4. [Mecanismo de Conexión: Comunicación por Procesos](#mecanismo-de-conexión-comunicación-por-procesos)
5. [Flujo Detallado: Análisis de Código](#flujo-detallado-análisis-de-código)
6. [Flujo Detallado: Generación de Sesiones](#flujo-detallado-generación-de-sesiones)
7. [Extracción de JSON desde la Salida de Rascal](#extracción-de-json-desde-la-salida-de-rascal)
8. [Deserialización: De JSON a Objetos Kotlin](#deserialización-de-json-a-objetos-kotlin)
9. [Manejo de Concurrencia](#manejo-de-concurrencia)
10. [Ajuste de Distancia Post-Generación](#ajuste-de-distancia-post-generación)
11. [Comparación con la Versión Web](#comparación-con-la-versión-web)
12. [Diagrama Completo del Flujo de Datos](#diagrama-completo-del-flujo-de-datos)

---

## Visión General

La aplicación de escritorio en Kotlin Compose se comunica con el backend de Rascal mediante **Inter-Process Communication (IPC)**. No se usa una conexión de red (HTTP), ni se importan clases de Rascal directamente en Kotlin. En su lugar, Kotlin lanza un **proceso JVM independiente** que ejecuta Rascal, le pasa los argumentos por línea de comandos, lee la respuesta JSON que Rascal imprime por **stdout**, y la deserializa a objetos Kotlin.

```
┌─────────────────────────┐              ┌─────────────────────────┐
│     Proceso JVM #1      │   spawn      │     Proceso JVM #2      │
│  ┌───────────────────┐  │  ────────►   │  ┌───────────────────┐  │
│  │  Kotlin/Compose   │  │              │  │  Rascal Shell     │  │
│  │  (Interfaz de     │  │   args[]     │  │  (Interpreta      │  │
│  │   usuario)        │──┼──────────►   │  │   archivos .rsc)  │  │
│  │                   │  │              │  │                   │  │
│  │  RascalService    │  │   stdout     │  │  WebAPI.rsc       │  │
│  │  (lee respuesta)  │◄─┼──────────    │  │  (imprime JSON)   │  │
│  └───────────────────┘  │              │  └───────────────────┘  │
└─────────────────────────┘              └─────────────────────────┘
     La app del usuario                    Se crea y destruye por
     (siempre corriendo)                   cada operación
```

---

## ¿Qué es Rascal y cómo se ejecuta?

**Rascal** es un lenguaje de metaprogramación diseñado para el análisis y transformación de programas. Corre sobre la JVM mediante un intérprete empaquetado en `rascal-shell-stable.jar`.

Rascal **no** es una librería Java/Kotlin convencional. No expone clases públicas con una API que se pueda importar con `import`. Es un lenguaje interpretado: su JAR contiene el intérprete que lee archivos `.rsc` y los ejecuta.

### Ejecución por línea de comandos

Para ejecutar código Rascal, se invoca:

```bash
java -Drascal.projectPath=<ruta_al_src> -jar rascal-shell-stable.jar <modulo.rsc> <comando> <args...>
```

Por ejemplo, para analizar código DSL:

```bash
java -Drascal.projectPath=/Users/.../swimmingdsl/src \
     -jar rascal-shell-stable.jar \
     Runner.rsc \
     analyze \
     "session morning { swim 400 m freestyle easy pace 120 }"
```

Rascal interpreta `Runner.rsc`, que delega a `WebAPI.rsc`, ejecuta la función correspondiente (`analyzeToJSON` o `generateToJSON`), e imprime el resultado como JSON por stdout.

### Archivos Rascal del proyecto

| Archivo | Responsabilidad |
|---------|----------------|
| `Runner.rsc` | Punto de entrada, delega a WebAPI |
| `WebAPI.rsc` | CLI con comandos `analyze` y `generate`, conversión a JSON |
| `SwimSyntax.rsc` | Gramática del DSL (define la sintaxis válida) |
| `AST.rsc` | Tipos de datos algebraicos (Session, Block, Exercise, etc.) |
| `Semantics.rsc` | Cálculos de distancia/tiempo y generadores de sesiones |

---

## ¿Qué es Kotlin Compose for Desktop?

**Jetpack Compose for Desktop** es un framework de UI declarativo de JetBrains para crear aplicaciones de escritorio con Kotlin. Corre sobre la JVM, lo que significa que nuestra aplicación de escritorio es un proceso Java.

### Dependencias clave del proyecto

- **Compose Desktop**: Framework de UI (`org.jetbrains.compose`)
- **kotlinx-serialization-json**: Deserialización de JSON a data classes
- **kotlinx-coroutines**: Programación asíncrona para no bloquear la UI

---

## Mecanismo de Conexión: Comunicación por Procesos

La clase `RascalService.kt` es el puente entre Kotlin y Rascal. Usa la clase `ProcessBuilder` de Java para crear procesos hijos.

### ¿Qué es ProcessBuilder?

`ProcessBuilder` es una clase de la librería estándar de Java (`java.lang.ProcessBuilder`) que permite:

1. **Crear un proceso del sistema operativo** (como si ejecutaras un comando en la terminal)
2. **Pasar argumentos** al proceso
3. **Leer su salida estándar (stdout)** y error estándar (stderr)
4. **Esperar** a que el proceso termine
5. **Obtener el código de salida** (0 = éxito)

### Construcción del comando

```kotlin
private fun executeRascal(command: String, args: List<String>): String {
    val shellCmd = listOf(
        "java",                                              // Ejecutable: JVM
        "-Dfile.encoding=UTF-8",                             // Codificación UTF-8
        "-Drascal.projectPath=${srcDir.absolutePath}",       // Propiedad: dónde están los .rsc
        "-jar", rascalJar.absolutePath,                      // JAR de Rascal
        "Runner.rsc",                                        // Módulo de entrada
        command                                              // "analyze" o "generate"
    ) + args                                                 // Argumentos adicionales

    val process = ProcessBuilder(shellCmd)
        .directory(srcDir)           // Directorio de trabajo
        .redirectErrorStream(false)  // Mantener stderr separado
        .start()                     // ¡Lanza el proceso!

    process.outputStream.close()     // Cerrar stdin (no enviamos input)

    val stdout = process.inputStream.bufferedReader().readText()  // Leer TODA la salida
    val stderr = process.errorStream.bufferedReader().readText()  // Leer errores

    val finished = process.waitFor(30, TimeUnit.SECONDS)  // Esperar máx 30 segundos
    if (!finished) {
        process.destroyForcibly()  // Matar si se cuelga
        throw RuntimeException("Rascal timeout (30s)")
    }

    return stdout  // Devolver la salida para extraer el JSON
}
```

### Lo que sucede a nivel de sistema operativo

1. `ProcessBuilder.start()` → el SO crea un nuevo proceso (fork + exec en Unix)
2. El nuevo proceso ejecuta `java -jar rascal-shell-stable.jar ...`
3. Esto arranca una **segunda instancia de la JVM** completamente independiente
4. Rascal se carga dentro de esa JVM, interpreta los `.rsc`, ejecuta el comando
5. El resultado se imprime por stdout
6. El proceso termina (la segunda JVM se destruye)
7. Kotlin lee el stdout y continúa

### Resolución de rutas

`RascalService` localiza automáticamente los archivos necesarios:

```
swimmingdsl/                          ← projectRoot
├── rascal-shell-stable.jar           ← rascalJar
├── src/                              ← srcDir (directorio de trabajo para Rascal)
│   ├── Runner.rsc
│   ├── WebAPI.rsc
│   ├── SwimSyntax.rsc
│   ├── AST.rsc
│   └── Semantics.rsc
└── kotlin-app/                       ← desde aquí se ejecuta la app
    └── src/main/kotlin/swimming/
        └── service/RascalService.kt
```

La resolución es relativa: la app Kotlin corre desde `kotlin-app/`, y `RascalService` sube un nivel (`..`) para encontrar el JAR y el directorio `src/`.

---

## Flujo Detallado: Análisis de Código

Cuando el usuario escribe código DSL y presiona "Analizar":

### Paso 1 — UI dispara la acción

```
EditorPanel.kt → botón "Analizar" → onAnalyze(code)
Main.kt → doAnalyze(code) → lanza coroutine
```

### Paso 2 — Kotlin invoca a Rascal

```kotlin
// RascalService.kt
suspend fun analyze(code: String): AnalysisResult = withContext(Dispatchers.IO) {
    val output = executeRascal("analyze", listOf(code))
    // ...
}
```

Esto ejecuta:

```bash
java -Dfile.encoding=UTF-8 \
     -Drascal.projectPath=/Users/.../swimmingdsl/src \
     -jar /Users/.../swimmingdsl/rascal-shell-stable.jar \
     Runner.rsc \
     analyze \
     "session morning { swim 400 m freestyle easy pace 120 }"
```

### Paso 3 — Rascal procesa el código

Dentro de la segunda JVM:

```
Runner.rsc → main(args)
  → WebAPI.rsc → main(["analyze", "session morning {...}"])
    → analyzeToJSON(code)
      → parse(#start[Program], code)    // Parsea con la gramática
      → Extrae métricas con regex:
          - Distancia: /<d:[0-9]+>\s*m/
          - Intervalos: /<reps:[0-9]+>\s*x\s*\w+\s*<d:[0-9]+>\s*m/
          - Estilos, intensidades, equipamiento, drills, descansos, tiempo
      → Construye mapa de resultados
      → toJSON(result)                  // Serializa a JSON
      → println(jsonStr)               // Imprime por stdout
```

### Paso 4 — Rascal imprime JSON por stdout

```json
{"success":true,"sessionCount":1,"sessionNames":["morning"],"totalDistance":400,
 "distanceKm":0.40,"styles":{"freestyle":1},"intensities":{"easy":1},
 "equipment":{},"drills":{},"rest":{"periods":0,"totalSeconds":0,"average":0},
 "time":{"swimSeconds":480,"restSeconds":0,"totalSeconds":480}}
```

### Paso 5 — Kotlin extrae y deserializa

```
stdout (string con posible basura de Rascal)
  → extractJson(output)     // Encuentra el JSON válido
  → Json.decodeFromString<AnalysisResult>(jsonStr)
  → AnalysisResult(success=true, totalDistance=400, ...)
```

### Paso 6 — UI se actualiza

```
AnalysisResult → Main.kt actualiza estado
  → AnalysisPanel.kt recompone la UI
  → Muestra: 400 metros, 1 sesión, 8:00 tiempo total, etc.
```

---

## Flujo Detallado: Generación de Sesiones

Cuando el usuario configura parámetros y presiona "Generar Sesión":

### Paso 1 — UI recoge parámetros

```
GeneratorPanel.kt:
  goal = "speed"
  distance = 2500
  styles = ["freestyle", "backstroke", "breaststroke", "butterfly"]
  duration = 60
```

### Paso 2 — Kotlin invoca a Rascal

```bash
java -jar rascal-shell-stable.jar Runner.rsc generate speed 2500 freestyle,backstroke,breaststroke,butterfly 60
```

### Paso 3 — Rascal genera la sesión

```
WebAPI.rsc → generateToJSON("speed", 2500, ["freestyle",...], 60)
  → Construye GeneratorConfig
  → Semantics.rsc → generateSpeedSession(config)
    → warmupDist = (2500 * 20) / 100 = 500
    → mainDist = (2500 * 60) / 100 = 1500
    → distPerStyle = 1500 / 4 = 375  (división entera)
    → repsPerStyle = 375 / 50 = 7    (división entera, pierde 25m × 4 = 100m)
    → cooldownDist = (2500 * 20) / 100 = 500
    → Retorna AST: structuredSession("generated_speed", [warmup, main, cooldown])
  → sessionToDSL(session)   // Convierte AST a código DSL
  → toJSON(result)           // JSON con el código
  → println(jsonStr)
```

### Paso 4 — Rascal retorna JSON

```json
{
  "success": true,
  "code": "session generated_speed {\n  warmup {\n    swim 500 m easy pace 120\n  }\n  main {\n    7 x swim 50 m freestyle hard pace 60 rest 45 s\n    7 x swim 50 m backstroke hard pace 60 rest 45 s\n    7 x swim 50 m breaststroke hard pace 60 rest 45 s\n    7 x swim 50 m butterfly hard pace 60 rest 45 s\n  }\n  cooldown {\n    swim 500 m easy pace 130\n  }\n}",
  "goal": "speed",
  "distance": 2500
}
```

### Paso 5 — Kotlin ajusta la distancia

```kotlin
// GeneratorPanel.kt
val adjustedCode = adjustGeneratedDistance(result.code, dist)
// Detecta: código suma 2400m, se pidieron 2500m → diferencia de 100m
// Ajusta cooldown: swim 500 m → swim 600 m
// Nuevo total: 500 + 1400 + 600 = 2500m ✓
```

### Paso 6 — Se ejecuta análisis automático

```
onCodeGenerated(adjustedCode)  → Pone el código ajustado en el editor
onAnalyze(adjustedCode)        → Ejecuta análisis (repite el flujo de análisis)
                               → Rascal confirma: totalDistance = 2500 ✓
```

---

## Extracción de JSON desde la Salida de Rascal

La salida de Rascal por stdout no es JSON puro. Puede contener mensajes del intérprete, warnings, códigos ANSI de color, etc. Por eso se necesita un algoritmo para **encontrar el JSON válido** dentro de la salida.

### Algoritmo de `extractJson()`

```
1. Limpiar códigos de escape ANSI (colores de terminal)
2. Escanear la cadena buscando '{'
3. Para cada '{' encontrado:
   a. Contar profundidad de llaves (manejo de anidamiento)
   b. Respetar strings (no contar llaves dentro de "...")
   c. Manejar caracteres escapados dentro de strings
   d. Cuando profundidad = 0, se encontró un JSON candidato
4. Intentar parsear el candidato como JSON
5. Si tiene campo "success", es nuestro resultado → guardar
6. Continuar escaneando (puede haber múltiples JSONs)
7. Retornar el ÚLTIMO JSON válido con "success"
```

Este algoritmo es idéntico al usado en `server.js` de la versión web, traducido de JavaScript a Kotlin.

---

## Deserialización: De JSON a Objetos Kotlin

Se usa `kotlinx-serialization-json` para convertir el JSON a data classes de Kotlin.

### Data classes serializables

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

La anotación `@Serializable` permite que `Json.decodeFromString<T>()` convierta automáticamente el JSON en instancias de estas clases. El parámetro `ignoreUnknownKeys = true` permite que funcione incluso si Rascal agrega campos nuevos al JSON.

---

## Manejo de Concurrencia

La ejecución de Rascal tarda varios segundos (arranque de JVM + interpretación). Para no congelar la interfaz gráfica:

### Coroutines de Kotlin

```kotlin
// Main.kt — lanza en background
val doAnalyze: (String) -> Unit = { code ->
    scope.launch {                          // Coroutine en el scope de Compose
        isLoading = true
        analysisResult = rascalService.analyze(code)  // Suspende aquí
        isLoading = false
    }
}

// RascalService.kt — ejecuta en hilo de IO
suspend fun analyze(code: String): AnalysisResult =
    withContext(Dispatchers.IO) {            // Cambia al thread pool de IO
        // ProcessBuilder, lectura de stdout, etc.
        // Este código corre en un hilo background
    }
```

- `Dispatchers.IO` mueve la ejecución a un pool de hilos optimizado para operaciones bloqueantes (I/O, procesos)
- La UI sigue respondiendo mientras Rascal procesa
- Cuando la coroutine termina, el estado se actualiza y Compose recompone la UI automáticamente

---

## Ajuste de Distancia Post-Generación

Rascal usa **división entera** al distribuir distancias entre estilos, lo que puede causar pérdida de metros. La función `adjustGeneratedDistance()` en Kotlin compensa esto.

### Ejemplo concreto: Speed, 2500m, 4 estilos

```
Rascal genera:
  mainDist = 2500 × 60% = 1500
  distPerStyle = 1500 / 4 = 375     ← división exacta
  repsPerStyle = 375 / 50 = 7       ← pierde 375 - (7×50) = 25m por estilo
  Pérdida total: 25 × 4 = 100m

Código generado suma: 500 + (7×50×4) + 500 = 2400m (faltan 100m)
```

### Algoritmo de ajuste (en Kotlin)

```
1. Calcular distancia del código con las MISMAS regex que Rascal:
   - Sumar todos los "N m"
   - Para cada "N x WORD N m", sumar (N-1) × distancia
   → Resultado: 2400

2. Diferencia: 2500 - 2400 = 100m

3. Encontrar el último "swim N m" (el del cooldown)
   → "swim 500 m"

4. Ajustar: 500 + 100 = 600
   → "swim 600 m"

5. Total ajustado: 500 + 1400 + 600 = 2500m ✓
```

Esto es una corrección puramente en Kotlin que no modifica el backend de Rascal.

---

## Comparación con la Versión Web

Ambas versiones usan la **misma estrategia** de comunicación con Rascal (proceso externo + stdout), pero con una capa intermedia diferente:

### Versión Web

```
Browser (HTML/JS)
    │
    │  HTTP (fetch)
    ▼
Node.js / Express (server.js)          ← Proceso #1
    │
    │  child_process.spawn("java", [...])
    ▼
JVM + Rascal                           ← Proceso #2
    │
    │  stdout → JSON
    ▼
Node.js parsea JSON, agrega campos de tiempo formateado
    │
    │  HTTP Response
    ▼
Browser renderiza resultados
```

### Versión Kotlin Desktop

```
Kotlin Compose (JVM)                   ← Proceso #1
    │
    │  ProcessBuilder("java", [...])
    ▼
JVM + Rascal                           ← Proceso #2
    │
    │  stdout → JSON
    ▼
Kotlin extrae JSON, deserializa, ajusta distancia
    │
    │  Actualización de estado (recomposición)
    ▼
Compose renderiza resultados
```

### Diferencias clave

- **Web**: necesita un servidor HTTP corriendo (`node server.js`) como intermediario
- **Desktop**: se comunica directamente con Rascal, sin servidor intermedio
- **Web**: el formateo de tiempo lo hace el servidor Node.js
- **Desktop**: el formateo de tiempo lo hacen las propiedades calculadas en las data classes Kotlin
- **Desktop**: incluye ajuste de distancia post-generación que la versión web no tiene

---

## Diagrama Completo del Flujo de Datos

```
                        USUARIO
                          │
                ┌─────────┴─────────┐
                │                   │
           Escribe código      Configura generación
           y presiona          y presiona
           "Analizar"         "Generar Sesión"
                │                   │
                ▼                   ▼
          ┌──────────┐      ┌──────────────┐
          │ Editor   │      │ Generator    │
          │ Panel.kt │      │ Panel.kt     │
          └────┬─────┘      └──────┬───────┘
               │                   │
               │            ┌──────┴───────┐
               │            │ RascalService│
               │            │ .generate()  │
               │            └──────┬───────┘
               │                   │
               │            ┌──────▼───────────────────────┐
               │            │ ProcessBuilder:              │
               │            │ java -jar rascal-shell.jar   │
               │            │ Runner.rsc generate          │
               │            │ speed 2500 freestyle,... 60  │
               │            └──────┬───────────────────────┘
               │                   │ stdout: JSON {code, goal, distance}
               │                   │
               │            ┌──────▼───────┐
               │            │ extractJson()│
               │            │ deserialize  │
               │            │ → Generate   │
               │            │   Result     │
               │            └──────┬───────┘
               │                   │
               │            ┌──────▼──────────────┐
               │            │ adjustGenerated     │
               │            │ Distance()          │
               │            │ (compensa int div)  │
               │            └──────┬──────────────┘
               │                   │
               │         código ajustado
               │            ┌──────┴───────┐
               ├────────────┤              │
               │            │ onCodeGenerated()
               │            │ (pone en editor)
               │            └──────────────┘
               │
        ┌──────▼───────┐
        │ RascalService│
        │ .analyze()   │
        └──────┬───────┘
               │
        ┌──────▼───────────────────────────────┐
        │ ProcessBuilder:                      │
        │ java -jar rascal-shell.jar           │
        │ Runner.rsc analyze "session ... {}"  │
        └──────┬───────────────────────────────┘
               │ stdout: JSON {totalDistance, styles, time, ...}
               │
        ┌──────▼───────┐
        │ extractJson()│
        │ deserialize  │
        │ → Analysis   │
        │   Result     │
        └──────┬───────┘
               │
        ┌──────▼───────┐
        │ Analysis     │
        │ Panel.kt     │
        │ (muestra     │
        │  resultados) │
        └──────────────┘
```
