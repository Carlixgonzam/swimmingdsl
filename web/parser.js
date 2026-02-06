const TokenType = {
  SESSION: 'SESSION',
  SWIM: 'SWIM',
  KICK: 'KICK',
  DRILL: 'DRILL',
  PACE: 'PACE',
  REST: 'REST',
  TARGET: 'TARGET',
  WITH: 'WITH',
  GENERATE: 'GENERATE',
  GOAL: 'GOAL',
  DISTANCE: 'DISTANCE',
  STYLES: 'STYLES',
  DURATION: 'DURATION',
  MINUTES: 'MINUTES',
  WARMUP: 'WARMUP',
  MAIN: 'MAIN',
  COOLDOWN: 'COOLDOWN',
  FREESTYLE: 'FREESTYLE',
  BACKSTROKE: 'BACKSTROKE',
  BREASTSTROKE: 'BREASTSTROKE',
  BUTTERFLY: 'BUTTERFLY',
  EASY: 'EASY',
  MODERATE: 'MODERATE',
  HARD: 'HARD',
  FINS: 'FINS',
  PADDLES: 'PADDLES',
  BOARD: 'BOARD',
  PULLBUOY: 'PULLBUOY',
  SNORKEL: 'SNORKEL',
  CATCHUP: 'CATCHUP',
  ONESIDED: 'ONESIDED',
  FINGERTIP: 'FINGERTIP',
  SIXKICK: 'SIXKICK',
  SCULLING: 'SCULLING',
  ENDURANCE: 'ENDURANCE',
  SPEED: 'SPEED',
  TECHNIQUE: 'TECHNIQUE',
  RECOVERY: 'RECOVERY',
  LBRACE: 'LBRACE',
  RBRACE: 'RBRACE',
  LBRACKET: 'LBRACKET',
  RBRACKET: 'RBRACKET',
  COLON: 'COLON',
  COMMA: 'COMMA',
  X: 'X',
  M: 'M',
  S: 'S',
  INT: 'INT',
  ID: 'ID',
  EOF: 'EOF'
};

const KEYWORDS = {
  'session': TokenType.SESSION,
  'swim': TokenType.SWIM,
  'kick': TokenType.KICK,
  'drill': TokenType.DRILL,
  'pace': TokenType.PACE,
  'rest': TokenType.REST,
  'target': TokenType.TARGET,
  'with': TokenType.WITH,
  'generate': TokenType.GENERATE,
  'goal': TokenType.GOAL,
  'distance': TokenType.DISTANCE,
  'styles': TokenType.STYLES,
  'duration': TokenType.DURATION,
  'minutes': TokenType.MINUTES,
  'warmup': TokenType.WARMUP,
  'main': TokenType.MAIN,
  'cooldown': TokenType.COOLDOWN,
  'freestyle': TokenType.FREESTYLE,
  'backstroke': TokenType.BACKSTROKE,
  'breaststroke': TokenType.BREASTSTROKE,
  'butterfly': TokenType.BUTTERFLY,
  'easy': TokenType.EASY,
  'moderate': TokenType.MODERATE,
  'hard': TokenType.HARD,
  'fins': TokenType.FINS,
  'paddles': TokenType.PADDLES,
  'board': TokenType.BOARD,
  'pullbuoy': TokenType.PULLBUOY,
  'snorkel': TokenType.SNORKEL,
  'catchup': TokenType.CATCHUP,
  'onesided': TokenType.ONESIDED,
  'fingertip': TokenType.FINGERTIP,
  '6kick': TokenType.SIXKICK,
  'sculling': TokenType.SCULLING,
  'endurance': TokenType.ENDURANCE,
  'speed': TokenType.SPEED,
  'technique': TokenType.TECHNIQUE,
  'recovery': TokenType.RECOVERY,
  'x': TokenType.X,
  'm': TokenType.M,
  's': TokenType.S
};

class Token {
  constructor(type, value, line, column) {
    this.type = type;
    this.value = value;
    this.line = line;
    this.column = column;
  }
}

class Lexer {
  constructor(input) {
    this.input = input;
    this.pos = 0;
    this.line = 1;
    this.column = 1;
  }

  peek() {
    return this.input[this.pos] || '\0';
  }

  advance() {
    const ch = this.input[this.pos++];
    if (ch === '\n') {
      this.line++;
      this.column = 1;
    } else {
      this.column++;
    }
    return ch;
  }

