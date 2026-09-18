package ir.amir.triedgame.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.amir.triedgame.data.GameRepository
import ir.amir.triedgame.engine.PriceEngine
import ir.amir.triedgame.model.Asset
import ir.amir.triedgame.model.AssetCatalog
import ir.amir.triedgame.model.Candle
import ir.amir.triedgame.model.ChallengeRecord
import ir.amir.triedgame.model.DailyChallenge
import ir.amir.triedgame.model.DailyChallengeCatalog
import ir.amir.triedgame.model.LifeStats
import ir.amir.triedgame.model.Position
import ir.amir.triedgame.model.PositionSide
import ir.amir.triedgame.model.UserProfile
import ir.amir.triedgame.model.Wallet
import ir.amir.triedgame.model.xpToReachLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

/** UI-ready snapshot of a daily challenge: definition + today's live progress. */
data class ChallengeUiState(
    val challenge: DailyChallenge,
    val progress: Int,
    val isComplete: Boolean,
    val isClaimed: Boolean
)

/** Candle timeframe options the user can pick on the trading screen. */
enum class Timeframe(val millis: Long, val label: String) {
    M1(PriceEngine.MINUTE_MS, "۱ دقیقه"),
    M5(5 * PriceEngine.MINUTE_MS, "۵ دقیقه"),
    H1(PriceEngine.HOUR_MS, "۱ ساعت"),
    H24(PriceEngine.DAY_MS, "۲۴ ساعت")
}

class GameViewModel(private val repository: GameRepository) : ViewModel() {

    var profile by mutableStateOf<UserProfile?>(null)
        private set

    var wallet by mutableStateOf(Wallet())
        private set

    var lifeStats by mutableStateOf(LifeStats())
        private set

    var positions by mutableStateOf<List<Position>>(emptyList())
        private set

    var selectedAsset by mutableStateOf(AssetCatalog.all.first())
        private set

    var timeframe by mutableStateOf(Timeframe.M1)
        private set

    var candles by mutableStateOf<List<Candle>>(emptyList())
        private set

    /** Live current price per asset symbol, updated every tick. */
    var currentPrices by mutableStateOf<Map<String, Double>>(emptyMap())
        private set

    var challenges by mutableStateOf<List<ChallengeUiState>>(emptyList())
        private set

    /** Set briefly when the user levels up, so the UI can show a small celebration. */
    var levelUpEvent by mutableStateOf<Int?>(null)
        private set

    private var tickerStarted = false
    private var challengeRecord = ChallengeRecord()
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun todayKey() = dayFormat.format(Date())

    fun loadOrCreateProfile(existing: UserProfile?) {
        if (existing != null) {
            profile = existing
            wallet = repository.loadWallet()
            lifeStats = repository.loadLifeStats()
            positions = repository.loadPositions()
            loadChallenges()
            resolveOfflineGapAndStartTicking()
        }
    }

    fun registerNewUser(firstName: String, lastName: String) {
        val created = repository.createProfile(firstName, lastName)
        profile = created
        wallet = Wallet()
        lifeStats = LifeStats()
        positions = emptyList()
        loadChallenges()
        resolveOfflineGapAndStartTicking()
    }

    fun selectAsset(asset: Asset) {
        selectedAsset = asset
        rebuildChartFor(asset)
    }

    fun selectTimeframe(tf: Timeframe) {
        timeframe = tf
        rebuildChartFor(selectedAsset)
    }

    fun clearLevelUpEvent() {
        levelUpEvent = null
    }

    private fun referencePriceFor(asset: Asset) = asset.startingPriceUsd

    private fun rebuildChartFor(asset: Asset) {
        val p = profile ?: return
        val now = System.currentTimeMillis()
        // Show ~150 candles at the chosen timeframe.
        val from = now - timeframe.millis * 150
        candles = PriceEngine.generateCandles(
            asset = asset,
            seed = p.randomSeed,
            fromMillis = from,
            toMillis = now,
            intervalMillis = timeframe.millis,
            referenceTimestampMillis = p.accountCreatedAtMillis,
            referencePrice = referencePriceFor(asset)
        )
    }

