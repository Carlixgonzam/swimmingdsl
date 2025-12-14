module Lexer
import IO;


layout WS = [\t-\n\r\ ]*;                    
lexical INT = [0-9]+;
lexical ID  = [a-zA-Z][a-zA-Z0-9_]*;           
lexical SWIM = "swim";
lexical KICK = "kick";
lexical PACE = "pace";
lexical REST = "rest";
lexical X = "x";
lexical SESSION = "session";



