module Main
import IO;
import ParseTree;
import util::Reflect;
import util::Benchmark;
import ast::AST;
import syntaxis::SwimSyntax;
import semantics::TypeChecker;
import semantics::Visitors;
import semantics::Semantics;

void main(list[str] args) {
  if (size(args) != 1) {
    println("Usage: rascal Main <file.swim>");
    return;
  }

  str inputFile = args[0];
  str inputProgram = readFile(inputFile);

  Program parseTree = parse(#Program, inputProgram);
  Program ast = toAST(parseTree);
  println("Program parsed successfully");

  bool isWellTyped = true;
  

  if (!isWellTyped) {
    return;
  }

  
  int total = totalDistance(ast);
  

  println("Total distance: <total> meters");
}
