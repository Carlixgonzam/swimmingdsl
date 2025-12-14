module Main

import IO;
import ParseTree;
import SwimSyntax;

void main(list[str] args) {
  if (size(args) != 1) {
    println("Usage: rascal Main <file.swim>");
    return;
  }

  str input = readFile(args[0]);

  parse(#Program, input);

  println("Parsed successfully");
}