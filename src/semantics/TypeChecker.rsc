module semantics::TypeChecker
import ast::AST;


bool wellTyped(Block b) {
  switch(b) {
    case Single(_): return true;
    case Interval(r, _, _): return r > 0;
  }
}

