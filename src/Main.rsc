module Main

import IO;
import ParseTree;
import String;
import SwimSyntax;
import AST;

void main() {
  loc file = |project://swimmingdsl/file.swim|;
  str input = readFile(file);
  
  println("PARSING");
  Tree tree = parse(#start[Program], input);
  println("parseo exitoso");
  println();
  
  println("PARSE TREE");
  println(tree);
  println();
  
  println("MANUAL PROCESSING");
  if (/session\s+<name:[a-zA-Z][a-zA-Z0-9_]*>/ := input) {
    println("sesion encontrada: <name>");  
  }
  
  int totalDist = 0;
  
  for (/<d:[0-9]+>\s*m/ := input) {
    int dist = toInt(d);
    println("distancia encontrada: <dist>m");
    totalDist += dist;
  }
  
  for (/<reps:[0-9]+>\s*x\s*\w+\s*<d:[0-9]+>\s*m/ := input) {
    int r = toInt(reps);
    int dist = toInt(d);
    println("intervalo encontrado: <r> x <dist>m = <r * dist>m");
    totalDist += (r * dist);
  }
  
  println();
  println("RESULT");
  println("distancia total: <totalDist> metros");
}