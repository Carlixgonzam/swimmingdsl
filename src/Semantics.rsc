module Semantics

import AST;

int distance(Set s) {
  switch(s) {
    case Swim(m, _): return m;
    case Swim(m): return m;
    case Kick(m): return m;
  }
}

int blockDistance(Block b) {
  switch(b) {
    case Single(s): return distance(s);
    case Interval(r, s, _): return r * distance(s);
    case Interval(r, s): return r * distance(s);
  }
}