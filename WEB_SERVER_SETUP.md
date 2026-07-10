# Swimming DSL — Web Server Setup

This document describes how to run the web server that connects the frontend to the Rascal backend.

## Architecture

```
Browser (HTML/JS)
    down to  HTTP/JSON
Node.js Server (Express)
    down to  exec shell
Rascal (Java)
    down to  parse/analyze
Swimming DSL (.swim files)
```

The system has three main components:

1. **Web frontend** (`web/index-server.html`) — the user interface in the browser
2. **Node.js server** (`server/server.js`) — the REST API acting as a bridge
3. **Rascal backend** (`src/WebAPI.rsc`) — the analysis engine, written in Rascal

## Prerequisites

- **Node.js** 14+ and npm
- **Java** 11+
- **Rascal runtime** (`rascal-shell-stable.jar`, at the root of the project)

## Installation

### 1. Check for the Rascal runtime

The project needs `rascal-shell-stable.jar` at its root. It is already included; if it is missing for any reason, see the troubleshooting section below.

### 2. Install the Node.js dependencies

```bash
cd server
npm install
```

This installs:
- `express` — web framework
- `cors` — allows cross-origin requests

## Usage

### Start the server

From the `server/` directory:

```bash
npm start
```

You should see something like:

```
Swimming DSL Server running on http://localhost:3000

Endpoints:
  POST /api/analyze    - Analyze DSL (Rascal)
  POST /api/generate   - Generate session (Rascal)
  POST /api/translate  - NL to DSL (AI + Rascal loop)
  POST /api/coach      - Coaching feedback (AI)
  POST /api/optimize   - Optimize session (AI + Rascal)

GEMINI_API_KEY: set
```

### Open the web interface

1. The server is already serving the frontend.
2. Open your browser at **http://localhost:3000**.
3. You will see the Swimming DSL web interface.

## Using the interface

### Analyzing code

1. Write Swimming DSL code in the editor, or load an example.
2. Press "Analyze".
3. The frontend sends the code to the Node.js server.
4. The server runs Rascal against that code.
5. Results are shown in the Analysis panel.

### Generating a session

1. Go to the "Generator" tab.
2. Set the parameters (goal, distance, styles, duration).
3. Press "Generate Session".
4. Rascal generates the DSL code.
5. The code appears in the editor and is analyzed automatically.

## API endpoints

### GET /api/health

Server health check.

**Response:**
```json
{
  "status": "ok",
  "message": "Swimming DSL API is running"
}
```

### POST /api/analyze

Analyzes Swimming DSL code.

**Request:**
```json
{
  "code": "session morning {\n  swim 400 m freestyle easy pace 120\n}"
}
```

**Response:**
```json
{
  "success": true,
  "sessionCount": 1,
  "sessionNames": ["morning"],
  "totalDistance": 400,
  "distanceKm": 0.4,
  "styles": { "freestyle": 1 },
  "intensities": { "easy": 1 },
  "equipment": {},
  "drills": {},
  "rest": {
    "periods": 0,
    "totalSeconds": 0,
    "totalFormatted": "0:00",
    "average": 0
  },
  "time": {
    "swimSeconds": 480,
    "restSeconds": 0,
    "totalSeconds": 480,
    "swimFormatted": "8:00",
    "restFormatted": "0:00",
    "totalFormatted": "8:00"
  }
}
```

### POST /api/generate

Generates a session from a set of parameters.

**Request:**
```json
{
  "goal": "endurance",
  "distance": 3000,
  "styles": ["freestyle", "backstroke"],
  "duration": 60
}
```

**Response:**
```json
{
  "success": true,
  "code": "session generated_endurance {\n  warmup {\n    swim 450 m easy pace 120\n  }\n  ...\n}",
  "goal": "endurance",
  "distance": 3000
}
```

## Troubleshooting

### Error: "Cannot find module 'express'"

Run `npm install` inside `server/`.

### Error: "Rascal execution failed"

Check that:
1. `rascal-shell-stable.jar` exists at the root of the project.
2. Java is installed: `java -version`.
3. The `WebAPI.rsc` module is present in `src/`.

### Error: "No JSON output from Rascal"

This usually means Rascal hit a syntax error. Check the server logs (in the terminal where you ran `npm start`) to see the full Rascal output.

### The server does not start on port 3000

If port 3000 is already in use, use a different one:

```bash
PORT=8080 npm start
```

Then open `http://localhost:8080`.

## Development

### Auto-reload during development

```bash
npm run dev
```

This uses `nodemon` to restart the server automatically whenever the code changes.

### Changing the port

Edit `server/server.js`:

```javascript
const PORT = process.env.PORT || 3000;  // change 3000 to another port
```

Or use an environment variable:

```bash
PORT=8080 npm start
```

### Adding new endpoints

Edit `server/server.js` and add additional routes:

```javascript
app.post('/api/my-new-endpoint', async (req, res) => {
  // your code here
});
```

## Testing with curl

```bash
# Health check
curl http://localhost:3000/api/health

# Analyze code
curl -X POST http://localhost:3000/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"session test {\n  swim 100 m easy\n}"}'

# Generate a session
curl -X POST http://localhost:3000/api/generate \
  -H "Content-Type: application/json" \
  -d '{"goal":"endurance","distance":2000,"styles":["freestyle"],"duration":45}'
```

## File structure

```
swimmingdsl/
  src/
    WebAPI.rsc            Rascal module with analysis and JSON functions
    AST.rsc                AST definitions
    SwimSyntax.rsc         DSL grammar
    Semantics.rsc           semantic analysis
    ...
  server/
    server.js              Node.js Express server
    package.json            npm dependencies
    node_modules/           generated by npm install
  web/
    index-server.html      frontend served by the Node.js server
  rascal-shell-stable.jar  Rascal MPL runtime
  WEB_SERVER_SETUP.md      this file
```
