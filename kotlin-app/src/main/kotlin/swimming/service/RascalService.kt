package swimming.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import swimming.model.AnalysisResult
import swimming.model.GenerateResult
import java.io.File
import java.util.concurrent.TimeUnit

class RascalService {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Resolve paths relative to the kotlin-app directory
    private val projectRoot: File by lazy {
        // kotlin-app/ is inside swimmingdsl/, so go up one level
        val jarDir = File(System.getProperty("user.dir"))
        val candidate = jarDir.resolve("../rascal-shell-stable.jar")
        if (candidate.exists()) jarDir.resolve("..").canonicalFile
        else {
            // Fallback: try to find from the class location
            val altRoot = jarDir.parentFile
            if (altRoot?.resolve("rascal-shell-stable.jar")?.exists() == true) altRoot
            else jarDir.resolve("..").canonicalFile
        }
    }

    private val rascalJar: File get() = projectRoot.resolve("rascal-shell-stable.jar")
    private val srcDir: File get() = projectRoot.resolve("src")

    suspend fun analyze(code: String): AnalysisResult = withContext(Dispatchers.IO) {
        try {
            val output = executeRascal("analyze", listOf(code))
            val jsonStr = extractJson(output)
                ?: return@withContext AnalysisResult(success = false, error = "No se encontró JSON en la salida de Rascal")
            json.decodeFromString<AnalysisResult>(jsonStr)
        } catch (e: Exception) {
            AnalysisResult(success = false, error = e.message ?: "Error desconocido")
        }
    }

    suspend fun generate(goal: String, distance: Int, styles: List<String>, duration: Int): GenerateResult =
        withContext(Dispatchers.IO) {
            try {
                val stylesStr = styles.joinToString(",")
                val output = executeRascal("generate", listOf(goal, distance.toString(), stylesStr, duration.toString()))
                val jsonStr = extractJson(output)
                    ?: return@withContext GenerateResult(success = false, error = "No se encontró JSON en la salida de Rascal")
                json.decodeFromString<GenerateResult>(jsonStr)
            } catch (e: Exception) {
                GenerateResult(success = false, error = e.message ?: "Error desconocido")
            }
        }

    private fun executeRascal(command: String, args: List<String>): String {
        if (!rascalJar.exists()) {
            throw RuntimeException("No se encontró rascal-shell-stable.jar en ${rascalJar.absolutePath}")
        }
        if (!srcDir.exists()) {
            throw RuntimeException("No se encontró directorio src/ en ${srcDir.absolutePath}")
        }

        // Build command (same approach as server.js)
        val shellCmd = listOf(
            "java",
            "-Dfile.encoding=UTF-8",
            "-Drascal.projectPath=${srcDir.absolutePath}",
            "-jar", rascalJar.absolutePath,
            "Runner.rsc",
            command
        ) + args

        val process = ProcessBuilder(shellCmd)
            .directory(srcDir)
            .redirectErrorStream(false)
            .start()

        // Close stdin immediately
        process.outputStream.close()

        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()

        val finished = process.waitFor(30, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            throw RuntimeException("Rascal timeout (30s)")
        }

        if (process.exitValue() != 0 && stdout.isBlank()) {
            throw RuntimeException("Rascal error (exit ${process.exitValue()}): ${stderr.take(500)}")
        }

        return stdout
    }

    /**
     * Extracts the last valid JSON object from Rascal output.
     * Same algorithm as server.js — finds the last JSON with a "success" field.
     */
    private fun extractJson(output: String): String? {
        // Strip ANSI escape codes
        val clean = output.replace(Regex("\\x1b\\[[0-9;]*[a-zA-Z]"), "")

        var lastValidJson: String? = null
        var startIdx = 0

        while (startIdx < clean.length) {
            val braceIdx = clean.indexOf('{', startIdx)
            if (braceIdx == -1) break

            // Find matching closing brace
            var depth = 0
            var inString = false
            var escaped = false
            var endIdx = -1

            for (i in braceIdx until clean.length) {
                val ch = clean[i]

                if (escaped) {
                    escaped = false
                    continue
                }
                if (ch == '\\' && inString) {
                    escaped = true
                    continue
                }
                if (ch == '"') {
                    inString = !inString
                    continue
                }
                if (!inString) {
                    if (ch == '{') depth++
                    else if (ch == '}') {
                        depth--
                        if (depth == 0) {
                            endIdx = i
                            break
                        }
                    }
                }
            }

            if (endIdx != -1) {
                val candidate = clean.substring(braceIdx, endIdx + 1)
                try {
                    val parsed = Json.parseToJsonElement(candidate)
                    if (parsed is kotlinx.serialization.json.JsonObject) {
                        if (parsed.containsKey("success")) {
                            lastValidJson = candidate
                        } else if (lastValidJson == null) {
                            lastValidJson = candidate
                        }
                    }
                } catch (_: Exception) {
                    // Not valid JSON, skip
                }
            }

            startIdx = braceIdx + 1
        }

        return lastValidJson
    }
}
