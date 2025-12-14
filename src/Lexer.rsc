module Lexer

layout WS = [\t-\n\r\ ]*;

lexical INT = [0-9]+;
lexical ID  = [a-zA-Z][a-zA-Z0-9_]* !>> [a-zA-Z0-9_];

keyword Keywords = "swim" | "kick" | "pace" | "rest" | "x" | "session" | "m" | "s";
