module SwimSyntax

extend Lexer;

start syntax Program
  = program: Session+;

syntax Session
  = session: "session" ID "{" Block+ "}";

syntax Block
  = single: Set
  | interval: INT "x" Set Rest?;

syntax Set
  = swim: "swim" INT "m" Pace?
  | kick: "kick" INT "m";

syntax Pace
  = pace: "pace" INT;

syntax Rest
  = rest: "rest" INT "s";