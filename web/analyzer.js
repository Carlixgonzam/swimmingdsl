class Analyzer {
  constructor(ast) {
    this.ast = ast;
  }
  getAllBlocks(session) { //aca obtengo todos los bloqies, tanto las simples como las estructuradas
    if (session.sections && session.sections.length > 0) {
      return session.sections.flatMap(s => s.blocks); //transforma cada sesion y hace merge en otro array con el resultado
    }
    return session.blocks;
  }
  exerciseDistance(exercise) {
    return exercise.meters;
  }
  blockDistance(block) {
    return block.reps * this.exerciseDistance(block.exercise);
  }
  sessionDistance(session) {
    return this.getAllBlocks(session).reduce((sum, block) => sum + this.blockDistance(block), 0);
  }
  totalDistance() {
    return this.ast.sessions.reduce((sum, session) => sum + this.sessionDistance(session), 0);
  }
  getPace(exercise) {
    return exercise.pace;
  }
  //estimo el timepo, TOCA REVISAR ESTA
  estimatedTime(exercise) {
    const dist = this.exerciseDistance(exercise);
    const pace = this.getPace(exercise);
    if (pace && pace > 0) {
      return Math.floor((dist * pace) / 100);
    }
    return 0;
  }
  blockEstimatedTime(block) {
    const swimTime = block.reps * this.estimatedTime(block.exercise);
    const totalRest = block.reps > 1 ? (block.reps - 1) * block.restSeconds : 0;
    return swimTime + totalRest;
  }
  blockRestTime(block) {
    if (block.reps > 1 && block.restSeconds > 0) {
      return (block.reps - 1) * block.restSeconds;
    }
    return 0;
  }
  //formatoen MM:SS
  formatTime(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }
//estilo contador de los estilos por entreno
  countStyles() {
    const counts = {};
    for (const session of this.ast.sessions) {
      for (const block of this.getAllBlocks(session)) {
        const style = block.exercise.style;
        if (style) {
          counts[style] = (counts[style] || 0) + 1;
        }
      }
    }
    return counts;
  }

  //contador de las intensidades
  countIntensities() {
    const counts = {};
    for (const session of this.ast.sessions) {
      for (const block of this.getAllBlocks(session)) {
        const intensity = block.exercise.intensity;
        if (intensity) {
          counts[intensity] = (counts[intensity] || 0) + 1;
        }
      }
    }
    return counts;
  }

  //contador del material usado
  countEquipment() {
    const counts = {};
    for (const session of this.ast.sessions) {
      for (const block of this.getAllBlocks(session)) {
        const equipment = block.exercise.equipment;
        if (equipment) {
          counts[equipment] = (counts[equipment] || 0) + 1;
        }
      }
    }
    return counts;
  }
  //contador de las tecnicas en la sesion de entreno
  countDrills() {
    const counts = {};
    for (const session of this.ast.sessions) {
      for (const block of this.getAllBlocks(session)) {
        if (block.exercise.exerciseType === 'drill' && block.exercise.drillType) {
          const drillType = block.exercise.drillType;
          counts[drillType] = (counts[drillType] || 0) + 1;
        }
      }
    }
    return counts;
  }
  //aca analiza los periosos de descanso
  analyzeRest() {
    let totalRest = 0;
    let restCount = 0;
    for (const session of this.ast.sessions) {
      for (const block of this.getAllBlocks(session)) {
        if (block.restSeconds > 0 && block.reps > 1) {
          totalRest += (block.reps - 1) * block.restSeconds;
          restCount += block.reps - 1;
        }
      }
    }
    return {
      totalRestSeconds: totalRest,
      restPeriods: restCount,
      averageRest: restCount > 0 ? Math.round(totalRest / restCount) : 0
    };
  }
  estimateTotalTime() {
    let swimTime = 0;
    let restTime = 0;
    for (const session of this.ast.sessions) {
      for (const block of this.getAllBlocks(session)) {
        swimTime += block.reps * this.estimatedTime(block.exercise);
        restTime += this.blockRestTime(block);
      }
    }
    
    return {
      swimSeconds: swimTime,
      restSeconds: restTime,
      totalSeconds: swimTime + restTime
    };
  }
  //todo el reporte
  analyze() {
    const sessionNames = this.ast.sessions.map(s => s.name);
    const totalDist = this.totalDistance();
    const styles = this.countStyles();
    const intensities = this.countIntensities();
    const equipment = this.countEquipment();
    const drills = this.countDrills();
    const rest = this.analyzeRest();
    const time = this.estimateTotalTime();
    return {
      sessionCount: this.ast.sessions.length,
      sessionNames,
      totalDistance: totalDist,
      distanceKm: (totalDist / 1000).toFixed(2),
      styles,
      intensities,
      equipment,
      drills,
      rest: {
        periods: rest.restPeriods,
        totalSeconds: rest.totalRestSeconds,
        totalFormatted: this.formatTime(rest.totalRestSeconds),
        average: rest.averageRest
      },
      time: {
        swimFormatted: this.formatTime(time.swimSeconds),
        restFormatted: this.formatTime(time.restSeconds),
        totalFormatted: this.formatTime(time.totalSeconds)
      },
      sessions: this.ast.sessions.map(session => this.analyzeSession(session))
    };
  }

  //analiza solo una sesion
  analyzeSession(session) {
    const blocks = this.getAllBlocks(session);
    const distance = this.sessionDistance(session);
    let swimTime = 0;
    let restTime = 0;
    for (const block of blocks) {
      swimTime += block.reps * this.estimatedTime(block.exercise);
      restTime += this.blockRestTime(block);
    }
    return {
      name: session.name,
      isStructured: session.sections && session.sections.length > 0,
      sections: session.sections ? session.sections.map(s => ({
        type: s.sectionType,
        blockCount: s.blocks.length,
        distance: s.blocks.reduce((sum, b) => sum + this.blockDistance(b), 0)
      })) : null,
      blockCount: blocks.length,
      distance,
      distanceKm: (distance / 1000).toFixed(2),
      estimatedTime: this.formatTime(swimTime + restTime)
    };
  }
}
//generador de entrenamientos
class SessionGenerator {
  generate(config) {
    switch (config.goal) {
      case 'endurance': return this.generateEndurance(config);
      case 'speed': return this.generateSpeed(config);
      case 'technique': return this.generateTechnique(config);
      case 'recovery': return this.generateRecovery(config);
      default: return this.generateEndurance(config);
    }
  }
  createExercise(type, meters, options = {}) {
    return {
      type: 'Exercise',
      exerciseType: type,
      meters,
      style: options.style || null,
      intensity: options.intensity || null,
      pace: options.pace || null,
      equipment: options.equipment || null,
      target: options.target || null,
      drillType: options.drillType || null
    };
  }
  createBlock(exercise, reps = 1, restSeconds = 0) {
    return {
      type: 'Block',
      exercise,
      reps,
      restSeconds
    };
  }
  createSection(sectionType, blocks) {
    return {
      type: 'SessionSection',
      sectionType,
      blocks
    };
  }
  createSession(name, sections) {
    return {
      type: 'Session',
      name,
      blocks: [],
      sections
    };
  }
  //aca geenra las intensidades
  generateEndurance(config) {
    const totalDist = config.distance;
    const mainStyle = config.styles[0] || 'freestyle';
    const warmupDist = Math.floor((totalDist * 15) / 100);
    const mainDist = Math.floor((totalDist * 70) / 100);
    const cooldownDist = Math.floor((totalDist * 15) / 100); //de acuerdo al porcentaje definido
    const setDist = Math.floor(mainDist / 4);
    const warmup = this.createSection('warmup', [
      this.createBlock(this.createExercise('swim', warmupDist, { intensity: 'easy', pace: 120 }))
    ]);
    const main = this.createSection('main', [
      this.createBlock(
        this.createExercise('swim', setDist, { style: mainStyle, intensity: 'moderate', pace: 100 }),
        4, 30
      )
    ]);
    const cooldown = this.createSection('cooldown', [
      this.createBlock(this.createExercise('swim', cooldownDist, { intensity: 'easy', pace: 130 }))
    ]);
    return this.createSession('generated_endurance', [warmup, main, cooldown]);
  }

