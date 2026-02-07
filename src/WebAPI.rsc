module WebAPI

import IO;
import ParseTree;
import String;
import Map;
import List;
import util::Math;
import SwimSyntax;
import AST;
import Semantics;

// Convert analysis to JSON string
str toJSON(value v) {
  switch(v) {
    case str s: return "\"<escape(s, ("\"": "\\\"", "\\": "\\\\", "\n": "\\n", "\r": "\\r", "\t": "\\t"))>\"";
    case int i: return "<i>";
    case real r: return "<r>";
    case true: return "true";
    case false: return "false";
    case list[value] lst: {
      if (size(lst) == 0) return "[]";
      return "[<intercalate(",", [toJSON(e) | e <- lst])>]";
    }
    case map[str, value] m: {
      if (size(m) == 0) return "{}";
      str result = "{";
      bool first = true;
      for (k <- m) {
        if (!first) result += ",";
        result += "\"<k>\":<toJSON(m[k])>";
        first = false;
      }
      return result + "}";
    }
    default: return "null";
  }
}

// Analyze and return JSON
str analyzeToJSON(str input) {
  try {
    Tree tree = parse(#start[Program], input);
    
    // Parse to AST would require implementing implode or manually converting
    // For now, we'll do text-based analysis like Main.rsc does
    
    map[str, value] result = ();
    result["success"] = true;
    
    // Count sessions
    list[str] sessionNames = [];
    for (/session\s+<name:[a-zA-Z][a-zA-Z0-9_]*>/ := input) {
      sessionNames += name;
    }
    result["sessionCount"] = size(sessionNames);
    result["sessionNames"] = sessionNames;
    
    // Calculate distance
    int totalDist = 0;
    for (/<d:[0-9]+>\s*m/ := input) {
      totalDist += toInt(d);
    }
    for (/<reps:[0-9]+>\s*x\s*\w+\s*<d:[0-9]+>\s*m/ := input) {
      totalDist += ((toInt(reps) - 1) * toInt(d));
    }
    result["totalDistance"] = totalDist;
    result["distanceKm"] = round(totalDist / 1000.0, 0.01);
    
    // Count styles
    map[str, int] styleCount = ();
    if (/freestyle/ := input) styleCount["freestyle"] = size([1 | /freestyle/ := input]);
    if (/backstroke/ := input) styleCount["backstroke"] = size([1 | /backstroke/ := input]);
    if (/breaststroke/ := input) styleCount["breaststroke"] = size([1 | /breaststroke/ := input]);
    if (/butterfly/ := input) styleCount["butterfly"] = size([1 | /butterfly/ := input]);
    result["styles"] = styleCount;
    
    // Count intensities
    map[str, int] intensityCount = ();
    if (/easy/ := input) intensityCount["easy"] = size([1 | /easy/ := input]);
    if (/moderate/ := input) intensityCount["moderate"] = size([1 | /moderate/ := input]);
    if (/hard/ := input) intensityCount["hard"] = size([1 | /hard/ := input]);
    result["intensities"] = intensityCount;
    
    // Count equipment
    map[str, int] equipmentCount = ();
    if (/with\s+fins/ := input) equipmentCount["fins"] = size([1 | /with\s+fins/ := input]);
    if (/with\s+paddles/ := input) equipmentCount["paddles"] = size([1 | /with\s+paddles/ := input]);
    if (/with\s+board/ := input) equipmentCount["board"] = size([1 | /with\s+board/ := input]);
    if (/with\s+pullbuoy/ := input) equipmentCount["pullbuoy"] = size([1 | /with\s+pullbuoy/ := input]);
    if (/with\s+snorkel/ := input) equipmentCount["snorkel"] = size([1 | /with\s+snorkel/ := input]);
    result["equipment"] = equipmentCount;
    
    // Count drills
    map[str, int] drillCount = ();
    if (/drill\s+catchup/ := input) drillCount["catchup"] = size([1 | /drill\s+catchup/ := input]);
    if (/drill\s+onesided/ := input) drillCount["onesided"] = size([1 | /drill\s+onesided/ := input]);
    if (/drill\s+fingertip/ := input) drillCount["fingertip"] = size([1 | /drill\s+fingertip/ := input]);
    if (/drill\s+6kick/ := input) drillCount["6kick"] = size([1 | /drill\s+6kick/ := input]);
    if (/drill\s+sculling/ := input) drillCount["sculling"] = size([1 | /drill\s+sculling/ := input]);
    result["drills"] = drillCount;
    
    // Rest analysis
    int totalRest = 0;
    int restCount = 0;
    for (/rest\s+<seconds:[0-9]+>\s*s/ := input) {
      totalRest += toInt(seconds);
      restCount += 1;
    }
    map[str, value] restInfo = (
      "periods": restCount,
      "totalSeconds": totalRest,
      "average": restCount > 0 ? totalRest / restCount : 0
    );
    result["rest"] = restInfo;
    
    // Time estimation
    int totalTime = 0;
    int paceCount = 0;
    for (/<d:[0-9]+>\s*m.*?pace\s+<p:[0-9]+>/ := input) {
      int dist = toInt(d);
      int pace = toInt(p);
      totalTime += (dist * pace) / 100;
      paceCount += 1;
    }
    
    map[str, value] timeInfo = (
      "swimSeconds": totalTime,
      "restSeconds": totalRest,
      "totalSeconds": totalTime + totalRest
    );
    result["time"] = timeInfo;
    
    return toJSON(result);
    
  } catch ParseError(loc l): {
    map[str, value] error = (
      "success": false,
      "error": "Parse error at line <l.begin.line>, column <l.begin.column>"
    );
    return toJSON(error);
  } catch value e: {
    map[str, value] error = (
      "success": false,
      "error": "Unexpected error: <e>"
    );
    return toJSON(error);
  }
}

// Generate session and return DSL code
str generateToJSON(str goal, int distance, list[str] styles, int duration) {
  try {
    // Convert strings to Goal and Style types
    Goal g = endurance();
    switch(goal) {
      case "speed": g = speed();
      case "technique": g = technique();
      case "recovery": g = recovery();
    }
    
    list[Style] styleList = [];
    for (s <- styles) {
      switch(s) {
        case "freestyle": styleList += freestyle();
        case "backstroke": styleList += backstroke();
        case "breaststroke": styleList += breaststroke();
        case "butterfly": styleList += butterfly();
      }
    }
    
    if (size(styleList) == 0) styleList = [freestyle()];
    
    GeneratorConfig config = generatorConfig(g, distance, styleList, duration);
    Session session = generateSession(config);
    
    // Convert session to DSL code
    str dslCode = sessionToDSL(session);
    
    map[str, value] result = (
      "success": true,
      "code": dslCode,
      "goal": goal,
      "distance": distance
    );
    
    return toJSON(result);
    
  } catch value e: {
    map[str, value] error = (
      "success": false,
      "error": "Generation error: <e>"
    );
    return toJSON(error);
  }
}

// Convert Session AST to DSL code
str sessionToDSL(Session session) {
  str code = "";
  switch(session) {
    case session(name, blocks): {
      code = "session <name> {\n";
      for (block <- blocks) {
        code += "  <blockToDSL(block)>\n";
      }
      code += "}";
    }
    case structuredSession(name, sections): {
      code = "session <name> {\n";
      for (section <- sections) {
        code += sectionToDSL(section);
      }
      code += "}";
    }
  }
  return code;
}

str sectionToDSL(SessionSection section) {
  str code = "";
  switch(section) {
    case warmup(blocks): {
      code = "  warmup {\n";
      for (block <- blocks) {
        code += "    <blockToDSL(block)>\n";
      }
      code += "  }\n";
    }
    case mainSection(blocks): {
      code = "  main {\n";
      for (block <- blocks) {
        code += "    <blockToDSL(block)>\n";
      }
      code += "  }\n";
    }
    case cooldown(blocks): {
      code = "  cooldown {\n";
      for (block <- blocks) {
        code += "    <blockToDSL(block)>\n";
      }
      code += "  }\n";
    }
  }
  return code;
}

str blockToDSL(Block block) {
  str code = "";
  switch(block) {
    case exercise(ex): code = exerciseToDSL(ex);
    case interval(reps, ex, rest): code = "<reps> x <exerciseToDSL(ex)> rest <rest> s";
    case interval(reps, ex): code = "<reps> x <exerciseToDSL(ex)>";
  }
  return code;
}

str exerciseToDSL(Exercise ex) {
  str code = "";
  switch(ex) {
    case swim(m, style, intensity, pace, equipment, target): {
      code = "swim <m> m";
      if (style != noStyle()) code += " <styleName(style)>";
      if (intensity != noIntensity()) code += " <intensityName(intensity)>";
      code += " pace <pace>";
      if (equipment != noEquipment()) code += " with <equipmentName(equipment)>";
      if (target != noTarget()) code += " <targetToStr(target)>";
    }
    case swim(m, style, intensity, pace, equipment): {
      code = "swim <m> m";
      if (style != noStyle()) code += " <styleName(style)>";
      if (intensity != noIntensity()) code += " <intensityName(intensity)>";
      code += " pace <pace>";
      if (equipment != noEquipment()) code += " with <equipmentName(equipment)>";
    }
    case swim(m, style, intensity, pace, target): {
      code = "swim <m> m";
      if (style != noStyle()) code += " <styleName(style)>";
      if (intensity != noIntensity()) code += " <intensityName(intensity)>";
      code += " pace <pace>";
      if (target != noTarget()) code += " <targetToStr(target)>";
    }
    case swim(m, style, intensity, pace): {
      code = "swim <m> m";
      if (style != noStyle()) code += " <styleName(style)>";
      if (intensity != noIntensity()) code += " <intensityName(intensity)>";
      code += " pace <pace>";
    }
    case swim(m, style, intensity, equipment): {
      code = "swim <m> m";
      if (style != noStyle()) code += " <styleName(style)>";
      if (intensity != noIntensity()) code += " <intensityName(intensity)>";
      if (equipment != noEquipment()) code += " with <equipmentName(equipment)>";
    }
    case swim(m, style, intensity): {
      code = "swim <m> m";
      if (style != noStyle()) code += " <styleName(style)>";
      if (intensity != noIntensity()) code += " <intensityName(intensity)>";
    }
    case swim(m, style, pace): {
      code = "swim <m> m";
      if (style != noStyle()) code += " <styleName(style)>";
      code += " pace <pace>";
    }
    case swim(m, intensity, pace): {
      code = "swim <m> m <intensityName(intensity)> pace <pace>";
    }
    case swim(m, pace): {
      code = "swim <m> m pace <pace>";
    }
    case swim(m): {
      code = "swim <m> m";
    }
    case kick(m, intensity, equipment): {
      code = "kick <m> m <intensityName(intensity)> with <equipmentName(equipment)>";
    }
    case kick(m, intensity): {
      code = "kick <m> m <intensityName(intensity)>";
    }
    case kick(m, equipment): {
      code = "kick <m> m with <equipmentName(equipment)>";
    }
    case kick(m): {
      code = "kick <m> m";
    }
    case drill(drillType, m, intensity): {
      code = "drill <drillName(drillType)> <m> m <intensityName(intensity)>";
    }
    case drill(drillType, m): {
      code = "drill <drillName(drillType)> <m> m";
    }
  }
  return code;
}

str styleName(Style s) {
  switch(s) {
    case freestyle(): return "freestyle";
    case backstroke(): return "backstroke";
    case breaststroke(): return "breaststroke";
    case butterfly(): return "butterfly";
    default: return "";
  }
}

str intensityName(Intensity i) {
  switch(i) {
    case easy(): return "easy";
    case moderate(): return "moderate";
    case hard(): return "hard";
    default: return "";
  }
}

str equipmentName(Equipment e) {
  switch(e) {
    case fins(): return "fins";
    case paddles(): return "paddles";
    case board(): return "board";
    case pullbuoy(): return "pullbuoy";
    case snorkel(): return "snorkel";
    default: return "";
  }
}

str drillName(DrillType d) {
  switch(d) {
    case catchup(): return "catchup";
    case onesided(): return "onesided";
    case fingertip(): return "fingertip";
    case sixKick(): return "6kick";
    case sculling(): return "sculling";
  }
}

str targetToStr(Target t) {
  switch(t) {
    case target(m, s): {
      str sec = s < 10 ? "0<s>" : "<s>";
      return "target <m>:<sec>";
    }
    default: return "";
  }
}

// CLI interface
void main(list[str] args) {
  if (size(args) < 1) {
    println("{\"success\":false,\"error\":\"No command provided\"}");
    return;
  }
  
  str command = args[0];
  
  switch(command) {
    case "analyze": {
      if (size(args) < 2) {
        println("{\"success\":false,\"error\":\"Missing code argument\"}");
        return;
      }
      str code = args[1];
      println(analyzeToJSON(code));
    }
    case "generate": {
      if (size(args) < 5) {
        println("{\"success\":false,\"error\":\"Missing arguments for generate\"}");
        return;
      }
      str goal = args[1];
      int distance = toInt(args[2]);
      list[str] styles = split(",", args[3]);
      int duration = toInt(args[4]);
      println(generateToJSON(goal, distance, styles, duration));
    }
    default: {
      println("{\"success\":false,\"error\":\"Unknown command: <command>\"}");
    }
  }
}