    /**
     * Called once when a profile becomes available (fresh registration or app
     * resume). Backfills every asset's price up to "now" from the last time we
     * ticked, so positions the user held while the app was closed reflect the
     * time that actually passed -- then starts the live per-second ticker.
     */
    private fun resolveOfflineGapAndStartTicking() {
        val p = profile ?: return
        val now = System.currentTimeMillis()

        val prices = mutableMapOf<String, Double>()
        AssetCatalog.all.forEach { asset ->
            prices[asset.symbol] = PriceEngine.priceAt(
                asset, p.randomSeed, now, p.accountCreatedAtMillis, referencePriceFor(asset)
            )
        }
        currentPrices = prices
        rebuildChartFor(selectedAsset)
        repository.saveLastSeen(now)

        if (!tickerStarted) {
            tickerStarted = true
            startTicker()
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(3000)
                tickOnce()
            }
        }
    }

    private fun tickOnce() {
        val p = profile ?: return
        val now = System.currentTimeMillis()
        val prices = mutableMapOf<String, Double>()
        AssetCatalog.all.forEach { asset ->
            prices[asset.symbol] = PriceEngine.priceAt(
                asset, p.randomSeed, now, p.accountCreatedAtMillis, referencePriceFor(asset)
            )
        }
        currentPrices = prices
        rebuildChartFor(selectedAsset)
        repository.saveLastSeen(now)
        applyLifeDrain()
        checkLiquidations()
        refreshChallengeUiState()
    }

    private fun applyLifeDrain() {
        var stats = lifeStats
        // Small, steady drain while the trading screen is active; food/rest
        // in the "زندگی من" section restores these.
        stats = stats.copy(hunger = stats.hunger - 1, energy = stats.energy - 1)
        if (stats.hunger <= 0) {
            stats = stats.copy(health = stats.health - 2)
        }
        stats = stats.clamp()
        lifeStats = stats
        repository.saveLifeStats(stats)
    }

    private fun checkLiquidations() {
        val toLiquidate = positions.filter { pos ->
            val price = currentPrices[pos.assetSymbol] ?: return@filter false
            pos.isLiquidated(price)
        }
        if (toLiquidate.isEmpty()) return
        positions = positions.filterNot { p -> toLiquidate.any { it.id == p.id } }
        repository.savePositions(positions)
        // Margin is lost entirely on liquidation -- nothing is returned to the wallet.
    }

    // --- Trading ---

    fun openPosition(side: PositionSide, marginUsd: Double, leverage: Int): Boolean {
        if (marginUsd <= 0.0) return false
        val price = currentPrices[selectedAsset.symbol] ?: return false
        val w = wallet.spendUsdForMargin(marginUsd) ?: return false
        wallet = w
        repository.saveWallet(wallet)
        val position = Position(
            id = UUID.randomUUID().toString(),
            assetSymbol = selectedAsset.symbol,
            side = side,
            entryPrice = price,
            marginUsd = marginUsd,
            leverage = leverage,
            openedAtMillis = System.currentTimeMillis()
        )
        positions = positions + position
        repository.savePositions(positions)

        challengeRecord = challengeRecord.copy(
            tradesOpened = challengeRecord.tradesOpened + 1,
            fictionalTraded = challengeRecord.fictionalTraded ||
                selectedAsset.category == ir.amir.triedgame.model.AssetCategory.FICTIONAL
        )
        saveChallengeRecord()
        refreshChallengeUiState()
        return true
    }

    fun closePosition(position: Position) {
        val price = currentPrices[position.assetSymbol] ?: return
        val pnl = position.currentPnlUsd(price)
        val payout = position.currentValueUsd(price)
        wallet = wallet.addUsd(payout)
        repository.saveWallet(wallet)
        positions = positions.filterNot { it.id == position.id }
        repository.savePositions(positions)

        if (pnl > 0) {
            challengeRecord = challengeRecord.copy(profitableCloses = challengeRecord.profitableCloses + 1)
            saveChallengeRecord()
        }
        gainXp((pnl.coerceAtLeast(0.0) / 5.0).roundToInt().coerceIn(0, 200))
        refreshChallengeUiState()
    }

    // --- Wallet ---

    fun creditUsd(amount: Double) {
        wallet = wallet.addUsd(amount)
        repository.saveWallet(wallet)
    }

    fun creditToman(amount: Double) {
        wallet = wallet.addToman(amount)
        repository.saveWallet(wallet)
    }

    fun convertUsdToToman(amount: Double) {
        wallet = wallet.convertUsdToToman(amount)
        repository.saveWallet(wallet)
    }

    fun convertTomanToUsd(amount: Double) {
        wallet = wallet.convertTomanToUsd(amount)
        repository.saveWallet(wallet)
    }

    // --- Life / زندگی من ---

    fun spendTomanInLife(amount: Double, xpReward: Int, isHungerItem: Boolean, applyEffect: (LifeStats) -> LifeStats): Boolean {
        val newWallet = wallet.spendToman(amount) ?: return false
        wallet = newWallet
        repository.saveWallet(wallet)
        lifeStats = applyEffect(lifeStats).clamp()
        repository.saveLifeStats(lifeStats)
        gainXp(xpReward)

        if (isHungerItem) {
            challengeRecord = challengeRecord.copy(hungerItemBought = true)
            saveChallengeRecord()
            refreshChallengeUiState()
        }
        return true
    }

    // --- Level / XP ---

    private fun gainXp(amount: Int) {
        if (amount <= 0) return
        val p = profile ?: return
        var newXp = p.xp + amount
        var newLevel = p.level
        while (newXp >= xpToReachLevel(newLevel + 1)) {
            newLevel += 1
        }
        val leveledUp = newLevel > p.level
        val updated = p.copy(xp = newXp, level = newLevel)
        profile = updated
        repository.saveProfile(updated)
        if (leveledUp) levelUpEvent = newLevel
    }

    // --- Daily challenges ---

    private fun loadChallenges() {
        val (record, day) = repository.loadChallengeRecord()
        challengeRecord = if (day == todayKey()) record else ChallengeRecord()
        if (day != todayKey()) saveChallengeRecord()
        refreshChallengeUiState()
    }

    private fun saveChallengeRecord() {
        repository.saveChallengeRecord(challengeRecord, todayKey())
    }

    private fun refreshChallengeUiState() {
        challenges = DailyChallengeCatalog.today().map { challenge ->
            val progress = challenge.progressFrom(challengeRecord, lifeStats)
            ChallengeUiState(
                challenge = challenge,
                progress = progress.coerceAtMost(challenge.target),
                isComplete = progress >= challenge.target,
                isClaimed = challengeRecord.claimedIds.contains(challenge.id)
            )
        }
    }

    fun claimChallenge(challengeId: String) {
        val state = challenges.find { it.challenge.id == challengeId } ?: return
        if (!state.isComplete || state.isClaimed) return
        wallet = wallet.addToman(state.challenge.rewardToman)
        repository.saveWallet(wallet)
        challengeRecord = challengeRecord.copy(claimedIds = challengeRecord.claimedIds + challengeId)
        saveChallengeRecord()
        gainXp(30)
        refreshChallengeUiState()
    }
}

/** Wallet needs a margin-hold helper distinct from a plain spend, since the
 * amount is returned (plus/minus P&L) when the position closes rather than
 * being permanently gone. Kept here to avoid widening Wallet's own API. */
private fun Wallet.spendUsdForMargin(amount: Double): Wallet? =
    if (usdBalance >= amount) copy(usdBalance = usdBalance - amount) else null
