module Semantics

import AST;
import List;
int distance(Exercise ex) {
  switch(ex) {
    case swim(m, _, _, _, _, _): return m;
    case swim(m, _, _, _, _): return m;
    case swim(m, _, _, _): return m;
    case swim(m, _, _): return m;
    case swim(m, _): return m;
    case swim(m): return m;
    case kick(m, _, _): return m;
    case kick(m, _): return m;
    case kick(m): return m;
    case drill(_, m, _): return m;
    case drill(_, m): return m;
  }
}
int blockDistance(Block b) {
  switch(b) {
    case exercise(ex): return distance(ex);
    case interval(r, ex, _): return r * distance(ex);
    case interval(r, ex): return r * distance(ex);
  }
}
int getPace(Exercise ex) {
  switch(ex) {
    case swim(_, _, _, p, _, _): return p;
    case swim(_, _, _, p, _): return p;
    case swim(_, _, _, p): return p;
    case swim(_, _, p): return p;
    case swim(_, p): return p;
    default: return -1;
  }
}
int estimatedTime(Exercise ex) {
  int dist = distance(ex);
  int p = getPace(ex);
  
  if (p > 0) {
    return (dist * p) / 100;
  }
  return 0;
}
int blockEstimatedTime(Block b) {
  switch(b) {
    case exercise(ex): return estimatedTime(ex);
    case interval(r, ex, restSec): {
      int swimTime = r * estimatedTime(ex);
      int totalRest = (r - 1) * restSec;
      return swimTime + totalRest;
    }
    case interval(r, ex): return r * estimatedTime(ex);
  }
}
int blockRestTime(Block b) {
  switch(b) {
    case interval(r, _, restSec): return (r - 1) * restSec;
    default: return 0;
  }
}
str formatTime(int seconds) {
  int mins = seconds / 60;
  int secs = seconds % 60;
  str secStr = secs < 10 ? "0<secs>" : "<secs>";
  return "<mins>:<secStr>";
}
Session generateSession(GeneratorConfig config) {
  switch(config.goal) {
    case endurance(): return generateEnduranceSession(config);
    case speed(): return generateSpeedSession(config);
    case technique(): return generateTechniqueSession(config);
    case recovery(): return generateRecoverySession(config);
  }
}

Session generateEnduranceSession(GeneratorConfig config) {
  int totalDist = config.distance;
  list[Style] styles = config.styles;
  Style mainStyle = size(styles) > 0 ? styles[0] : freestyle();
  
  // Warmup15% of total distance
  int warmupDist = (totalDist * 15) / 100;
  list[Block] warmupBlocks = [
    exercise(swim(warmupDist, noStyle(), easy(), 120, noEquipment(), noTarget()))
  ];
  
  // Main 70% of total distance - long intervals
  int mainDist = (totalDist * 70) / 100;
  int setDist = mainDist / 4;
  list[Block] mainBlocks = [
    interval(4, swim(setDist, mainStyle, moderate(), 100, noEquipment(), noTarget()), 30)
  ];
  
  // Cooldown 15% of total distance
  int cooldownDist = (totalDist * 15) / 100;
  list[Block] cooldownBlocks = [
    exercise(swim(cooldownDist, noStyle(), easy(), 130, noEquipment(), noTarget()))
  ];
  
  return structuredSession("generated_endurance", [
    warmup(warmupBlocks),
    mainSection(mainBlocks),
    cooldown(cooldownBlocks)
  ]);
}

Session generateSpeedSession(GeneratorConfig config) {
  int totalDist = config.distance;
  list[Style] styles = config.styles;
  Style mainStyle = size(styles) > 0 ? styles[0] : freestyle();
  
  // Warmup 20% of total distance
  int warmupDist = (totalDist * 20) / 100;
  list[Block] warmupBlocks = [
    exercise(swim(warmupDist, noStyle(), easy(), 120, noEquipment(), noTarget()))
  ];
  
  // Main 60% of total distance - short fast intervals
  int mainDist = (totalDist * 60) / 100;
  int reps = mainDist / 50;
  list[Block] mainBlocks = [
    interval(reps, swim(50, mainStyle, hard(), 60, noEquipment(), noTarget()), 45)
  ];
  
  // Cooldown 20% of total distance
  int cooldownDist = (totalDist * 20) / 100;
  list[Block] cooldownBlocks = [
    exercise(swim(cooldownDist, noStyle(), easy(), 130, noEquipment(), noTarget()))
  ];
  
  return structuredSession("generated_speed", [
    warmup(warmupBlocks),
    mainSection(mainBlocks),
    cooldown(cooldownBlocks)
  ]);
}

Session generateTechniqueSession(GeneratorConfig config) {
  int totalDist = config.distance;
  
  // Warmup 25% of total distance
  int warmupDist = (totalDist * 25) / 100;
  list[Block] warmupBlocks = [
    exercise(swim(warmupDist, noStyle(), easy(), 120, noEquipment(), noTarget()))
  ];
  
  // Main 50% of total distance - drills
  int mainDist = (totalDist * 50) / 100;
  int drillDist = mainDist / 3;
  list[Block] mainBlocks = [
    interval(3, drill(catchup(), drillDist / 3, easy()), 20),
    interval(3, drill(fingertip(), drillDist / 3, easy()), 20),
    interval(3, drill(sculling(), drillDist / 3, easy()), 20)
  ];
  
  // Cooldown 25% of total distance
  int cooldownDist = (totalDist * 25) / 100;
  list[Block] cooldownBlocks = [
    exercise(swim(cooldownDist, noStyle(), easy(), 130, noEquipment(), noTarget()))
  ];
  
  return structuredSession("generated_technique", [
    warmup(warmupBlocks),
    mainSection(mainBlocks),
    cooldown(cooldownBlocks)
  ]);
}

Session generateRecoverySession(GeneratorConfig config) {
  int totalDist = config.distance;
  list[Block] mainBlocks = [
    exercise(swim(totalDist, noStyle(), easy(), 140, noEquipment(), noTarget()))
  ];
  
  return session("generated_recovery", mainBlocks);
}
