module SwimSyntax

extend Lexer;

start syntax Program
  = Session+
  | Generator;

syntax Session
  = "session" ID "{" SectionBlock* "}"
  | "session" ID "{" Block* "}";

syntax SectionBlock
  = Section "{" Block* "}";

syntax Section
  = "warmup"
  | "main"
  | "cooldown";

syntax Block
  = Interval
  | Exercise;

syntax Interval
  = INT "x" Exercise Rest?;

syntax Exercise
  = SwimExercise
  | KickExercise
  | DrillExercise;

syntax SwimExercise
  = "swim" INT "m" Style Intensity Pace Equipment Target
  | "swim" INT "m" Style Intensity Pace Equipment
  | "swim" INT "m" Style Intensity Pace Target
  | "swim" INT "m" Style Intensity Pace
  | "swim" INT "m" Style Intensity Equipment
  | "swim" INT "m" Style Intensity
  | "swim" INT "m" Style Pace
  | "swim" INT "m" Intensity Pace
  | "swim" INT "m" Pace
  | "swim" INT "m";

syntax KickExercise
  = "kick" INT "m" Intensity Equipment
  | "kick" INT "m" Intensity
  | "kick" INT "m" Equipment
  | "kick" INT "m";

syntax DrillExercise
  = "drill" DrillType INT "m" Intensity
  | "drill" DrillType INT "m";

syntax Style
  = "freestyle"
  | "backstroke"
  | "breaststroke"
  | "butterfly";

syntax Intensity
  = "easy"
  | "moderate"
  | "hard";

syntax Pace
  = "pace" INT;

syntax Rest
  = "rest" INT "s";

syntax Equipment
  = "with" EquipmentType;

syntax EquipmentType
  = "fins"
  | "paddles"
  | "board"
  | "pullbuoy"
  | "snorkel";

syntax DrillType
  = "catchup"
  | "onesided"
  | "fingertip"
  | "6kick"
  | "sculling";

syntax Target
  = "target" TimeFormat;

syntax TimeFormat
  = INT ":" INT;

syntax Generator
  = "generate" "session" "{" GeneratorOption* "}";

syntax GeneratorOption
  = "goal" ":" Goal
  | "distance" ":" INT
  | "styles" ":" "[" {Style ","}+ "]"
  | "duration" ":" INT "minutes";

syntax Goal
  = "endurance"
  | "speed"
  | "technique"
  | "recovery";
