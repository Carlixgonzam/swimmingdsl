module SwimSyntax

extend Lexer;

start syntax Program
  = Session+;

syntax Session
  = "session" ID "{" Block* "}";


syntax Block
  = Interval
  | Exercise;

syntax Interval
  = INT "x" Exercise Rest?;

syntax Exercise
  = SwimExercise
  | KickExercise;

syntax SwimExercise
  = "swim" INT "m" Modifier*;

syntax KickExercise
  = "kick" INT "m" Modifier*;

syntax Modifier
  = Style
  | Intensity
  | Pace;

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
