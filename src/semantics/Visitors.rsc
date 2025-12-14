module Visitors
import AST;
import Semantics;


int totalDistance(Program p) {
  int total = 0;
  visit(p) {
    case Block b:
      total += blockDistance(b);
  }
  return total;
}