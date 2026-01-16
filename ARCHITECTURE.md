# Swimming DSL una arquitectura del Proyecto

## visión general

```
┌──────────────────┐
│  Archivo .swim   │  ← input del usuario
│  (simple.swim)   │
└────────┬─────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                        FASE 1: PARSING                          │
│                                                                 │
│  ┌──────────┐      ┌──────────────┐      ┌──────────────┐    │
│  │  Lexer   │ ───► │ SwimSyntax   │ ───► │  ParseTree   │    │
│  │  .rsc    │      │    .rsc      │      │   (Tree)     │    │
│  └──────────┘      └──────────────┘      └──────────────┘    │
│                                                                 │
│  tokens:                Rreglas sintaxis:                      │
│  - INT: [0-9]+          - Program                              │
│  - ID: [a-z]+           - Session                              │
│  - Keywords             - Block                                │
│                         - Exercise                             │
└─────────────────────────────────────────────────────────────────┘
         │
         │ parse(#start[Program], input)
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    FASE 2: AST                       │
│                                                                 │
│  ┌──────────────┐                                              │
│  │   ParseTree  │                                              │
│  │    (Tree)    │                                              │
│  └──────┬───────┘                                              │
│         │                                                       │
│         │ toAST(tree)                                          │
│         │ implode(#Program, tree, lexConverter)                │
│         │                                                       │
│         │ conversión de lex:                             │
│         │ • INT → int (toInt())                                │
│         │ • ID → str  (string interpolation)                   │
│         │                                                       │
│         ▼                                                       │
│  ┌──────────────┐                                              │
│  │  AST.rsc     │  ← tipos de datos algebricos                 │
│  │  (Program)   │                                              │
│  └──────────────┘                                              │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                  FASE 3: ANALISIS SEMÁNTICO                     │
│                                                                 │
│  ┌──────────────┐                                              │
│  │ Semantics    │  ← funciones de análisis                     │
│  │   .rsc       │                                              │
│  └──────────────┘                                              │
│         │                                                       │
│         │ Operaciones:                                         │
│         │ • distance(Exercise) → int                           │
│         │ • blockDistance(Block) → int                         │
│         │ • getPace(Exercise) → int                            │
│         │ • estimatedTime(Exercise) → int                      │
│         │ • generateSession(Config) → Session                  │
│         │                                                       │
│         ▼                                                       │
│  ┌──────────────┐                                              │
│  │   Main.rsc   │  ← el que realiza el análisis                 │
│  └──────────────┘                                              │
│         │                                                       │
│         │ analiza:                                             │
│         │ • distancias totales                                 │
│         │ • distribución de estilos                            │
│         │ • niveles de intensidad                              │
│         │ • tiempos de descanso                                │
│         │ • uso de equipamiento                                │
│         │ • drills técnicos                                    │
│         │ • estimación de tiempo                               │
│         ▼                                                       │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────┐
│  Reporte Final   │  ← output para el usuario
│   (Consola)      │
└──────────────────┘
```

---

## Estructura del AST (Abstract Syntax Tree)

### Jerarquía Completa

```
Program
├─ program(list[Session])
└─ generator(GeneratorConfig)

Session
├─ session(str name, list[Block] blocks)
└─ structuredSession(str name, list[SessionSection] sections)

SessionSection
├─ warmup(list[Block] blocks)
├─ mainSection(list[Block] blocks)
└─ cooldown(list[Block] blocks)

Block
├─ exercise(Exercise ex)
├─ interval(int reps, Exercise ex, int restSeconds)
└─ interval(int reps, Exercise ex)

Exercise
├─ swim(meters, style, intensity, pace, equipment, target)
├─ swim(meters, style, intensity, pace, equipment)
├─ swim(meters, style, intensity, pace, target)
├─ swim(meters, style, intensity, pace)
├─ swim(meters, style, intensity, equipment)
├─ swim(meters, style, intensity)
├─ swim(meters, style, pace)
├─ swim(meters, intensity, pace)
├─ swim(meters, pace)
├─ swim(meters)
├─ kick(meters, intensity, equipment)
├─ kick(meters, intensity)
├─ kick(meters, equipment)
├─ kick(meters)
├─ drill(drillType, meters, intensity)
└─ drill(drillType, meters)

Style: freestyle | backstroke | breaststroke | butterfly | noStyle
Intensity: easy | moderate | hard | noIntensity
Equipment: fins | paddles | board | pullbuoy | snorkel | noEquipment
DrillType: catchup | onesided | fingertip | sixKick | sculling
Target: target(int minutes, int seconds) | noTarget
```

