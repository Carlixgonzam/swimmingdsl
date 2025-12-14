module Main

import IO;
import ParseTree;
import ast::AST;
import syntaxis::SwimSyntax;
import semantics::Visitors;

void main(list[str] args) {
  if (size(args) != 1) {
    println("Usage: rascal Main <file.swim>");
    return;
  }

  str inputProgram = readFile(args[0]);

  Tree pt = parse(#Program, inputProgram);
  Program ast = toAST(pt);

  println("Program parsed successfully");

  int total = totalDistance(ast);
  println("Total distance: <total> meters");
}