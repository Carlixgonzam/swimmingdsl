# Swimming DSL - Web Server Setup

este documento describe como ejecutar el servidor web que conecta el frontend con el backend de Rascal

## Arquitectura

```
Browser (HTML/JS)
    ↓ HTTP/JSON
Node.js Server (Express)
    ↓ exec shell
Rascal (Java)
    ↓ parse/analyze
Swimming DSL (.swim files)
```

el sistema tiene 3 componentes principales:

1. **frontend Web** (`web/index-server.html`) - Interfaz de usuario en el navegador
2. **node.js Server** (`server/server.js`) - API REST que actúa como puente
3. **rascal Backend** (`src/WebAPI.rsc`) - Motor de análisis escrito en Rascal

## Requisitos Previos

- **Node.js** 14+ y npm
- **Java** 11+
- **Rascal JAR** (debe existir como `rascal.jar` en el directorio principal)

## Instalación

### 1. verificar que eestá rascal.jar

El proyecto necesita `rascal.jar` en el directorio raíz. Si no lo tienes, descárgalo o usa el que ya tenías.

### 2. Instalar dependencias de Node.js

```bash
cd server
npm install
```

Esto instalará:
- `express` - framwork web
- `cors` - pard permitir peticiones

## Uso

### iniciar el servidor

Desde el directorio `server/`:

```bash
npm start
```


se debería ver:

```
🏊‍♀️ Swimming DSL Server running on http://localhost:3000
📂 Project path: /Users/carlagonzalez/Desktop/swimmingdsl
☕ Rascal JAR: /Users/carlagonzalez/Desktop/swimmingdsl/rascal.jar

API Endpoints:
  GET  /api/health - Health check
  POST /api/analyze - Analyze DSL code
  POST /api/generate - Generate session
```

### abrir la interfaz web

1. el servidor ya está sirviendo el frontend
2. abror tu navegador en: **http://localhost:3000**
3. verás la interfaz web del Swimming DSL

## Uso de la Interfaz

### analizar Código

1. Escribe código Swimming DSL en el editor o carga un ejemplo
2. presiona "Analizar"
3. el frontend envía el código al servidor Node.js
4. el servidor ejecuta Rascal con tu código
5. los resultados se muestran en el panel de Análisis

### generar Sesión

1. Ve a la pestaña "Generador"
2. Configura los parámetros (objetivo, distancia, estilos, duración)
3. Presiona "Generar Sesión"
4. Rascal genera el código DSL
5. El código aparece en el editor y se analiza automáticamente

## API Endpoints

### GET /api/health

Health check del servidor.

**Response:**
```json
{
  "status": "ok",
  "message": "Swimming DSL API is running"
}
```

### POST /api/analyze

Analiza código Swimming DSL.

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

Genera una sesión basada en parámetros.

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

Ejecuta `npm install` en el directorio `server/`.

### Error: "Rascal execution failed"

Verifica que:
1. `rascal.jar` existe en el directorio raíz
2. Java está instalado: `java -version`
3. El módulo `WebAPI.rsc` está en `src/`

### Error: "No JSON output from Rascal"

Esto puede ocurrir si Rascal tiene un error de sintaxis. Revisa los logs del servidor (en la terminal donde ejecutaste `npm start`) para ver el output completo de Rascal.

### El servidor no arranca en el puerto 3000

Si el puerto 3000 está ocupado, puedes usar otro:

```bash
PORT=8080 npm start
```

Luego abre `http://localhost:8080`

## Desarrollo

### Modo desarrollo con auto-reload

```bash
npm run dev
```

Esto usa `nodemon` para reiniciar el servidor automáticamente cuando cambies código.

### Modificar el puerto

Edita `server/server.js`:

```javascript
const PORT = process.env.PORT || 3000;  // cambia 3000 por otro puerto
```

O usa una variable de entorno:

```bash
PORT=8080 npm start
```

### Agregar más endpoints

Edita `server/server.js` y agrega rutas adicionales:

```javascript
app.post('/api/mi-nuevo-endpoint', async (req, res) => {
  // Tu código aquí
});
```

## Testing con curl

Puedes probar los endpoints con curl:

```bash
# Health check
curl http://localhost:3000/api/health

# Analizar código
curl -X POST http://localhost:3000/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"session test {\n  swim 100 m easy\n}"}'

# Generar sesión
curl -X POST http://localhost:3000/api/generate \
  -H "Content-Type: application/json" \
  -d '{"goal":"endurance","distance":2000,"styles":["freestyle"],"duration":45}'
```

## Estructura de Archivos

```
swimmingdsl/
├── src/
│   ├── WebAPI.rsc          # Módulo Rascal con funciones de análisis y JSON
│   ├── AST.rsc             # Definiciones del AST
│   ├── SwimSyntax.rsc      # Gramática del DSL
│   ├── Semantics.rsc       # Análisis semántico
│   └── ...
├── server/
│   ├── server.js           # Servidor Node.js Express
│   ├── package.json        # Dependencias npm
│   └── node_modules/       # (generado por npm install)
├── web/
│   ├── index.html          # Frontend standalone (sin servidor)
│   ├── index-server.html   # Frontend que usa el servidor
│   ├── parser.js           # Parser JavaScript standalone
│   └── analyzer.js         # Analyzer JavaScript standalone
├── rascal.jar              # Rascal MPL runtime
└── WEB_SERVER_SETUP.md     # Este archivo
```




