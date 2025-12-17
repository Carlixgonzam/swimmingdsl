module AST

import ParseTree;
import String;
data Program = program(list[Session] sessions);

data Session = session(str name, list[Block] blocks);

data Block
  = exercise(Exercise ex)
  | interval(int reps, Exercise ex, int restSeconds)
  | interval(int reps, Exercise ex);

data Exercise
  = swim(int meters, list[Modifier] modifiers)
  | kick(int meters, list[Modifier] modifiers);

data Modifier
  = style(Style s)
  | intensity(Intensity i)
  | pace(int seconds);

data Style
  = freestyle()
  | backstroke()
  | breaststroke()
  | butterfly();

data Intensity
  = easy()
  | moderate()
  | hard();
