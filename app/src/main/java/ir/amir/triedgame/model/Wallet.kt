package ir.amir.triedgame.model

/** Fixed USD -> Toman conversion rate used throughout the game's economy. */
const val USD_TO_TOMAN_RATE = 220_000.0

data class Wallet(
    val usdBalance: Double = 0.0,
    val tomanBalance: Double = 0.0
) {
    fun convertUsdToToman(amountUsd: Double): Wallet {
        val usable = amountUsd.coerceAtMost(usdBalance)
        return copy(
            usdBalance = usdBalance - usable,
            tomanBalance = tomanBalance + usable * USD_TO_TOMAN_RATE
        )
    }

    fun convertTomanToUsd(amountToman: Double): Wallet {
        val usable = amountToman.coerceAtMost(tomanBalance)
        return copy(
            tomanBalance = tomanBalance - usable,
            usdBalance = usdBalance + usable / USD_TO_TOMAN_RATE
        )
    }

    fun addUsd(amount: Double): Wallet = copy(usdBalance = usdBalance + amount)
    fun addToman(amount: Double): Wallet = copy(tomanBalance = tomanBalance + amount)
    fun spendToman(amount: Double): Wallet? =
        if (tomanBalance >= amount) copy(tomanBalance = tomanBalance - amount) else null
}

enum class PositionSide { LONG, SHORT }

/**
 * A trading position. [marginUsd] is the amount actually held from the
 * wallet; [leverage] (1x = spot-style, >1x = futures-style) multiplies both
 * the exposure and the P&L. A position is liquidated (forced-closed at a
 * total loss of the margin) if losses reach 100% of the margin -- exactly
 * like a real futures exchange.
 */
data class Position(
    val id: String,
    val assetSymbol: String,
    val side: PositionSide,
    val entryPrice: Double,
    val marginUsd: Double,
    val leverage: Int = 1,
    val openedAtMillis: Long
) {
    val notionalUsd: Double get() = marginUsd * leverage

    fun currentPnlUsd(currentPrice: Double): Double {
        val change = (currentPrice - entryPrice) / entryPrice
        val signedChange = if (side == PositionSide.LONG) change else -change
        return notionalUsd * signedChange
    }

    /** True once losses have wiped out the full margin -- the exchange would force-close here. */
    fun isLiquidated(currentPrice: Double): Boolean = currentPnlUsd(currentPrice) <= -marginUsd

    /** What the user gets back if they close now: margin + P&L, floored at 0. */
    fun currentValueUsd(currentPrice: Double): Double =
        (marginUsd + currentPnlUsd(currentPrice)).coerceAtLeast(0.0)

    fun pnlPercentOfMargin(currentPrice: Double): Double =
        if (marginUsd <= 0.0) 0.0 else (currentPnlUsd(currentPrice) / marginUsd) * 100.0
}

data class LifeStats(
    val health: Int = 100,
    val hunger: Int = 100,
    val energy: Int = 100
) {
    fun clamp() = copy(
        health = health.coerceIn(0, 100),
        hunger = hunger.coerceIn(0, 100),
        energy = energy.coerceIn(0, 100)
    )
}

data class UserProfile(
    val firstName: String,
    val lastName: String,
    val level: Int = 1,
    val xp: Int = 0,
    val randomSeed: Long,
    val accountCreatedAtMillis: Long
)
