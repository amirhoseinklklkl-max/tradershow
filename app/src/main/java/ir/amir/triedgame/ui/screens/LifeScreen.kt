package ir.amir.triedgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.triedgame.model.LifeStats
import ir.amir.triedgame.ui.GameViewModel
import ir.amir.triedgame.ui.theme.TsSurfaceElevated
import ir.amir.triedgame.ui.theme.statColor

private data class LifeItem(
    val title: String,
    val priceToman: Double,
    val xpReward: Int,
    val isHungerItem: Boolean,
    val effect: (LifeStats) -> LifeStats,
    val description: String
)

private val lifeItems = listOf(
    LifeItem("قهوه", 30_000.0, 5, false, { it.copy(energy = it.energy + 15) }, "انرژی +۱۵"),
    LifeItem("ساندویچ فست‌فود", 80_000.0, 10, true, { it.copy(hunger = it.hunger + 30) }, "گرسنگی +۳۰"),
    LifeItem("غذای رستوران", 250_000.0, 20, true, { it.copy(hunger = it.hunger + 60, health = it.health + 5) }, "گرسنگی +۶۰، سلامتی +۵"),
    LifeItem("میان‌وعده سالم", 60_000.0, 8, true, { it.copy(hunger = it.hunger + 20, health = it.health + 5) }, "گرسنگی +۲۰، سلامتی +۵"),
    LifeItem("استراحت کوتاه", 50_000.0, 5, false, { it.copy(energy = it.energy + 20) }, "انرژی +۲۰"),
    LifeItem("ویزیت دکتر", 400_000.0, 25, false, { it.copy(health = it.health + 40) }, "سلامتی +۴۰"),
    LifeItem("سفر داخلی", 3_000_000.0, 100, false, { it }, "افزایش تجربه + تزئینی"),
    LifeItem("ماشین اقتصادی", 200_000_000.0, 500, false, { it }, "دارایی تزئینی، تجربه‌ی زیاد")
)

@Composable
fun LifeScreen(viewModel: GameViewModel) {
    val stats = viewModel.lifeStats
    var message by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().padding(20.dp)) {
        Column(Modifier.fillMaxSize()) {
            Text("زندگی من", style = MaterialTheme.typography.headlineSmall)
            message?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Text("مغازه", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(lifeItems) { item ->
                    Surface(color = TsSurfaceElevated, shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(item.description, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Text(String.format("%,.0f تومان", item.priceToman), style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val ok = viewModel.spendTomanInLife(item.priceToman, item.xpReward, item.isHungerItem, item.effect)
                                    message = if (ok) null else "موجودی تومانی کافی نیست"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) { Text("خرید", style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                }
            }
        }

        // Small stat chips, top-right corner, colored dynamically by value.
        Column(
            modifier = Modifier.align(Alignment.TopEnd).width(120.dp),
            horizontalAlignment = Alignment.End
        ) {
            MiniStatChip("سلامتی", stats.health)
            Spacer(Modifier.height(4.dp))
            MiniStatChip("گرسنگی", stats.hunger)
            Spacer(Modifier.height(4.dp))
            MiniStatChip("انرژی", stats.energy)
        }
    }
}

@Composable
private fun MiniStatChip(label: String, value: Int) {
    val color = statColor(value)
    Surface(color = TsSurfaceElevated, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp).width(110.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text("$value%", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(3.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF2A3040))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    }
}
