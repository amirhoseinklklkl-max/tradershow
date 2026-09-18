package ir.amir.triedgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.triedgame.ui.ChallengeUiState
import ir.amir.triedgame.ui.GameViewModel
import ir.amir.triedgame.ui.theme.TsAccent
import ir.amir.triedgame.ui.theme.TsGreen
import ir.amir.triedgame.ui.theme.TsSurfaceElevated

@Composable
fun ChallengesScreen(viewModel: GameViewModel) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("چالش‌های امروز", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "هر روز کامل کن، پاداش تومانی بگیر و تجربه (XP) کسب کن",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(viewModel.challenges) { state ->
                ChallengeCard(state, onClaim = { viewModel.claimChallenge(state.challenge.id) })
            }
        }
    }
}

@Composable
private fun ChallengeCard(state: ChallengeUiState, onClaim: () -> Unit) {
    Surface(
        color = TsSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(state.challenge.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            val progressFraction = (state.progress.toFloat() / state.challenge.target.toFloat()).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF2A3040))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (state.isComplete) TsGreen else TsAccent)
                )
            }
            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    "${state.progress}/${state.challenge.target} • پاداش: ${state.challenge.rewardToman.toInt()} تومان",
                    style = MaterialTheme.typography.bodySmall
                )
                when {
                    state.isClaimed -> Text("دریافت شد ✓", color = TsGreen, style = MaterialTheme.typography.labelMedium)
                    state.isComplete -> Button(onClick = onClaim) { Text("دریافت پاداش") }
                    else -> {}
                }
            }
        }
    }
}
