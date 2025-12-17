module Semantics

import AST;
int distance(Exercise ex) {
  switch(ex) {
    case swim(m, _): return m;
    case kick(m, _): return m;
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
    case swim(_, mods): {
      for (m <- mods) {
        if (pace(p) := m) return p;
      }
    }
    case kick(_, mods): {
      for (m <- mods) {
        if (pace(p) := m) return p;
      }
    }
  }
  return 0;
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


str getStyle(Exercise ex) {
  switch(ex) {
    case swim(_, mods): {
      for (m <- mods) {
        if (style(freestyle()) := m) return "freestyle";
        if (style(backstroke()) := m) return "backstroke";
        if (style(breaststroke()) := m) return "breaststroke";
        if (style(butterfly()) := m) return "butterfly";
      }
    }
  }
  return "any style";
}


str getIntensity(Exercise ex) {
  switch(ex) {
    case swim(_, mods): {
      for (m <- mods) {
        if (intensity(easy()) := m) return "easy";
        if (intensity(moderate()) := m) return "moderate";
        if (intensity(hard()) := m) return "hard";
      }
    }
    case kick(_, mods): {
      for (m <- mods) {
        if (intensity(easy()) := m) return "easy";
        if (intensity(moderate()) := m) return "moderate";
        if (intensity(hard()) := m) return "hard";
      }
    }
  }
  return "normal";
}

str formatTime(int seconds) {
  int mins = seconds / 60;
  int secs = seconds % 60;
  return "<mins>:<secs < 10 ? "0" : ""><secs>";
}
