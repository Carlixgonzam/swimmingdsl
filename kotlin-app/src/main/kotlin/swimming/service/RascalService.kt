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
    private val projectRoot: File by lazy {
        val jarDir = File(System.getProperty("user.dir"))
        val candidate = jarDir.resolve("../rascal-shell-stable.jar")
        if (candidate.exists()) jarDir.resolve("..").canonicalFile
        else {
            val altRoot = jarDir.parentFile
            if (altRoot?.resolve("rascal-shell-stable.jar")?.exists() == true) altRoot
            else jarDir.resolve("..").canonicalFile
        }
    }

    private val rascalJar: File get() = projectRoot.resolve("rascal-shell-stable.jar")
    private val srcDir: File get() = projectRoot.resolve("src")
    suspend fun analyze(code: String): AnalysisResult = withContext(Dispatchers.IO) {
        try {
            println("Iniciando análisis...")
            println("Código (${code.length} chars):")
            println("${code.lines().joinToString("\n")}")
            val startTime = System.currentTimeMillis()
            val output = executeRascal("analyze", listOf(code))
            val elapsed = System.currentTimeMillis() - startTime
            println("rascal respondió en ${elapsed}ms")
            println("stdout (${output.length} chars): ${output.take(300)}")
            val jsonStr = extractJson(output)
            if (jsonStr == null) {
                println("no se encontro JSON en la salida")
                return@withContext AnalysisResult(success = false, error = "No se encontró JSON en la salida de Rascal")
            }
            println("JSON extraído (${jsonStr.length} chars):")
            println(" $jsonStr")
            val result = json.decodeFromString<AnalysisResult>(jsonStr)
            println("esultado: distance=${result.totalDistance}m (${result.distanceKm}km)")
            println("sessions=${result.sessionCount}, styles=${result.styles}")
            println("intensities=${result.intensities}")
            println("time: swim=${result.time.swimFormatted}, rest=${result.time.restFormatted}, total=${result.time.totalFormatted}")
            println("rest: ${result.rest.periods} periods, avg=${result.rest.average}s")
            println("equipment=${result.equipment}, drills=${result.drills}")
            result
        } catch (e: Exception) {
            println("error: ${e.message}")
            e.printStackTrace()
            AnalysisResult(success = false, error = e.message ?: "error desconocido")
        }
    }

    suspend fun generate(goal: String, distance: Int, styles: List<String>, duration: Int): GenerateResult =
        withContext(Dispatchers.IO) {
            try {
                val stylesStr = styles.joinToString(",")
                println("iniciando generación...")
                println("params: goal=$goal, distance=${distance}m, styles=[$stylesStr], duration=${duration}min")
                val startTime = System.currentTimeMillis()
                val output = executeRascal("generate", listOf(goal, distance.toString(), stylesStr, duration.toString()))
                val elapsed = System.currentTimeMillis() - startTime
                println("rascal respondió en ${elapsed}ms")
                println("stdout (${output.length} chars): ${output.take(300)}")
                val jsonStr = extractJson(output)
                if (jsonStr == null) {
                    println("no se encontró JSON en la salida")
                    return@withContext GenerateResult(success = false, error = "No se encontró JSON en la salida de Rascal")
                }
                println("JSON extraído (${jsonStr.length} chars)")
                val result = json.decodeFromString<GenerateResult>(jsonStr)
                println("goal=${result.goal}, distance=${result.distance}m")
                println("cdigo generado:")
                result.code?.lines()?.forEach { println("║   $it") }
                result
            } catch (e: Exception) {
                println("error: ${e.message}")
                e.printStackTrace()
                GenerateResult(success = false, error = e.message ?: "error desconocido")
            }
        }

    private fun executeRascal(command: String, args: List<String>): String {
        if (!rascalJar.exists()) {
            throw RuntimeException("No se encontró rascal-shell-stable.jar en ${rascalJar.absolutePath}")
        }
        if (!srcDir.exists()) {
            throw RuntimeException("No se encontró directorio src/ en ${srcDir.absolutePath}")
        }
        val shellCmd = listOf(
            "java",
            "-Dfile.encoding=UTF-8",
            "-Drascal.projectPath=${srcDir.absolutePath}",
            "-jar", rascalJar.absolutePath,
            "Runner.rsc",
            command
        ) + args

        println("║ [CMD] ${shellCmd.joinToString(" ") { if (it.length > 80) it.take(80) + "..." else it }}")

        val process = ProcessBuilder(shellCmd)
            .directory(srcDir)
            .redirectErrorStream(false)
            .start()
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
    private fun extractJson(output: String): String? {
        
        val clean = output.replace(Regex("\\x1b\\[[0-9;]*[a-zA-Z]"), "")
        var lastValidJson: String? = null
        var startIdx = 0
        while (startIdx < clean.length) {
            val braceIdx = clean.indexOf('{', startIdx)
            if (braceIdx == -1) break
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
                    // not valid JSON so skip jajaja
                }
            }

            startIdx = braceIdx + 1
        }

        return lastValidJson
    }
}
