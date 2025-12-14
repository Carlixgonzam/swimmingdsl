module AST

import ParseTree;

data Program = program(list[Session] sessions);
data Session = session(str name, list[Block] blocks);
data Block
  = single(Set s)
  | interval(int reps, Set s, Rest rest)
  | interval(int reps, Set s);  
data Set
  = swim(int meters, Pace pace)
  | swim(int meters)            
  | kick(int meters);
data Pace = pace(int seconds);
data Rest = rest(int seconds);