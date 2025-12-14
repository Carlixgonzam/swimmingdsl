module syntaxis::SwimSyntax

import lexer::Lexer;

syntax Program
  = Session+;

syntax Session
  = SESSION ID "{" Block+ "}"; 

syntax Block
  = Set
  | INT X Set Rest?;

syntax Set
  = SWIM INT "m" Pace?
  | KICK INT "m";

syntax Pace
  = PACE INT;

syntax Rest
  = REST INT "s";