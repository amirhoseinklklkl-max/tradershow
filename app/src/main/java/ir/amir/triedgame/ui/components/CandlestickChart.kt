package ir.amir.triedgame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import ir.amir.triedgame.model.Candle
import ir.amir.triedgame.ui.theme.TsBackground
import ir.amir.triedgame.ui.theme.TsGreen
import ir.amir.triedgame.ui.theme.TsRed

private const val MIN_VISIBLE = 12
private const val DEFAULT_VISIBLE = 60

/**
 * Candlestick chart with pinch-to-zoom (changes how many candles are
 * visible) and drag-to-pan (scrolls through history), similar to a real
 * trading app. Drawn on a plain Compose Canvas -- no external chart library.
 *
 * Pass a growing [candles] list; the chart always starts anchored to the
 * most recent candle until the user pans away from it.
 */
@Composable
fun CandlestickChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier
) {
    var visibleCount by remember { mutableIntStateOf(DEFAULT_VISIBLE) }
    // Fractional scroll offset from the right edge (0 = pinned to latest candle).
    var scrollFromEnd by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .background(TsBackground)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val maxVisible = candles.size.coerceAtLeast(MIN_VISIBLE)
                    visibleCount = (visibleCount / zoom)
                        .toInt()
                        .coerceIn(MIN_VISIBLE, maxVisible)

                    // Dragging right (positive pan.x) reveals older candles.
                    val candleWidthPx = (size.width / visibleCount.toFloat()).coerceAtLeast(1f)
                    val deltaCandles = pan.x / candleWidthPx
                    val maxScroll = (candles.size - visibleCount).coerceAtLeast(0).toFloat()
                    scrollFromEnd = (scrollFromEnd - deltaCandles).coerceIn(0f, maxScroll)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (candles.isEmpty()) return@Canvas

            val count = visibleCount.coerceAtMost(candles.size)
            val maxScroll = (candles.size - count).coerceAtLeast(0)
            val startIndex = (candles.size - count - scrollFromEnd.toInt()).coerceIn(0, maxScroll)
            val visible = candles.subList(startIndex, (startIndex + count).coerceAtMost(candles.size))
            if (visible.isEmpty()) return@Canvas

            val maxPrice = visible.maxOf { it.high }
            val minPrice = visible.minOf { it.low }
            val priceRange = (maxPrice - minPrice).let { if (it <= 0.0) 1.0 else it }

            val candleSlotWidth = size.width / visible.size
            val bodyWidth = candleSlotWidth * 0.62f
            val wickWidth = (candleSlotWidth * 0.1f).coerceAtLeast(2f)

            fun yFor(price: Double): Float {
                val fraction = (price - minPrice) / priceRange
                return (size.height * (1.0 - fraction)).toFloat()
            }

            visible.forEachIndexed { index, candle ->
                val centerX = candleSlotWidth * index + candleSlotWidth / 2f
                val isBullish = candle.close >= candle.open
                val color = if (isBullish) TsGreen else TsRed

                val highY = yFor(candle.high)
                val lowY = yFor(candle.low)
                val openY = yFor(candle.open)
                val closeY = yFor(candle.close)

                drawLine(
                    color = color,
                    start = Offset(centerX, highY),
                    end = Offset(centerX, lowY),
                    strokeWidth = wickWidth
                )

                val bodyTop = minOf(openY, closeY)
                val bodyBottom = maxOf(openY, closeY).coerceAtLeast(bodyTop + 2f)
                drawRect(
                    color = color,
                    topLeft = Offset(centerX - bodyWidth / 2f, bodyTop),
                    size = androidx.compose.ui.geometry.Size(bodyWidth, bodyBottom - bodyTop)
                )
            }
        }
    }
}