  skipWhitespace() {
    while (/\s/.test(this.peek())) {
      this.advance();
    }
  }

  readNumber() {
    let num = '';
    while (/[0-9]/.test(this.peek())) {
      num += this.advance();
    }
    return parseInt(num, 10);
  }

  readIdentifier() {
    let id = '';
    if (this.peek() === '6') {
      id += this.advance();
    }
    while (/[a-zA-Z0-9_]/.test(this.peek())) {
      id += this.advance();
    }
    return id;
  }

  nextToken() {
    this.skipWhitespace();
    const line = this.line;
    const column = this.column;
    if (this.pos >= this.input.length) {
      return new Token(TokenType.EOF, null, line, column);
    }
    const ch = this.peek();
    if (ch === '{') { this.advance(); return new Token(TokenType.LBRACE, '{', line, column); }
    if (ch === '}') { this.advance(); return new Token(TokenType.RBRACE, '}', line, column); }
    if (ch === '[') { this.advance(); return new Token(TokenType.LBRACKET, '[', line, column); }
    if (ch === ']') { this.advance(); return new Token(TokenType.RBRACKET, ']', line, column); }
    if (ch === ':') { this.advance(); return new Token(TokenType.COLON, ':', line, column); }
    if (ch === ',') { this.advance(); return new Token(TokenType.COMMA, ',', line, column); }
    if (/[0-9]/.test(ch)) {
     
      if (ch === '6' && this.input.substring(this.pos, this.pos + 5) === '6kick') {
        this.readIdentifier();
        return new Token(TokenType.SIXKICK, '6kick', line, column);
      }
      const num = this.readNumber();
      return new Token(TokenType.INT, num, line, column);
    }

    
    if (/[a-zA-Z_]/.test(ch)) {
      const id = this.readIdentifier();
      const lower = id.toLowerCase();
      if (KEYWORDS[lower]) {
        return new Token(KEYWORDS[lower], lower, line, column);
      }
      return new Token(TokenType.ID, id, line, column);
    }

    throw new Error(`Unexpected character '${ch}' at line ${line}, column ${column}`);
  }

  tokenize() {
    const tokens = [];
    let token;
    do {
      token = this.nextToken();
      tokens.push(token);
    } while (token.type !== TokenType.EOF);
    return tokens;
  }
}


class Program {
  constructor(sessions, generator = null) {
    this.type = 'Program';
    this.sessions = sessions;
    this.generator = generator;
  }
}

class Session {
  constructor(name, blocks, sections = null) {
    this.type = 'Session';
    this.name = name;
    this.blocks = blocks;
    this.sections = sections;
  }
}

class SessionSection {
  constructor(sectionType, blocks) {
    this.type = 'SessionSection';
    this.sectionType = sectionType; 
    this.blocks = blocks;
  }
}

class Block {
  constructor(exercise, reps = 1, restSeconds = 0) {
    this.type = 'Block';
    this.exercise = exercise;
    this.reps = reps;
    this.restSeconds = restSeconds;
  }
}

class Exercise {
  constructor(exerciseType, meters, options = {}) {
    this.type = 'Exercise';
    this.exerciseType = exerciseType; 
    this.meters = meters;
    this.style = options.style || null;
    this.intensity = options.intensity || null;
    this.pace = options.pace || null;
    this.equipment = options.equipment || null;
    this.target = options.target || null;
    this.drillType = options.drillType || null;
  }
}

class Target {
  constructor(minutes, seconds) {
    this.type = 'Target';
    this.minutes = minutes;
    this.seconds = seconds;
  }
}

class GeneratorConfig {
  constructor(goal, distance, styles, durationMinutes) {
    this.type = 'GeneratorConfig';
    this.goal = goal;
    this.distance = distance;
    this.styles = styles;
    this.durationMinutes = durationMinutes;
  }
}


class Parser {
  constructor(tokens) {
    this.tokens = tokens;
    this.pos = 0;
  }

  current() {
    return this.tokens[this.pos];
  }

  peek(offset = 0) {
    return this.tokens[this.pos + offset];
  }

  advance() {
    return this.tokens[this.pos++];
  }

  expect(type) {
    const token = this.current();
    if (token.type !== type) {
      throw new Error(`Expected ${type} but got ${token.type} at line ${token.line}, column ${token.column}`);
    }
    return this.advance();
  }

  match(...types) {
    return types.includes(this.current().type);
  }

