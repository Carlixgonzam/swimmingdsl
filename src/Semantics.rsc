module Semantics

import AST;

int distance(Set s) {
  switch(s) {
    case swim(m, _): return m;
    case swim(m): return m;
    case kick(m): return m;
  }
}

int blockDistance(Block b) {
  switch(b) {
    case single(s): return distance(s);
    case interval(r, s, _): return r * distance(s);
    case interval(r, s): return r * distance(s);
  }
}