module SwimSyntax

extend Lexer;

start syntax Program
  = Session+;

syntax Session
  = "session" ID "{" Block+ "}";

syntax Block
  = Set
  | INT "x" Set Rest?;

syntax Set
  = "swim" INT "m" Pace?
  | "kick" INT "m";

syntax Pace
  = "pace" INT;

syntax Rest
  = "rest" INT "s";