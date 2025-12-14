module TypeChecker

import AST;

bool wellTyped(Block b) {
  switch(b) {
    case single(_): return true;
    case interval(r, _, _): return r > 0;
    case interval(r, _): return r > 0;
  }
}