  parse() {
    if (this.match(TokenType.GENERATE)) {
      return new Program([], this.parseGenerator());
    }
    
    const sessions = [];
    while (!this.match(TokenType.EOF)) {
      sessions.push(this.parseSession());
    }
    return new Program(sessions);
  }

  parseGenerator() {
    this.expect(TokenType.GENERATE);
    this.expect(TokenType.SESSION);
    this.expect(TokenType.LBRACE);
    
    let goal = 'endurance';
    let distance = 2000;
    let styles = ['freestyle'];
    let duration = 60;

    while (!this.match(TokenType.RBRACE)) {
      if (this.match(TokenType.GOAL)) {
        this.advance();
        this.expect(TokenType.COLON);
        goal = this.parseGoal();
      } else if (this.match(TokenType.DISTANCE)) {
        this.advance();
        this.expect(TokenType.COLON);
        distance = this.expect(TokenType.INT).value;
      } else if (this.match(TokenType.STYLES)) {
        this.advance();
        this.expect(TokenType.COLON);
        styles = this.parseStyleList();
      } else if (this.match(TokenType.DURATION)) {
        this.advance();
        this.expect(TokenType.COLON);
        duration = this.expect(TokenType.INT).value;
        this.expect(TokenType.MINUTES);
      } else {
        throw new Error(`Unexpected token in generator: ${this.current().type}`);
      }
    }
    
    this.expect(TokenType.RBRACE);
    return new GeneratorConfig(goal, distance, styles, duration);
  }

  parseGoal() {
    if (this.match(TokenType.ENDURANCE)) { this.advance(); return 'endurance'; }
    if (this.match(TokenType.SPEED)) { this.advance(); return 'speed'; }
    if (this.match(TokenType.TECHNIQUE)) { this.advance(); return 'technique'; }
    if (this.match(TokenType.RECOVERY)) { this.advance(); return 'recovery'; }
    throw new Error(`Expected goal type at line ${this.current().line}`);
  }

  parseStyleList() {
    this.expect(TokenType.LBRACKET);
    const styles = [this.parseStyle()];
    while (this.match(TokenType.COMMA)) {
      this.advance();
      styles.push(this.parseStyle());
    }
    this.expect(TokenType.RBRACKET);
    return styles;
  }

  parseSession() {
    this.expect(TokenType.SESSION);
    const name = this.expect(TokenType.ID).value;
    this.expect(TokenType.LBRACE);

    
    if (this.match(TokenType.WARMUP, TokenType.MAIN, TokenType.COOLDOWN)) {
      const sections = [];
      while (!this.match(TokenType.RBRACE)) {
        sections.push(this.parseSection());
      }
      this.expect(TokenType.RBRACE);
      return new Session(name, [], sections);
    }

   
    const blocks = [];
    while (!this.match(TokenType.RBRACE)) {
      blocks.push(this.parseBlock());
    }
    this.expect(TokenType.RBRACE);
    return new Session(name, blocks);
  }

  parseSection() {
    let sectionType;
    if (this.match(TokenType.WARMUP)) { sectionType = 'warmup'; }
    else if (this.match(TokenType.MAIN)) { sectionType = 'main'; }
    else if (this.match(TokenType.COOLDOWN)) { sectionType = 'cooldown'; }
    else { throw new Error(`Expected section type at line ${this.current().line}`); }
    
    this.advance();
    this.expect(TokenType.LBRACE);
    
    const blocks = [];
    while (!this.match(TokenType.RBRACE)) {
      blocks.push(this.parseBlock());
    }
    this.expect(TokenType.RBRACE);
    
    return new SessionSection(sectionType, blocks);
  }

  parseBlock() {
   
    if (this.match(TokenType.INT) && this.peek(1)?.type === TokenType.X) {
      const reps = this.expect(TokenType.INT).value;
      this.expect(TokenType.X);
      const exercise = this.parseExercise();
      let restSeconds = 0;
      if (this.match(TokenType.REST)) {
        this.advance();
        restSeconds = this.expect(TokenType.INT).value;
        this.expect(TokenType.S);
      }
      return new Block(exercise, reps, restSeconds);
    }
    
   
    return new Block(this.parseExercise());
  }

  parseExercise() {
    if (this.match(TokenType.SWIM)) {
      return this.parseSwim();
    }
    if (this.match(TokenType.KICK)) {
      return this.parseKick();
    }
    if (this.match(TokenType.DRILL)) {
      return this.parseDrill();
    }
    throw new Error(`Expected exercise at line ${this.current().line}`);
  }

