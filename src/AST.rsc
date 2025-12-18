module AST

import ParseTree;
import String;
data Program 
  = program(list[Session] sessions)
  | generator(GeneratorConfig config);

data Session 
  = session(str name, list[Block] blocks)
  | structuredSession(str name, list[SessionSection] sections);

data SessionSection
  = warmup(list[Block] blocks)
  | mainSection(list[Block] blocks)
  | cooldown(list[Block] blocks);

data Block
  = exercise(Exercise ex)
  | interval(int reps, Exercise ex, int restSeconds)
  | interval(int reps, Exercise ex);

data Exercise
  = swim(int meters, Style style, Intensity intensity, int pace, Equipment equipment, Target target)
  | swim(int meters, Style style, Intensity intensity, int pace, Equipment equipment)
  | swim(int meters, Style style, Intensity intensity, int pace, Target target)
  | swim(int meters, Style style, Intensity intensity, int pace)
  | swim(int meters, Style style, Intensity intensity, Equipment equipment)
  | swim(int meters, Style style, Intensity intensity)
  | swim(int meters, Style style, int pace)
  | swim(int meters, Intensity intensity, int pace)
  | swim(int meters, int pace)
  | swim(int meters)
  | kick(int meters, Intensity intensity, Equipment equipment)
  | kick(int meters, Intensity intensity)
  | kick(int meters, Equipment equipment)
  | kick(int meters)
  | drill(DrillType drillType, int meters, Intensity intensity)
  | drill(DrillType drillType, int meters);

data Style
  = freestyle()
  | backstroke()
  | breaststroke()
  | butterfly()
  | noStyle();

data Intensity
  = easy()
  | moderate()
  | hard()
  | noIntensity();

data Equipment
  = fins()
  | paddles()
  | board()
  | pullbuoy()
  | snorkel()
  | noEquipment();

data DrillType
  = catchup()
  | onesided()
  | fingertip()
  | sixKick()
  | sculling();

data Target
  = target(int minutes, int seconds)
  | noTarget();

data GeneratorConfig = generatorConfig(
  Goal goal,
  int distance,
  list[Style] styles,
  int durationMinutes
);

data Goal
  = endurance()
  | speed()
  | technique()
  | recovery();
