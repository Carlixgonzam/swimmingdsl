# Swimming DSL - Quick Start Guide

## un setup bien especificado

### Paso 1: Obtener Rascal JAR

se necesita el archivo `rascal.jar` en el directorio raíz del proyecto.

**Opción A: Descargar desde el sitio oficial**

**Opción B: Si ya tienes Rascal instalado**

copia tu `rascal.jar` al directorio del proyecto:

```bash
cp /ruta/a/tu/rascal.jar /Users/carlagonzalez/Desktop/swimmingdsl/
```

### Paso 2: verificar javita

```bash
java -version
```

se deberia ver Java 11 o superior

### Paso 3: instalar Node.js dependencies

```bash
cd /Users/carlagonzalez/Desktop/swimmingdsl/server
npm install
```

### Paso 4: iniciar el servidor

```bash
npm start
```

### Paso 5: abrir en el navegador

abrir http://localhost:3000

## que se implementó?

### 1. **WebAPI.rsc** - Rascal Backend
- modulo Rascal que expone funciones para análisis y generación
- exporto resultados como JSON
- se ejecuta desde linea de comandos

**Ubicación**: `src/WebAPI.rsc`

**funciones principales**:
- `analyzeToJSON(str code)` - Analiza código y retorna JSON
- `generateToJSON(str goal, int distance, list[str] styles, int duration)` - Genera sesiones

### 2. **Node.js Server**
- servidor que actúa como puente
- recibe peticiones HTTP del frontend
- ejecuta Rascal vía `java -jar`
- retorna JSON al navegador

**Ubicación**: `server/server.js`

**Endpoints**:
- `GET /api/health` - Health check
- `POST /api/analyze` - Analiza código DSL
- `POST /api/generate` - Genera sesiones

### 3. **Frontend** 
- interfaz HTML/CSS/JS que se conecta al servidor
- editor de código con syntax highlighting
- visualización de análisis
- formulario para generar sesiones

**Ubicación**: `web/index-server.html`

## flujo de los datos

```
Usuario escribe código DSL
    ↓
Frontend (JavaScript)
    ↓ HTTP POST /api/analyze
Node.js Server
    ↓ exec("java -jar rascal.jar")
Rascal ejecuta WebAPI.rsc
    ↓ parseea y analiza
Rascal retorna JSON
    ↓ stdout
Node.js captura JSON
    ↓ HTTP Response
Frontend muestra resultados
```


### Test del servidor Node.js

Con el servidor corriendo:

```bash
# Health check
curl http://localhost:3000/api/health

# Analizar código
curl -X POST http://localhost:3000/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"session test { swim 100 m easy }"}'
```

### test del frontend

1. abre http://localhost:3000
2. esccribe código en el editor
3. presiona "Analizar"
4. se deberá ver resultados

## Solución de Problemas

### "Cannot find module 'express'"

```bash
cd server
npm install
```

### "java: command not found"

Instala Java 11+:
- macOS: `brew install openjdk@11`
- Ubuntu: `sudo apt install openjdk-11-jdk`
- Windows: Descarga desde https://adoptium.net/

### "rascal.jar not found"

Descarga rascal.jar:

```bash
cd /Users/carlagonzalez/Desktop/swimmingdsl
curl -L -o rascal.jar https://update.rascal-mpl.org/console/rascal-shell-stable.jar
```

### "Error: EADDRINUSE" (puerto ocupado)

Cambia el puerto:

```bash
PORT=8080 npm start
```

### rascal da error de sintaxis

Revisa que `WebAPI.rsc` esté en `src/` y que todos los imports sean correctos.

## 📁 Archivos Creados

```
swimmingdsl/
├── src/
│   └── WebAPI.rsc              ✨ API Rascal con exports JSON
├── server/
│   ├── server.js               ✨ servidor Node.js Express
│   └── package.json            ✨ dependencias npm
├── web/
│   ├── index-server.html       ✨ frontend que usa servidor
│   ├── index.html              (anterior - standalone)
│   ├── parser.js               (anterior - parser JS standalone)
│   └── analyzer.js             (anterior - analyzer JS standalone)
├── WEB_SERVER_SETUP.md         ✨ documentación completa
├── QUICKSTART.md               ✨ esta guía
└── rascal.jar                  ⚠️  NECESARIO descargar
```

## dos Modos de Uso

### Modo 1: Standalone (sin servidor)

**Archivo**: `web/index.html`

**Ventajas**:
- no requiere servidor
- funciona offline
- setup simple

**Desventajas**:
- Usa parser JS (no Rascal)
- Puede tener diferencias

**Uso**:
```bash
open web/index.html
```

### Modo 2: server Mode (con Rascal)

**Archivo**: `web/index-server.html` (servido por server.js)

**Ventajas**:
- Usa el parser oficial de Rascal
- Resultados 100% confiables
- Puede extenderse con features chéveres

**Uso**:
```bash
cd server && npm start
# Abre http://localhost:3000
```

## mas Información

- **WEB_SERVER_SETUP.md** - Documentación completa del servidor
- **README.md** - Documentación del DSL
- **ARCHITECTURE.md** - Arquitectura del proyecto

---

**¿Preguntas?** Revisa WEB_SERVER_SETUP.md para documentación detallada.
