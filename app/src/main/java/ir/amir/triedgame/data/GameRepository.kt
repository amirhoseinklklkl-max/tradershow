package ir.amir.triedgame.data

import android.content.Context
import android.content.SharedPreferences
import ir.amir.triedgame.model.ChallengeRecord
import ir.amir.triedgame.model.LifeStats
import ir.amir.triedgame.model.Position
import ir.amir.triedgame.model.PositionSide
import ir.amir.triedgame.model.UserProfile
import ir.amir.triedgame.model.Wallet
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * Simple local persistence for the game's save data. SharedPreferences + JSON
 * is used deliberately instead of a database: the data set is small (one
 * profile, one wallet, a handful of open positions) and this keeps the
 * project dependency-light.
 */
class GameRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("tradershow_save", Context.MODE_PRIVATE)

    fun hasProfile(): Boolean = prefs.contains(KEY_FIRST_NAME)

    fun saveProfile(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_FIRST_NAME, profile.firstName)
            .putString(KEY_LAST_NAME, profile.lastName)
            .putInt(KEY_LEVEL, profile.level)
            .putInt(KEY_XP, profile.xp)
            .putLong(KEY_SEED, profile.randomSeed)
            .putLong(KEY_CREATED_AT, profile.accountCreatedAtMillis)
            .apply()
    }

    fun loadProfile(): UserProfile? {
        if (!hasProfile()) return null
        return UserProfile(
            firstName = prefs.getString(KEY_FIRST_NAME, "") ?: "",
            lastName = prefs.getString(KEY_LAST_NAME, "") ?: "",
            level = prefs.getInt(KEY_LEVEL, 1),
            xp = prefs.getInt(KEY_XP, 0),
            randomSeed = prefs.getLong(KEY_SEED, Random.nextLong()),
            accountCreatedAtMillis = prefs.getLong(KEY_CREATED_AT, System.currentTimeMillis())
        )
    }

    fun createProfile(firstName: String, lastName: String): UserProfile {
        val now = System.currentTimeMillis()
        val profile = UserProfile(
            firstName = firstName,
            lastName = lastName,
            level = 1,
            xp = 0,
            randomSeed = Random.nextLong(),
            accountCreatedAtMillis = now
        )
        saveProfile(profile)
        saveLastSeen(now)
        saveWallet(Wallet())
        saveLifeStats(LifeStats())
        return profile
    }

    fun saveWallet(wallet: Wallet) {
        prefs.edit()
            .putFloat(KEY_WALLET_USD, wallet.usdBalance.toFloat())
            .putFloat(KEY_WALLET_TOMAN, wallet.tomanBalance.toFloat())
            .apply()
    }

    fun loadWallet(): Wallet = Wallet(
        usdBalance = prefs.getFloat(KEY_WALLET_USD, 0f).toDouble(),
        tomanBalance = prefs.getFloat(KEY_WALLET_TOMAN, 0f).toDouble()
    )

    fun saveLifeStats(stats: LifeStats) {
        prefs.edit()
            .putInt(KEY_HEALTH, stats.health)
            .putInt(KEY_HUNGER, stats.hunger)
            .putInt(KEY_ENERGY, stats.energy)
            .apply()
    }

    fun loadLifeStats(): LifeStats = LifeStats(
        health = prefs.getInt(KEY_HEALTH, 100),
        hunger = prefs.getInt(KEY_HUNGER, 100),
        energy = prefs.getInt(KEY_ENERGY, 100)
    )

    /** Timestamp of the last moment the game engine was ticked (used to compute offline gaps). */
    fun saveLastSeen(timestampMillis: Long) {
        prefs.edit().putLong(KEY_LAST_SEEN, timestampMillis).apply()
    }

    fun loadLastSeen(): Long = prefs.getLong(KEY_LAST_SEEN, System.currentTimeMillis())

    fun saveDailyAdCount(count: Int, dayKey: String) {
        prefs.edit()
            .putInt(KEY_AD_COUNT, count)
            .putString(KEY_AD_DAY, dayKey)
            .apply()
    }

    fun loadDailyAdCount(dayKey: String): Int =
        if (prefs.getString(KEY_AD_DAY, null) == dayKey) prefs.getInt(KEY_AD_COUNT, 0) else 0

    fun saveLastAdTimestamp(timestampMillis: Long) {
        prefs.edit().putLong(KEY_LAST_AD_TIME, timestampMillis).apply()
    }

    fun loadLastAdTimestamp(): Long = prefs.getLong(KEY_LAST_AD_TIME, 0L)

    fun savePositions(positions: List<Position>) {
        val array = JSONArray()
        positions.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("symbol", p.assetSymbol)
            obj.put("side", p.side.name)
            obj.put("entryPrice", p.entryPrice)
            obj.put("marginUsd", p.marginUsd)
            obj.put("leverage", p.leverage)
            obj.put("openedAt", p.openedAtMillis)
            array.put(obj)
        }
        prefs.edit().putString(KEY_POSITIONS, array.toString()).apply()
    }

    fun loadPositions(): List<Position> {
        val raw = prefs.getString(KEY_POSITIONS, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Position(
                id = obj.getString("id"),
                assetSymbol = obj.getString("symbol"),
                side = PositionSide.valueOf(obj.getString("side")),
                entryPrice = obj.getDouble("entryPrice"),
                marginUsd = if (obj.has("marginUsd")) obj.getDouble("marginUsd") else obj.optDouble("amountUsd", 0.0),
                leverage = if (obj.has("leverage")) obj.getInt("leverage") else 1,
                openedAtMillis = obj.getLong("openedAt")
            )
        }
    }

    fun saveChallengeRecord(record: ChallengeRecord, dayKey: String) {
        prefs.edit()
            .putInt(KEY_CH_TRADES, record.tradesOpened)
            .putInt(KEY_CH_PROFITABLE, record.profitableCloses)
            .putBoolean(KEY_CH_FICTIONAL, record.fictionalTraded)
            .putBoolean(KEY_CH_HUNGER, record.hungerItemBought)
            .putStringSet(KEY_CH_CLAIMED, record.claimedIds)
            .putString(KEY_CH_DAY, dayKey)
            .apply()
    }

    /** Returns the saved record plus the day it was recorded for (caller decides whether it's stale). */
    fun loadChallengeRecord(): Pair<ChallengeRecord, String> {
        val record = ChallengeRecord(
            tradesOpened = prefs.getInt(KEY_CH_TRADES, 0),
            profitableCloses = prefs.getInt(KEY_CH_PROFITABLE, 0),
            fictionalTraded = prefs.getBoolean(KEY_CH_FICTIONAL, false),
            hungerItemBought = prefs.getBoolean(KEY_CH_HUNGER, false),
            claimedIds = prefs.getStringSet(KEY_CH_CLAIMED, emptySet()) ?: emptySet()
        )
        val day = prefs.getString(KEY_CH_DAY, "") ?: ""
        return record to day
    }

    companion object {
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_LEVEL = "level"
        private const val KEY_XP = "xp"
        private const val KEY_SEED = "seed"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_WALLET_USD = "wallet_usd"
        private const val KEY_WALLET_TOMAN = "wallet_toman"
        private const val KEY_HEALTH = "health"
        private const val KEY_HUNGER = "hunger"
        private const val KEY_ENERGY = "energy"
        private const val KEY_LAST_SEEN = "last_seen"
        private const val KEY_AD_COUNT = "ad_count_today"
        private const val KEY_AD_DAY = "ad_count_day"
        private const val KEY_LAST_AD_TIME = "last_ad_time"
        private const val KEY_POSITIONS = "positions"
        private const val KEY_CH_TRADES = "ch_trades"
        private const val KEY_CH_PROFITABLE = "ch_profitable"
        private const val KEY_CH_FICTIONAL = "ch_fictional"
        private const val KEY_CH_HUNGER = "ch_hunger"
        private const val KEY_CH_CLAIMED = "ch_claimed"
        private const val KEY_CH_DAY = "ch_day"
    }
}
