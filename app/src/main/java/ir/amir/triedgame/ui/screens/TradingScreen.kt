package ir.amir.triedgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.amir.triedgame.model.AssetCatalog
import ir.amir.triedgame.model.Position
import ir.amir.triedgame.model.PositionSide
import ir.amir.triedgame.ui.GameViewModel
import ir.amir.triedgame.ui.Timeframe
import ir.amir.triedgame.ui.components.CandlestickChart
import ir.amir.triedgame.ui.theme.TsAccent
import ir.amir.triedgame.ui.theme.TsGreen
import ir.amir.triedgame.ui.theme.TsRed
import ir.amir.triedgame.ui.theme.TsSurfaceElevated
import java.util.Locale

@Composable
fun TradingScreen(viewModel: GameViewModel) {
    val price = viewModel.currentPrices[viewModel.selectedAsset.symbol] ?: viewModel.selectedAsset.startingPriceUsd
    var customAmountText by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(10.0) }
    var leverage by remember { mutableIntStateOf(1) }

    val tradeAmount = customAmountText.toDoubleOrNull() ?: selectedPreset

    Row(Modifier.fillMaxSize()) {
        // Asset list
        LazyColumn(
            modifier = Modifier
                .width(150.dp)
                .fillMaxHeight()
                .background(TsSurfaceElevated)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(AssetCatalog.all) { asset ->
                val isSelected = asset.symbol == viewModel.selectedAsset.symbol
                val assetPrice = viewModel.currentPrices[asset.symbol] ?: asset.startingPriceUsd
                Surface(
                    color = if (isSelected) TsAccent.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectAsset(asset) }
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(asset.symbol, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = if (isSelected) TsAccent else MaterialTheme.colorScheme.onSurface)
                        Text(
                            formatPrice(assetPrice),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Chart + trade controls
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${viewModel.selectedAsset.displayName} (${viewModel.selectedAsset.symbol})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(formatPrice(price), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                // Timeframe selector
                Row {
                    Timeframe.entries.forEach { tf ->
                        val selected = tf == viewModel.timeframe
                        Surface(
                            color = if (selected) TsAccent else TsSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .clickable { viewModel.selectTimeframe(tf) }
                        ) {
                            Text(
                                tf.label,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            CandlestickChart(
                candles = viewModel.candles,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(12.dp))

            // Amount + leverage
            Text("مقدار (دلار)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(10.0, 50.0, 100.0).forEach { amt ->
                    val selected = customAmountText.isEmpty() && selectedPreset == amt
                    Surface(
                        color = if (selected) TsAccent else TsSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable { selectedPreset = amt; customAmountText = "" }
                    ) {
                        Text(
                            "$${amt.toInt()}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = { customAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("دلخواه") },
                    singleLine = true,
                    modifier = Modifier.width(110.dp)
                )
                Spacer(Modifier.weight(1f))
                Text("موجودی: ${String.format(Locale.US, "%.2f", viewModel.wallet.usdBalance)}$", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(10.dp))
            Text("اهرم (فیوچرز)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Row {
                listOf(1, 5, 10, 20).forEach { lev ->
                    val selected = leverage == lev
                    Surface(
                        color = if (selected) TsAccent else TsSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable { leverage = lev }
                    ) {
                        Text(
                            "${lev}x",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (leverage > 1) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "با اهرم ${leverage}x، حجم معامله: ${(tradeAmount * leverage).toInt()}$ — اگه ضرر به اندازه‌ی مارجین برسه، پوزیشن لیکویید می‌شه.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TsRed
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.openPosition(PositionSide.LONG, tradeAmount, leverage) },
                    colors = ButtonDefaults.buttonColors(containerColor = TsGreen),
                    modifier = Modifier.weight(1f)
                ) { Text("خرید / Long", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.openPosition(PositionSide.SHORT, tradeAmount, leverage) },
                    colors = ButtonDefaults.buttonColors(containerColor = TsRed),
                    modifier = Modifier.weight(1f)
                ) { Text("فروش / Short", fontWeight = FontWeight.Bold) }
            }
        }

        // Open positions
        Column(
            modifier = Modifier
                .width(230.dp)
                .fillMaxHeight()
                .background(TsSurfaceElevated)
                .padding(8.dp)
        ) {
            Text("پوزیشن‌های باز", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(viewModel.positions) { position ->
                    PositionRow(position, viewModel)
                }
            }
        }
    }
}

@Composable
private fun PositionRow(position: Position, viewModel: GameViewModel) {
    val price = viewModel.currentPrices[position.assetSymbol] ?: position.entryPrice
    val pnl = position.currentPnlUsd(price)
    val pnlPercent = position.pnlPercentOfMargin(price)
    val color = if (pnl >= 0) TsGreen else TsRed

    Surface(color = androidx.compose.ui.graphics.Color(0xFF232A3A), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${position.assetSymbol} • ${if (position.side == PositionSide.LONG) "Long" else "Short"}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                if (position.leverage > 1) {
                    Text("${position.leverage}x", color = TsAccent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                String.format(Locale.US, "%+.2f$ (%+.1f%%)", pnl, pnlPercent),
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = { viewModel.closePosition(position) }, contentPadding = PaddingValues(0.dp)) {
                Text("بستن پوزیشن", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun formatPrice(price: Double): String {
    val decimals = if (price < 1) 4 else 2
    return String.format(Locale.US, "%.${decimals}f $", price)
}