  generateSpeed(config) {
    const totalDist = config.distance;
    const mainStyle = config.styles[0] || 'freestyle'; //asumo este por default
    const warmupDist = Math.floor((totalDist * 20) / 100);
    const mainDist = Math.floor((totalDist * 60) / 100);
    const cooldownDist = Math.floor((totalDist * 20) / 100);
    const reps = Math.floor(mainDist / 50);
    const warmup = this.createSection('warmup', [
      this.createBlock(this.createExercise('swim', warmupDist, { intensity: 'easy', pace: 120 }))
    ]);
    const main = this.createSection('main', [
      this.createBlock(
        this.createExercise('swim', 50, { style: mainStyle, intensity: 'hard', pace: 60 }),
        reps, 45
      )
    ]);
    const cooldown = this.createSection('cooldown', [
      this.createBlock(this.createExercise('swim', cooldownDist, { intensity: 'easy', pace: 130 }))
    ]);
    return this.createSession('generated_speed', [warmup, main, cooldown]);
  }

  generateTechnique(config) {
    const totalDist = config.distance;
    const warmupDist = Math.floor((totalDist * 25) / 100);
    const mainDist = Math.floor((totalDist * 50) / 100);
    const cooldownDist = Math.floor((totalDist * 25) / 100);
    const drillDist = Math.floor(mainDist / 9);
    const warmup = this.createSection('warmup', [
      this.createBlock(this.createExercise('swim', warmupDist, { intensity: 'easy', pace: 120 }))
    ]);
    const main = this.createSection('main', [
      this.createBlock(
        this.createExercise('drill', drillDist, { drillType: 'catchup', intensity: 'easy' }),
        3, 20
      ),
      this.createBlock(
        this.createExercise('drill', drillDist, { drillType: 'fingertip', intensity: 'easy' }),
        3, 20
      ),
      this.createBlock(
        this.createExercise('drill', drillDist, { drillType: 'sculling', intensity: 'easy' }),
        3, 20
      )
    ]);
    const cooldown = this.createSection('cooldown', [
      this.createBlock(this.createExercise('swim', cooldownDist, { intensity: 'easy', pace: 130 }))
    ]);
    return this.createSession('generated_technique', [warmup, main, cooldown]);
  }

