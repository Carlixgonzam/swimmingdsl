package swimming.util

/**
 * Adjusts the generated DSL code so the total distance (as Rascal's analysis
 * would compute it) matches the requested distance. Compensates for integer
 * division loss in the generation by adjusting the cooldown swim distance.
 */
fun adjustGeneratedDistance(code: String, requestedDistance: Int): String {
    // Calculate distance using the same algorithm as Rascal's analyzeToJSON
    var calculatedDist = 0

    // 1. Sum all "N m" distance markers
    for (match in Regex("(\\d+)\\s+m(?![a-zA-Z])").findAll(code)) {
        calculatedDist += match.groupValues[1].toInt()
    }

    // 2. For interval blocks "N x WORD N m", add (N-1)*dist
    for (match in Regex("(\\d+)\\s+x\\s+\\w+\\s+(\\d+)\\s+m(?![a-zA-Z])").findAll(code)) {
        calculatedDist += (match.groupValues[1].toInt() - 1) * match.groupValues[2].toInt()
    }

    val diff = requestedDistance - calculatedDist
    if (diff == 0) return code

    // Find the last "swim N m" (typically the cooldown) and adjust N
    val swimMatches = Regex("(swim\\s+)(\\d+)(\\s+m)").findAll(code).toList()
    if (swimMatches.isEmpty()) return code

    val lastSwim = swimMatches.last()
    val currentDist = lastSwim.groupValues[2].toInt()
    val newDist = currentDist + diff
    if (newDist <= 0) return code

    return code.substring(0, lastSwim.groups[2]!!.range.first) +
            newDist.toString() +
            code.substring(lastSwim.groups[2]!!.range.last + 1)
}
