module ast::AST
import ParseTree;
import String;
import List;

data Program
  = Program(list[Session] sessions);

data Session
  = Session(str name, list[Block] blocks);

data Block
  = SingleSet(Set seti)
  | Interval(int reps, Set seti, Rest rest);

data Set
  = Swim(int meters, Pace pace)
  | Kick(int meters);

data Pace
  = Pace(int seconds);

data Rest
  = Rest(int seconds);

Program toAST((Program) `<Session* sessions>`) =
  Program(toAST(sessions));

list[Session] toAST(list[Session] sessions) =
  [toAST(s) | s <- sessions];

Session toAST((Session) `SWIM <ID name> { <Block* blocks> }`) =
  Session(name.symbol, [toAST(b) | b <- blocks]);

list[Block] toAST(list[Block] blocks) =
  [toAST(b) | b <- blocks];

Block toAST((Block) `<Set seti>`) =
  SingleSet(toAST(seti));

Block toAST((Block) `<INT reps> x <Set seti> <Rest? rest>`) {
  int r = toInt(reps.symbol);
  Set s = toAST(seti);
  
  if (rest == []) {
    return Interval(r, s, Rest(0));
  } else {
    return Interval(r, s, toAST(rest[0]));
  }
}

Set toAST((Set) `swim <INT meters> m <Pace? pace>`) {
  int m = toInt(meters.symbol);
  
  if (pace == []) {
    return Swim(m, Pace(0));
  } else {
    return Swim(m, toAST(pace[0]));
  }
}

Set toAST((Set) `kick <INT meters> m`) =
  Kick(toInt(meters.symbol));

Pace toAST((Pace) `pace <INT seconds>`) =
  Pace(toInt(seconds.symbol));

Rest toAST((Rest) `rest <INT seconds> s`) =
  Rest(toInt(seconds.symbol));