module AST

import ParseTree;
import String;

data Program = Program(list[Session] sessions);
data Session = Session(str name, list[Block] blocks);
data Block
  = Single(Set s)
  | Interval(int reps, Set s, Rest rest)
  | Interval(int reps, Set s);
data Set
  = Swim(int meters, Pace pace)
  | Swim(int meters)
  | Kick(int meters);
data Pace = Pace(int seconds);
data Rest = Rest(int seconds);

Program buildAST(Tree parseTree) {
  list[Session] sessions = [];
  
  visit(parseTree) {
    case Tree t: {
      str treeStr = "<t>";
      
      if (/session/ := treeStr) {
        try {
          sessions += extractSession(t);
        } catch: ; 
      }
    }
  }
  
  return Program(sessions);
}

Session extractSession(Tree t) {
  str content = "<t>";
  
  if (/<n:[a-zA-Z][a-zA-Z0-9_]*>/ := content) {
    return Session(n, []);
  }
  
  return Session("no hay jajja", []);
}
