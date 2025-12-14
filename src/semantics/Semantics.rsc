module semantics::Semantics
import ast::AST;


int distance(Set s) {
  switch(s) {
    case Swim(m, _): return m;
    case Kick(m): return m;
  }
}

int blockDistance(Block b) {
  switch(b) {
    case Single(s): return distance(s);
    case Interval(r, s, _): return r * distance(s);
  }
}
