module Main

import IO;
import ParseTree;
import String;
import Map;
import SwimSyntax;
import AST;
import Semantics;

void main() {
  analyzeFile(|project://swimmingdsl/simple_advanced.swim|);
}

void analyzeFile(loc file) {
  str input = readFile(file);
  println("SWIMMING DSL Session Analysis");
  println("File: <file.file>");
  println();
  
  println("PARSING...");
  try {
    Tree tree = parse(#start[Program], input);
    println("Parse successful!\n");
    println("BASIC ANALYSIS");
    list[str] sessionNames = [];
    int totalDist = 0;
    int sessionCount = 0;
    for (/session\s+<name:[a-zA-Z][a-zA-Z0-9_]*>/ := input) {
      sessionNames += name;
      sessionCount += 1;
    }
    println("total sessions: <sessionCount>");
    println("Session names: <intercalate(", ", sessionNames)>");
    println();
    println("DISTANCE CALCULATION:");
    for (/<d:[0-9]+>\s*m/ := input) {
      int dist = toInt(d);
      totalDist += dist;
    }
    for (/<reps:[0-9]+>\s*x\s*\w+\s*<d:[0-9]+>\s*m/ := input) {
      int r = toInt(reps);
      int dist = toInt(d);
      totalDist += ((r - 1) * dist); 
    }
    
    println("Total distance: <totalDist> meters (<totalDist / 1000.0> km)");
    println();
    println("STROKE ANALYSIS:");
    map[str, int] styleCount = ();
    if (/freestyle/ := input) {
      int count = size([1 | /freestyle/ := input]);
      styleCount["freestyle"] = count;
    }
    if (/backstroke/ := input) {
      int count = size([1 | /backstroke/ := input]);
      styleCount["backstroke"] = count;
    }
    if (/breaststroke/ := input) {
      int count = size([1 | /breaststroke/ := input]);
      styleCount["breaststroke"] = count;
    }
    if (/butterfly/ := input) {
      int count = size([1 | /butterfly/ := input]);
      styleCount["butterfly"] = count;
    }
    
    if (size(styleCount) > 0) {
      for (style <- domain(styleCount)) {
        println("<style>: <styleCount[style]> set(s)");
      }
    } else {
      println("no specific strokes defined bb");
    }
    println();
    println("INTENSITY ANALYSIS:");
    map[str, int] intensityCount = ();
    if (/easy/ := input) {
      int count = size([1 | /easy/ := input]);
      intensityCount["easy"] = count;
    }
    if (/moderate/ := input) {
      int count = size([1 | /moderate/ := input]);
      intensityCount["moderate"] = count;
    }
    if (/hard/ := input) {
      int count = size([1 | /hard/ := input]);
      intensityCount["hard"] = count;
    }
    
    if (size(intensityCount) > 0) {
      for (intensity <- domain(intensityCount)) {
        println("<intensity>: <intensityCount[intensity]> set(s)");
      }
    } else {
      println("no intensity levels specified sorry bb");
    }
    println();
    println("REST ANALYSIS:");
    int totalRest = 0;
    int restCount = 0;
    for (/rest\s+<seconds:[0-9]+>\s*s/ := input) {
      int secs = toInt(seconds);
      totalRest += secs;
      restCount += 1;
    }
    if (restCount > 0) {
      println("Total rest periods: <restCount>");
      println("total rest time: <totalRest> seconds (<formatTime(totalRest)>)");
      println("average rest: <totalRest / restCount> seconds");
    } else {
      println("no rest periods defined");
    }
    println();
    println("TIME ESTIMATION:");
    int totalTime = 0;
    int paceCount = 0;
    for (/pace\s+<p:[0-9]+>/ := input) {
      int pace = toInt(p);
      if (/<d:[0-9]+>\s*m.*pace\s+<pace>/ := input) {
        int dist = toInt(d);
        int time = (dist * pace) / 100;
        totalTime += time;
        paceCount += 1;
      }
    }
    
    if (paceCount > 0) {
      println("estimated swim time: <formatTime(totalTime)>");
      println("rest time: <formatTime(totalRest)>");
      println("total session time: <formatTime(totalTime + totalRest)>");
    } else {
      println("no pace information available for time estimation");
    }
    println();
    println("Analysis complete!");
    
  } catch ParseError(loc l): {
    println("parse error at line <l.begin.line>, column <l.begin.column>");
    println("check your syntax!");
  }
}
