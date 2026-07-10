# Swimming DSL — Quick Start Guide

## Setup

### Step 1: Check for the Rascal runtime

`rascal-shell-stable.jar` should already be present at the root of the project. If it is missing, download it with:

```bash
curl -L -o rascal-shell-stable.jar https://update.rascal-mpl.org/console/rascal-shell-stable.jar
```

### Step 2: Check your Java version

```bash
java -version
```

You should see Java 11 or newer.

### Step 3: Install the Node.js dependencies

```bash
cd server
npm install
```

### Step 4: Start the server

```bash
npm start
```

### Step 5: Open the app in your browser

Open http://localhost:3000

## What this sets up

### 1. WebAPI.rsc — Rascal backend

- Rascal module exposing functions for analysis and generation
- Exports results as JSON
- Invoked from the command line

**Location**: `src/WebAPI.rsc`

**Main functions**:
- `analyzeToJSON(str code)` — analyzes code and returns JSON
- `generateToJSON(str goal, int distance, list[str] styles, int duration)` — generates sessions

### 2. Node.js server

- Acts as the bridge between the frontend and Rascal
- Receives HTTP requests from the frontend
- Runs Rascal via `java -jar`
- Returns JSON to the browser

**Location**: `server/server.js`

**Endpoints**:
- `GET /api/health` — health check
- `POST /api/analyze` — analyzes DSL code
- `POST /api/generate` — generates sessions

### 3. Frontend

- HTML/CSS/JS interface that talks to the server
- Code editor with syntax highlighting
- Analysis visualization
- Form for generating sessions

**Location**: `web/index-server.html`

## Data flow

```
User writes DSL code
    down to
Frontend (JavaScript)
    down to  HTTP POST /api/analyze
Node.js server
    down to  exec("java -jar rascal-shell-stable.jar ...")
Rascal runs WebAPI.rsc
    down to  parses and analyzes
Rascal returns JSON
    down to  stdout
Node.js captures the JSON
    down to  HTTP response
Frontend displays the results
```

### Testing the Node.js server

With the server running:

```bash
# Health check
curl http://localhost:3000/api/health

# Analyze code
curl -X POST http://localhost:3000/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"session test { swim 100 m easy }"}'
```

### Testing the frontend

1. Open http://localhost:3000
2. Write code in the editor
3. Press "Analyze"
4. You should see the results

## Troubleshooting

### "Cannot find module 'express'"

```bash
cd server
npm install
```

### "java: command not found"

Install Java 11+:
- macOS: `brew install openjdk@11`
- Ubuntu: `sudo apt install openjdk-11-jdk`
- Windows: download from https://adoptium.net/

### "rascal-shell-stable.jar not found"

```bash
curl -L -o rascal-shell-stable.jar https://update.rascal-mpl.org/console/rascal-shell-stable.jar
```

Run this from the root of the project.

### "Error: EADDRINUSE" (port already in use)

Use a different port:

```bash
PORT=8080 npm start
```

### Rascal reports a syntax error

Check that `WebAPI.rsc` is in `src/` and that all its imports resolve correctly.

## Files involved

```
swimmingdsl/
  src/
    WebAPI.rsc               Rascal API with JSON exports
  server/
    server.js                Node.js Express server
    package.json              npm dependencies
  web/
    index-server.html        frontend served by the Node.js server
  WEB_SERVER_SETUP.md        full documentation
  QUICKSTART.md              this guide
  rascal-shell-stable.jar    Rascal runtime
```

## Further reading

- **WEB_SERVER_SETUP.md** — full server documentation
- **README.md** — DSL documentation
- **ARCHITECTURE.md** — project architecture
