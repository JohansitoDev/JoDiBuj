package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val bio: String = "",
    val avatarIcon: String = "brush",
    val avatarColor: String = "#1CB0F6",
    val totalPoints: Long = 0L,
    val level: Long = 1L,
    val completedLevel: Long = 1L,
    val gamesPlayed: Long = 0L,
    val wins: Long = 0L,
    val losses: Long = 0L,
    val currentStreak: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val calculatedLosses: Long
        get() = if (losses > 0L) losses else (gamesPlayed - wins).coerceAtLeast(0L)

    val winRatePercent: Int
        get() = if (gamesPlayed > 0L) ((wins.toFloat() / gamesPlayed.toFloat()) * 100f).toInt() else 0
}

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val avatarIcon: String = "brush",
    val avatarColor: String = "#1CB0F6",
    val totalPoints: Long = 0L,
    val level: Long = 1L,
    val wins: Long = 0L,
    val losses: Long = 0L,
    val gamesPlayed: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val calculatedLosses: Long
        get() = if (losses > 0L) losses else (gamesPlayed - wins).coerceAtLeast(0L)

    val winRatePercent: Int
        get() = if (gamesPlayed > 0L) ((wins.toFloat() / gamesPlayed.toFloat()) * 100f).toInt() else 0
}

@IgnoreExtraProperties
data class GameRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val status: String = "waiting",
    val promptWord: String = "Pez",
    val promptKey: String = "fish",
    val currentRound: Long = 1L,
    val totalRounds: Long = 3L,
    val maxPlayers: Long = 6L,
    val roundDurationSec: Long = 60L,
    val playerUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class RoomPlayer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomId: String = "",
    val username: String = "",
    val avatarIcon: String = "brush",
    val avatarColor: String = "#1CB0F6",
    val hasSubmittedRound: Long = 0L,
    @ServerTimestamp val joinedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class StrokeData(
    val colorHex: String = "#1CB0F6",
    val strokeWidth: Double = 8.0,
    val points: List<Double> = emptyList(),
)

@IgnoreExtraProperties
data class RoundDrawing(
    @DocumentId val id: String = "",
    val userId: String = "",
    val roomId: String = "",
    val username: String = "",
    val avatarColor: String = "#1CB0F6",
    val round: Long = 1L,
    val promptWord: String = "",
    val strokes: List<Map<String, Any>> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun parsedStrokes(): List<StrokeData> {
        return strokes.mapNotNull { strokeMap ->
            val color = strokeMap["colorHex"] as? String ?: "#1CB0F6"
            val width = (strokeMap["strokeWidth"] as? Number)?.toDouble() ?: 8.0
            val rawPoints = strokeMap["points"] as? List<*> ?: return@mapNotNull null
            val pts = rawPoints.mapNotNull { (it as? Number)?.toDouble() }
            if (pts.size >= 2) StrokeData(color, width, pts) else null
        }
    }
}