  parseSwim() {
    this.expect(TokenType.SWIM);
    const meters = this.expect(TokenType.INT).value;
    this.expect(TokenType.M);

    const options = {};

   
    if (this.isStyle()) {
      options.style = this.parseStyle();
    }

    
    if (this.isIntensity()) {
      options.intensity = this.parseIntensity();
    }

   
    if (this.match(TokenType.PACE)) {
      this.advance();
      options.pace = this.expect(TokenType.INT).value;
    }

   
    if (this.match(TokenType.WITH)) {
      options.equipment = this.parseEquipment();
    }

    
    if (this.match(TokenType.TARGET)) {
      options.target = this.parseTarget();
    }

    return new Exercise('swim', meters, options);
  }

  parseKick() {
    this.expect(TokenType.KICK);
    const meters = this.expect(TokenType.INT).value;
    this.expect(TokenType.M);

    const options = {};

    if (this.isIntensity()) {
      options.intensity = this.parseIntensity();
    }

    if (this.match(TokenType.WITH)) {
      options.equipment = this.parseEquipment();
    }

    return new Exercise('kick', meters, options);
  }

  parseDrill() {
    this.expect(TokenType.DRILL);
    const drillType = this.parseDrillType();
    const meters = this.expect(TokenType.INT).value;
    this.expect(TokenType.M);

    const options = { drillType };

    if (this.isIntensity()) {
      options.intensity = this.parseIntensity();
    }

    return new Exercise('drill', meters, options);
  }

  isStyle() {
    return this.match(TokenType.FREESTYLE, TokenType.BACKSTROKE, TokenType.BREASTSTROKE, TokenType.BUTTERFLY);
  }

  parseStyle() {
    if (this.match(TokenType.FREESTYLE)) { this.advance(); return 'freestyle'; }
    if (this.match(TokenType.BACKSTROKE)) { this.advance(); return 'backstroke'; }
    if (this.match(TokenType.BREASTSTROKE)) { this.advance(); return 'breaststroke'; }
    if (this.match(TokenType.BUTTERFLY)) { this.advance(); return 'butterfly'; }
    throw new Error(`Expected style at line ${this.current().line}`);
  }

  isIntensity() {
    return this.match(TokenType.EASY, TokenType.MODERATE, TokenType.HARD);
  }

  parseIntensity() {
    if (this.match(TokenType.EASY)) { this.advance(); return 'easy'; }
    if (this.match(TokenType.MODERATE)) { this.advance(); return 'moderate'; }
    if (this.match(TokenType.HARD)) { this.advance(); return 'hard'; }
    throw new Error(`Expected intensity at line ${this.current().line}`);
  }

  parseEquipment() {
    this.expect(TokenType.WITH);
    if (this.match(TokenType.FINS)) { this.advance(); return 'fins'; }
    if (this.match(TokenType.PADDLES)) { this.advance(); return 'paddles'; }
    if (this.match(TokenType.BOARD)) { this.advance(); return 'board'; }
    if (this.match(TokenType.PULLBUOY)) { this.advance(); return 'pullbuoy'; }
    if (this.match(TokenType.SNORKEL)) { this.advance(); return 'snorkel'; }
    throw new Error(`Expected equipment type at line ${this.current().line}`);
  }

  parseDrillType() {
    if (this.match(TokenType.CATCHUP)) { this.advance(); return 'catchup'; }
    if (this.match(TokenType.ONESIDED)) { this.advance(); return 'onesided'; }
    if (this.match(TokenType.FINGERTIP)) { this.advance(); return 'fingertip'; }
    if (this.match(TokenType.SIXKICK)) { this.advance(); return '6kick'; }
    if (this.match(TokenType.SCULLING)) { this.advance(); return 'sculling'; }
    throw new Error(`Expected drill type at line ${this.current().line}`);
  }

  parseTarget() {
    this.expect(TokenType.TARGET);
    const minutes = this.expect(TokenType.INT).value;
    this.expect(TokenType.COLON);
    const seconds = this.expect(TokenType.INT).value;
    return new Target(minutes, seconds);
  }
}


function parse(input) {
  const lexer = new Lexer(input);
  const tokens = lexer.tokenize();
  const parser = new Parser(tokens);
  return parser.parse();
}


if (typeof module !== 'undefined' && module.exports) {
  module.exports = { parse, Lexer, Parser, Program, Session, Block, Exercise, Target, GeneratorConfig };
}
