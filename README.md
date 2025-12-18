# Swimming DSL 🏊‍♀️

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

## 📂 Estructura del Proyecto

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
- [ ] Training Stress Score (TSS)
- [ ] Validaciones avanzadas (warnings)
- [ ] Comparación de sesiones
- [ ] Exportación a formatos de dispositivos (TCX/FIT)
- [ ] Historial de entrenamientos

## Contribuciones

Las contribuciones son bienvenidas! Por favor:
1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## Licencia

Este proyecto fue creado con fines educacionales.

## 👤 Autora

**Carla González Mina**

---