@IgnoreExtraProperties
data class RoundVote(
    @DocumentId val id: String = "",
    val voterId: String = "",
    val targetUserId: String = "",
    val roomId: String = "",
    val round: Long = 1L,
    val points: Long = 50L,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

data class DrawingPrompt(
    val key: String,
    val wordEs: String,
    val hintEs: String,
    val levelRequired: Int = 1,
    val xpReward: Int = 35,
)

data class GameLevel(
    val levelNumber: Int,
    val title: String,
    val description: String,
    val minUserLevel: Int,
    val prompts: List<DrawingPrompt>,
    val iconKey: String = "star",
    val themeColorHex: String = "#1CB0F6",
)

object DrawingCatalog {
    val prompts = listOf(
        DrawingPrompt("fish", "Pez", "Dibuja sus aletas y burbujas en el agua", 1, 30),
        DrawingPrompt("pencil", "Lápiz", "Punta afilada, cuerpo amarillo y borrador", 1, 30),
        DrawingPrompt("sun", "Sol", "Círculo radiante con rayos brillantes", 1, 25),
        DrawingPrompt("house", "Casa", "Techo triangular, puerta y chimenea", 1, 30),
        DrawingPrompt("apple", "Manzana", "Fruta brillante con su hojita verde", 2, 35),
        DrawingPrompt("tree", "Árbol", "Tronco firme y copa frondosa", 2, 40),
        DrawingPrompt("cat", "Gato", "Orejas puntiagudas y bigotes largos", 3, 35),
        DrawingPrompt("dog", "Perro", "Orejas caídas, hocico alegre y colita", 3, 35),
        DrawingPrompt("horse", "Caballo", "Crin ondulada y patas veloces", 3, 45),
        DrawingPrompt("star", "Estrella", "Cinco puntas brillantes en el cielo", 4, 40),
        DrawingPrompt("rocket", "Cohete", "Nave espacial despegando con fuego", 4, 55),
        DrawingPrompt("butterfly", "Mariposa", "Alas simétricas llenas de color", 4, 55),
        DrawingPrompt("robot", "Robot", "Cuerpo metálico, antenas y botones", 5, 60),
        DrawingPrompt("heart", "Corazón", "Simbolo de afecto y pulso brillante", 5, 50),
        DrawingPrompt("fire", "Fuego", "Llamas danzantes y calor ardiente", 6, 70),
    )

    val gameLevels = listOf(
        GameLevel(
            levelNumber = 1,
            title = "Nivel 1: Trazos Básicos",
            description = "Primeros dibujos para comenzar tu aventura",
            minUserLevel = 1,
            prompts = listOf(prompts[0], prompts[1], prompts[2], prompts[3]),
            iconKey = "pencil",
            themeColorHex = "#1CB0F6",
        ),
        GameLevel(
            levelNumber = 2,
            title = "Nivel 2: Naturaleza y Frutas",
            description = "Desbloqueado al superar el Nivel 1",
            minUserLevel = 2,
            prompts = listOf(prompts[4], prompts[5]),
            iconKey = "brush",
            themeColorHex = "#58CC02",
        ),
        GameLevel(
            levelNumber = 3,
            title = "Nivel 3: Reino Animal",
            description = "Desbloqueado al superar el Nivel 2",
            minUserLevel = 3,
            prompts = listOf(prompts[6], prompts[7], prompts[8]),
            iconKey = "cat",
            themeColorHex = "#FF9600",
        ),
        GameLevel(
            levelNumber = 4,
            title = "Nivel 4: Objetos y Vuelo",
            description = "Desbloqueado al superar el Nivel 3",
            minUserLevel = 4,
            prompts = listOf(prompts[9], prompts[10], prompts[11]),
            iconKey = "rocket",
            themeColorHex = "#CE82FF",
        ),
        GameLevel(
            levelNumber = 5,
            title = "Nivel 5: Tecnología y Magia",
            description = "Desbloqueado al superar el Nivel 4",
            minUserLevel = 5,
            prompts = listOf(prompts[12], prompts[13]),
            iconKey = "robot",
            themeColorHex = "#FF4B4B",
        ),
        GameLevel(
            levelNumber = 6,
            title = "Nivel 6: Desafío Leyenda",
            description = "Solo para los verdaderos maestros del dibujo",
            minUserLevel = 6,
            prompts = listOf(prompts[14]),
            iconKey = "fire",
            themeColorHex = "#FFC800",
        ),
    )

    fun byKey(key: String): DrawingPrompt =
        prompts.firstOrNull { it.key == key } ?: prompts.first()

    fun randomPrompt(excludeKey: String? = null): DrawingPrompt {
        val candidates = prompts.filter { it.key != excludeKey }
        return (if (candidates.isNotEmpty()) candidates else prompts).random()
    }
}

object LevelSystem {
    fun levelForPoints(points: Long): Int = ((points.coerceAtLeast(0L) / 100L) + 1L).toInt()

    fun xpProgressInLevel(points: Long): Float = ((points.coerceAtLeast(0L) % 100L) / 100f)

    fun xpToNextLevel(points: Long): Long = 100L - (points.coerceAtLeast(0L) % 100L)

    fun titleForLevel(level: Int): String = when {
        level <= 1 -> "Aprendiz del Trazo"
        level == 2 -> "Bocetista Alegre"
        level == 3 -> "Pincel Veloz"
        level == 4 -> "Ilustrador Estelar"
        level == 5 -> "Maestro del Color"
        level == 6 -> "Artista Legendario"
        else -> "Gran Campeón JoDiBuj"
    }

    fun isLevelUnlocked(levelNumber: Int, userLevel: Int, completedLevel: Int = 1): Boolean {
        if (levelNumber <= 1) return true
        return userLevel >= levelNumber || completedLevel >= (levelNumber - 1)
    }
}

object AvatarCatalog {
    val colors = listOf(
        "#1CB0F6", // Macaw Blue
        "#58CC02", // Feather Green
        "#FF9600", // Fox Orange
        "#CE82FF", // Beet Purple
        "#FF4B4B", // Cardinal Red
        "#FFC800", // Bee Yellow
        "#00CD9C", // Emerald Mint
        "#00B4D8", // Turquoise
        "#E52521", // Ruby Red
        "#111827", // Night Black
    )

    val icons = listOf(
        "brush" to "Pincel",
        "cat" to "Gato",
        "fish" to "Pez",
        "star" to "Estrella",
        "rocket" to "Cohete",
        "pencil" to "Lápiz",
        "palette" to "Paleta",
        "crown" to "Corona",
        "robot" to "Robot",
        "heart" to "Corazón",
        "sparkles" to "Destello",
        "fire" to "Fuego",
    )
}
