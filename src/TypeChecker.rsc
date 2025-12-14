module TypeChecker

import AST;

bool wellTyped(Block b) {
  switch(b) {
    case Single(_): return true;
    case Interval(r, _, _): return r > 0;
    case Interval(r, _): return r > 0;
  }
}