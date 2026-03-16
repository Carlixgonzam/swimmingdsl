package swimming.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val level: String = "intermediate",
    val availableMinutes: Int = 60,
    val preferredStyles: List<String> = listOf("freestyle"),
    val hasEquipment: List<String> = emptyList()
)