  generateRecovery(config) {
    const totalDist = config.distance;
    return {
      type: 'Session',
      name: 'generated_recovery',
      blocks: [
        this.createBlock(this.createExercise('swim', totalDist, { intensity: 'easy', pace: 140 }))
      ],
      sections: null
    };
  }
}

//conversion generada de la sesion a DSL
function sessionToDSL(session) {
  let output = `session ${session.name} {\n`;

  if (session.sections && session.sections.length > 0) {
    for (const section of session.sections) {
      output += `  ${section.sectionType} {\n`;
      for (const block of section.blocks) {
        output += `    ${blockToDSL(block)}\n`;
      }
      output += `  }\n`;
    }
  } else {
    for (const block of session.blocks) {
      output += `  ${blockToDSL(block)}\n`;
    }
  }

  output += `}`;
  return output;
}

function blockToDSL(block) {
  let line = '';
  
  if (block.reps > 1) {
    line += `${block.reps} x `;
  }
  
  line += exerciseToDSL(block.exercise);
  
  if (block.restSeconds > 0 && block.reps > 1) {
    line += ` rest ${block.restSeconds} s`;
  }
  
  return line;
}

function exerciseToDSL(ex) {
  let parts = [];
  
  if (ex.exerciseType === 'drill') {
    parts.push('drill');
    if (ex.drillType) parts.push(ex.drillType);
    parts.push(`${ex.meters} m`);
  } else {
    parts.push(ex.exerciseType);
    parts.push(`${ex.meters} m`);
  }
  
  if (ex.style) parts.push(ex.style);
  if (ex.intensity) parts.push(ex.intensity);
  if (ex.pace) parts.push(`pace ${ex.pace}`);
  if (ex.equipment) parts.push(`with ${ex.equipment}`);
  if (ex.target) parts.push(`target ${ex.target.minutes}:${ex.target.seconds.toString().padStart(2, '0')}`);
  
  return parts.join(' ');
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = { Analyzer, SessionGenerator, sessionToDSL };
}
