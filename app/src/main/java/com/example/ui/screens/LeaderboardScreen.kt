package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LeaderboardEntry
import com.example.data.LevelSystem
import com.example.ui.components.DuoButton
import com.example.ui.components.PlayerAvatar
import com.example.ui.theme.BeeYellow
import com.example.ui.theme.BorderGray
import com.example.ui.theme.CardinalRed
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.FeatherGreen
import com.example.ui.theme.FeatherGreenDark
import com.example.ui.theme.FoxOrange
import com.example.ui.theme.MacawBlue
import com.example.ui.theme.MacawBlueDark
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState

@Composable
fun LeaderboardScreen(
    leaderboardState: UiState<List<LeaderboardEntry>>,
    currentUserId: String,
    isGuestMode: Boolean = false,
    onConnectGoogle: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Top Puntos, 1: Top Ganadores, 2: Mi Posición

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .testTag("leaderboard_screen"),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (leaderboardState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MacawBlue)
                }
            }
            is UiState.Error -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = leaderboardState.message,
                        color = CardinalRed,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            is UiState.Success -> {
                val rawEntries = leaderboardState.data
                val sortedEntries = remember(rawEntries, selectedTab) {
                    when (selectedTab) {
                        1 -> rawEntries.sortedByDescending { it.wins }
                        else -> rawEntries.sortedByDescending { it.totalPoints }
                    }
                }

                val myIndex = sortedEntries.indexOfFirst { it.userId == currentUserId }
                val myEntry = if (myIndex >= 0) sortedEntries[myIndex] else null
                val myRank = if (myIndex >= 0) myIndex + 1 else 0

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = BeeYellow,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Tabla de Posiciones",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = DeepNavy,
                                )
                                Text(
                                    text = "Compite, gana partidas y sube de nivel en el ranking",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }

                    // Selector de Vistas de Posiciones
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SoftSurface)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            listOf(
                                0 to "🏆 Puntos & Nivel",
                                1 to "🥇 Top Ganadores",
                                2 to "📊 Mi Posición",
                            ).forEach { (tabIdx, label) ->
                                val isSelected = selectedTab == tabIdx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) PureWhite else Color.Transparent)
                                        .then(
                                            if (isSelected) Modifier.border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                                            else Modifier
                                        )
                                        .clickable { selectedTab = tabIdx },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MacawBlue else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }

                    if (selectedTab == 2) {
                        // Vista personal: Mi Posición y Estadísticas
                        item {
                            if (myEntry != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(SoftSurface)
                                        .border(2.dp, MacawBlue, RoundedCornerShape(22.dp))
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = "Tu Ficha de Rendimiento",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = DeepNavy,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(14.dp))

                                    PlayerAvatar(
                                        iconKey = myEntry.avatarIcon,
                                        colorHex = myEntry.avatarColor,
                                        size = 80.dp,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = myEntry.username,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = DeepNavy,
                                    )
                                    Text(
                                        text = "Posición Global: #${myRank}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BeeYellow,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Nivel ${myEntry.level} · ${LevelSystem.titleForLevel(myEntry.level.toInt())}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                    )

                                    Spacer(Modifier.height(18.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(PureWhite)
                                                .border(1.5.dp, BorderGray, RoundedCornerShape(16.dp))
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text("${myEntry.wins}", style = MaterialTheme.typography.headlineMedium, color = FeatherGreenDark, fontWeight = FontWeight.Bold)
                                            Text("Ganados 🏆", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(PureWhite)
                                                .border(1.5.dp, BorderGray, RoundedCornerShape(16.dp))
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text("${myEntry.calculatedLosses}", style = MaterialTheme.typography.headlineMedium, color = CardinalRed, fontWeight = FontWeight.Bold)
                                            Text("Perdidos ❌", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(PureWhite)
                                                .border(1.5.dp, BorderGray, RoundedCornerShape(16.dp))
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text("${myEntry.winRatePercent}%", style = MaterialTheme.typography.headlineMedium, color = MacawBlue, fontWeight = FontWeight.Bold)
                                            Text("Efectividad", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 30.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(Icons.Rounded.Person, contentDescription = null, tint = BorderGray, modifier = Modifier.size(56.dp))
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "¡Juega una partida para registrar tus victorias y posición!",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    } else if (sortedEntries.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = BorderGray,
                                    modifier = Modifier.size(64.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "¡Sé el primero en sumar puntos y liderar el ranking!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        // Top 3 Podium
                        item {
                            TopThreePodium(entries = sortedEntries.take(3), showWins = selectedTab == 1)
                        }

                        itemsIndexed(sortedEntries, key = { _, e -> e.userId }) { index, entry ->
                            val rank = index + 1
                            val isMe = entry.userId == currentUserId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isMe) MacawBlue.copy(alpha = 0.08f) else PureWhite)
                                    .border(
                                        width = if (isMe) 2.dp else 1.5.dp,
                                        color = if (isMe) MacawBlue else BorderGray,
                                        shape = RoundedCornerShape(18.dp),
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "#$rank",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = when (rank) {
                                        1 -> BeeYellow
                                        2 -> Color(0xFF94A3B8)
                                        3 -> FoxOrange
                                        else -> TextSecondary
                                    },
                                    modifier = Modifier.width(42.dp),
                                )
                                PlayerAvatar(
                                    iconKey = entry.avatarIcon,
                                    colorHex = entry.avatarColor,
                                    size = 44.dp,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isMe) "${entry.username} (Tú)" else entry.username,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "Nivel ${entry.level} · ${entry.wins} ganados / ${entry.calculatedLosses} perdidos",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                    )
                                }
                                Text(
                                    text = if (selectedTab == 1) "${entry.wins} victorias" else "${entry.totalPoints} pts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTab == 1) BeeYellow else FeatherGreenDark,
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TopThreePodium(entries: List<LeaderboardEntry>, showWins: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SoftSurface)
            .border(1.5.dp, BorderGray, RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        val second = entries.getOrNull(1)
        val first = entries.getOrNull(0)
        val third = entries.getOrNull(2)

        if (second != null) PodiumColumn(rank = 2, entry = second, badgeColor = Color(0xFF94A3B8), avatarSize = 54.dp, showWins = showWins)
        if (first != null) PodiumColumn(rank = 1, entry = first, badgeColor = BeeYellow, avatarSize = 68.dp, showWins = showWins)
        if (third != null) PodiumColumn(rank = 3, entry = third, badgeColor = FoxOrange, avatarSize = 52.dp, showWins = showWins)
    }
}

@Composable
private fun PodiumColumn(
    rank: Int,
    entry: LeaderboardEntry,
    badgeColor: Color,
    avatarSize: Dp,
    showWins: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(badgeColor)
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = "${rank}°",
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        PlayerAvatar(
            iconKey = entry.avatarIcon,
            colorHex = entry.avatarColor,
            size = avatarSize,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = entry.username,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (showWins) "${entry.wins} victorias" else "${entry.totalPoints} pts",
            style = MaterialTheme.typography.labelSmall,
            color = FeatherGreenDark,
        )
    }
}