---

## ejemplito

### Input (.swim)
```swim
session morning {
  swim 400 m freestyle easy pace 120
  8 x swim 100 m freestyle hard pace 75 rest 15 s
  drill catchup 200 m easy
}
```

### ParseTree ideal que quiero
```
start[Program](
  program(
    [session(
      ID("morning"),
      [
        exercise(
          swim(INT("400"), freestyle(), easy(), INT("120")...)
        ),
        interval(
          INT("8"),
          swim(INT("100"), freestyle(), hard(), INT("75")...),
          INT("15")
        ),
        exercise(
          drill(catchup(), INT("200"), easy())
        )
      ]
    )]
  )
)
```

### AST despues de la correción
```rascal
program([
  session("morning", [
    exercise(
      swim(400, freestyle(), easy(), 120, noEquipment(), noTarget())
    ),
    interval(
      8,
      swim(100, freestyle(), hard(), 75, noEquipment(), noTarget()),
      15
    ),
    exercise(
      drill(catchup(), 200, easy())
    )
  ])
])
```

### output del anlisis
```
═══════════════════════════════════════════════════════
  SWIMMING DSL - Session Analysis
═══════════════════════════════════════════════════════

DISTANCE CALCULATION:
Total distance: 1400 meters (1.4 km)

STROKE ANALYSIS:
  • freestyle: 9 set(s)

INTENSITY ANALYSIS:
  • easy: 2 set(s)
  • hard: 8 set(s)

REST ANALYSIS:
  Total rest periods: 1
  Total rest time: 105 seconds (1:45)

TECHNIQUE DRILLS:
  • catchup: 1 time(s)

TIME ESTIMATION:
  Estimated swim time: 19:00
  Rest time: 1:45
  Total session time: 20:45
```

---

## flujo de Datos Detallado

### 1. Lexer 
```
Input: "swim 400 m freestyle easy"
       ▼
Tokens: ["swim"(KEYWORD), "400"(INT), "m"(KEYWORD), 
         "freestyle"(KEYWORD), "easy"(KEYWORD)]
```

### 2. Parser 
```
Exercise
  ├─ "swim"
  ├─ INT("400")
  ├─ "m"
  ├─ Style(freestyle)
  └─ Intensity(easy)
```

### 3. ast
```rascal
swim(
  400,              // int (convertido con toInt())
  freestyle(),      // Style
  easy(),          // Intensity
  -1,              // pace (no especificado)
  noEquipment(),   // Equipment
  noTarget()       // Target
)
```

### 4. analisis semantico
```rascal
distance(swim(400, ...)) → 400
estimatedTime(swim(400, freestyle(), easy(), 120, ...))
  → (400 * 120) / 100
  → 480 segundos (8 minutos)
```

---

## puntos clave

### 1. **separacion de Concerns**
- **Lexer**: Define tokens básicos (INT, ID, Keywords)
- **SwimSyntax**: Define gramática específica del dominio
- **AST**: Define estructura de datos limpia (sin detalles sintácticos)
- **Semantics**: Lógica de negocio y cálculos
- **Main**: Orquestación y presentación

### 2. **conversiond e los lexers**
```rascal
implode(#Program, tree, value (Tree t) {
  if (appl(prod(lex("INT"),_,_), _) := t) return toInt("<t>");
  if (appl(prod(lex("ID"),_,_), _) := t) return "<t>";
  fail;
})
```
- Convierte INT (string) → int
- Convierte ID (string) → str
- Elimina nodos intermedios del ParseTree

### 3. **hay muchos constructores**
```rascal
// flexibilidad en la sintaxis
swim(meters)
swim(meters, style, intensity)
swim(meters, style, intensity, pace)
swim(meters, style, intensity, pace, equipment, target)
```

### 4. **pattern matching**
analisis mediante pattern matching recursivo:
```rascal
visit(block) {
  case freestyle(): count["freestyle"] += 1;
  case backstroke(): count["backstroke"] += 1;
  ...
}
```

