module syntaxis::SwimSyntax
import lexer::Lexer;

syntax Program
  = program: Session+;

syntax Session
  = session: SWIM ID "{" Block+ "}"; 

syntax Block
  = single: Set
  | interval: INT X Set Rest?;

syntax Set
  = swim: SWIM INT "m" Pace?
  | kick: KICK INT "m";

syntax Pace
  = pace: PACE INT;

syntax Rest
  = rest: REST INT "s";