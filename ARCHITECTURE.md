# Swimming DSL — Project Architecture

## Overview

```
  .swim file            user input
  (simple.swim)

        down to

  PHASE 1: PARSING

    Lexer.rsc  ---->  SwimSyntax.rsc  ---->  ParseTree (Tree)

    tokens:                 syntax rules:
    - INT: [0-9]+           - Program
    - ID: [a-z]+             - Session
    - Keywords               - Block
                              - Exercise

        down to  (parse(#start[Program], input))

  PHASE 2: AST CONSTRUCTION

    ParseTree (Tree)
        down to  toAST(tree)
                 implode(#Program, tree, lexConverter)

                 lexical conversion:
                 - INT -> int (toInt())
                 - ID  -> str (string interpolation)

        down to
    AST.rsc (Program)         algebraic data types

  PHASE 3: SEMANTIC ANALYSIS

    Semantics.rsc              analysis functions

        Operations:
        - distance(Exercise) -> int
        - blockDistance(Block) -> int
        - getPace(Exercise) -> int
        - estimatedTime(Exercise) -> int
        - generateSession(Config) -> Session

        down to
    Main.rsc                    drives the analysis

        analyzes:
        - total distance
        - style distribution
        - intensity levels
        - rest times
        - equipment usage
        - technique drills
        - estimated time

        down to

  Final report            console output
```

## AST structure

### Full hierarchy

```
Program
  program(list[Session])
  generator(GeneratorConfig)

Session
  session(str name, list[Block] blocks)
  structuredSession(str name, list[SessionSection] sections)

SessionSection
  warmup(list[Block] blocks)
  mainSection(list[Block] blocks)
  cooldown(list[Block] blocks)

Block
  exercise(Exercise ex)
  interval(int reps, Exercise ex, int restSeconds)
  interval(int reps, Exercise ex)

Exercise
  swim(meters, style, intensity, pace, equipment, target)
  swim(meters, style, intensity, pace, equipment)
  swim(meters, style, intensity, pace, target)
  swim(meters, style, intensity, pace)
  swim(meters, style, intensity, equipment)
  swim(meters, style, intensity)
  swim(meters, style, pace)
  swim(meters, intensity, pace)
  swim(meters, pace)
  swim(meters)
  kick(meters, intensity, equipment)
  kick(meters, intensity)
  kick(meters, equipment)
  kick(meters)
  drill(drillType, meters, intensity)
  drill(drillType, meters)

Style: freestyle | backstroke | breaststroke | butterfly | noStyle
Intensity: easy | moderate | hard | noIntensity
Equipment: fins | paddles | board | pullbuoy | snorkel | noEquipment
DrillType: catchup | onesided | fingertip | sixKick | sculling
Target: target(int minutes, int seconds) | noTarget
```

## Worked example

### Input (.swim)

```swim
session morning {
  swim 400 m freestyle easy pace 120
  8 x swim 100 m freestyle hard pace 75 rest 15 s
  drill catchup 200 m easy
}
```

### Concrete parse tree

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

### AST after conversion

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

### Analysis output

```
  SWIMMING DSL - Session Analysis

DISTANCE CALCULATION:
Total distance: 1400 meters (1.4 km)

STROKE ANALYSIS:
  freestyle: 9 set(s)

INTENSITY ANALYSIS:
  easy: 2 set(s)
  hard: 8 set(s)

REST ANALYSIS:
  Total rest periods: 1
  Total rest time: 105 seconds (1:45)

TECHNIQUE DRILLS:
  catchup: 1 time(s)

TIME ESTIMATION:
  Estimated swim time: 19:00
  Rest time: 1:45
  Total session time: 20:45
```

## Detailed data flow

### 1. Lexer

```
Input:  "swim 400 m freestyle easy"
        down to
Tokens: ["swim"(KEYWORD), "400"(INT), "m"(KEYWORD),
         "freestyle"(KEYWORD), "easy"(KEYWORD)]
```

### 2. Parser

```
Exercise
  "swim"
  INT("400")
  "m"
  Style(freestyle)
  Intensity(easy)
```

### 3. AST

```rascal
swim(
  400,              // int (converted with toInt())
  freestyle(),      // Style
  easy(),           // Intensity
  -1,               // pace (not specified)
  noEquipment(),    // Equipment
  noTarget()        // Target
)
```

### 4. Semantic analysis

```rascal
distance(swim(400, ...)) -> 400
estimatedTime(swim(400, freestyle(), easy(), 120, ...))
  -> (400 * 120) / 100
  -> 480 seconds (8 minutes)
```

## Key design points

### 1. Separation of concerns

- **Lexer**: defines the base tokens (INT, ID, keywords)
- **SwimSyntax**: defines the domain-specific grammar
- **AST**: defines a clean data structure, stripped of syntactic detail
- **Semantics**: business logic and calculations
- **Main**: orchestration and presentation

### 2. Lexical conversion

```rascal
implode(#Program, tree, value (Tree t) {
  if (appl(prod(lex("INT"),_,_), _) := t) return toInt("<t>");
  if (appl(prod(lex("ID"),_,_), _) := t) return "<t>";
  fail;
})
```

- Converts INT (string) to int
- Converts ID (string) to str
- Strips intermediate nodes from the parse tree

### 3. Constructor overloading instead of optional fields

```rascal
// flexibility in the grammar
swim(meters)
swim(meters, style, intensity)
swim(meters, style, intensity, pace)
swim(meters, style, intensity, pace, equipment, target)
```

Rather than modeling optional parameters as nullable fields, each valid combination is its own constructor. This keeps pattern matching exhaustive and explicit, at the cost of enumerating every combination by hand.

### 4. Pattern matching

Analysis proceeds through recursive pattern matching over the AST:

```rascal
visit(block) {
  case freestyle(): count["freestyle"] += 1;
  case backstroke(): count["backstroke"] += 1;
  ...
}
```
