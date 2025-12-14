module Main

import IO;
import ParseTree;
import SwimSyntax;
import AST;

void main() {
  loc file = |project://swimmingdsl/file.swim|;
  str input = readFile(file);
  
  Tree tree = parse(#Program, input);
  println("Parse tree created successfully");
  
  Program ast = implode(#Program, tree);
  println("AST created successfully");
  println(ast);
}