package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.GameType
import com.example.model.MissionCategory
import com.example.model.MissionMetric
import com.example.model.MissionProgressState
import com.example.model.PlayerProfile
import com.example.model.RoyaleMission
import com.example.model.RoyaleMissionCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MissionsSnapshotData(
    val dateKey: String,
    val dailyRoundsAny: Long,
    val dailyCardRounds: Long,
    val dailyWheelReelRounds: Long,
    val dailyWinsAny: Long,
    val careerWinsAny: Long,
    val gamesSampledCount: Long,
    val claimedIds: Set<String>
)

object MissionsEvaluator {

    fun currentDateKey(timeMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date(timeMillis))
    }

    fun buildStates(
        data: MissionsSnapshotData,
        profile: PlayerProfile
    ): List<MissionProgressState> {
        return RoyaleMissionCatalog.missions.map { mission ->
            val currentValue = when (mission.metric) {
                MissionMetric.DAILY_ROUNDS_ANY -> data.dailyRoundsAny
                MissionMetric.DAILY_CARD_ROUNDS -> data.dailyCardRounds
                MissionMetric.DAILY_WHEEL_REEL_ROUNDS -> data.dailyWheelReelRounds
                MissionMetric.DAILY_WINS_ANY -> data.dailyWinsAny
                MissionMetric.CAREER_WINS_ANY -> data.careerWinsAny
                MissionMetric.CAREER_BEST_STREAK -> profile.stats.bestWinStreak.toLong()
                MissionMetric.CAREER_TOTAL_WAGERED -> profile.stats.totalWagered
                MissionMetric.CAREER_GAMES_SAMPLED -> data.gamesSampledCount
                MissionMetric.CAREER_BIGGEST_WIN -> profile.stats.biggestWin
            }
            MissionProgressState(
                mission = mission,
                currentValue = currentValue,
                isClaimed = data.claimedIds.contains(mission.id)
            )
        }
    }
}

class MissionsRepository(
    context: Context
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _missionStates = MutableStateFlow<List<MissionProgressState>>(emptyList())
    val missionStates: StateFlow<List<MissionProgressState>> = _missionStates.asStateFlow()

    private val _claimableCount = MutableStateFlow(0)
    val claimableCount: StateFlow<Int> = _claimableCount.asStateFlow()

    @Synchronized
    fun syncWithProfile(profile: PlayerProfile, nowMillis: Long = System.currentTimeMillis()) {
        val data = loadNormalizedData(nowMillis)
        emitStates(data, profile)
    }

    @Synchronized
    fun recordRoundFinished(
        gameType: GameType,
        isWin: Boolean,
        profile: PlayerProfile,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val current = loadNormalizedData(nowMillis)
        val isCardGame = gameType == GameType.BLACKJACK ||
            gameType == GameType.VIDEO_POKER ||
            gameType == GameType.BACCARAT
        val isWheelOrReel = gameType == GameType.SLOTS || gameType == GameType.ROULETTE

        val sampledSet = (prefs.getStringSet(KEY_GAMES_SAMPLED, emptySet()) ?: emptySet()).toMutableSet()
        sampledSet.add(gameType.name)

        val updatedRounds = current.dailyRoundsAny + 1L
        val updatedCardRounds = current.dailyCardRounds + if (isCardGame) 1L else 0L
        val updatedWheelReelRounds = current.dailyWheelReelRounds + if (isWheelOrReel) 1L else 0L
        val updatedDailyWins = current.dailyWinsAny + if (isWin) 1L else 0L
        val updatedCareerWins = current.careerWinsAny + if (isWin) 1L else 0L

        prefs.edit()
            .putString(KEY_DAILY_DATE, current.dateKey)
            .putLong(KEY_DAILY_ROUNDS_ANY, updatedRounds)
            .putLong(KEY_DAILY_CARD_ROUNDS, updatedCardRounds)
            .putLong(KEY_DAILY_WHEEL_REEL_ROUNDS, updatedWheelReelRounds)
            .putLong(KEY_DAILY_WINS_ANY, updatedDailyWins)
            .putLong(KEY_CAREER_WINS_ANY, updatedCareerWins)
            .putStringSet(KEY_GAMES_SAMPLED, sampledSet)
            .apply()

        val updatedData = current.copy(
            dailyRoundsAny = updatedRounds,
            dailyCardRounds = updatedCardRounds,
            dailyWheelReelRounds = updatedWheelReelRounds,
            dailyWinsAny = updatedDailyWins,
            careerWinsAny = updatedCareerWins,
            gamesSampledCount = sampledSet.size.toLong()
        )
        emitStates(updatedData, profile)
    }

    @Synchronized
    fun claimMission(
        missionId: String,
        profile: PlayerProfile,
        nowMillis: Long = System.currentTimeMillis()
    ): RoyaleMission? {
        val data = loadNormalizedData(nowMillis)
        val states = MissionsEvaluator.buildStates(data, profile)
        val targetState = states.firstOrNull { it.mission.id == missionId } ?: return null
        if (!targetState.isClaimable) return null

        if (targetState.mission.category == MissionCategory.DAILY) {
            val dailyClaimed = (prefs.getStringSet(KEY_DAILY_CLAIMED, emptySet()) ?: emptySet()).toMutableSet()
            dailyClaimed.add(missionId)
            prefs.edit().putStringSet(KEY_DAILY_CLAIMED, dailyClaimed).apply()
        } else {
            val careerClaimed = (prefs.getStringSet(KEY_CAREER_CLAIMED, emptySet()) ?: emptySet()).toMutableSet()
            careerClaimed.add(missionId)
            prefs.edit().putStringSet(KEY_CAREER_CLAIMED, careerClaimed).apply()
        }

        val refreshedData = loadNormalizedData(nowMillis)
        emitStates(refreshedData, profile)
        return targetState.mission
    }

    private fun loadNormalizedData(nowMillis: Long): MissionsSnapshotData {
        val todayKey = MissionsEvaluator.currentDateKey(nowMillis)
        val savedKey = prefs.getString(KEY_DAILY_DATE, null)
        if (savedKey != todayKey) {
            prefs.edit()
                .putString(KEY_DAILY_DATE, todayKey)
                .putLong(KEY_DAILY_ROUNDS_ANY, 0L)
                .putLong(KEY_DAILY_CARD_ROUNDS, 0L)
                .putLong(KEY_DAILY_WHEEL_REEL_ROUNDS, 0L)
                .putLong(KEY_DAILY_WINS_ANY, 0L)
                .putStringSet(KEY_DAILY_CLAIMED, emptySet())
                .apply()
        }

        val dailyClaimed = prefs.getStringSet(KEY_DAILY_CLAIMED, emptySet()) ?: emptySet()
        val careerClaimed = prefs.getStringSet(KEY_CAREER_CLAIMED, emptySet()) ?: emptySet()
        val sampledSet = prefs.getStringSet(KEY_GAMES_SAMPLED, emptySet()) ?: emptySet()

        return MissionsSnapshotData(
            dateKey = todayKey,
            dailyRoundsAny = prefs.getLong(KEY_DAILY_ROUNDS_ANY, 0L),
            dailyCardRounds = prefs.getLong(KEY_DAILY_CARD_ROUNDS, 0L),
            dailyWheelReelRounds = prefs.getLong(KEY_DAILY_WHEEL_REEL_ROUNDS, 0L),
            dailyWinsAny = prefs.getLong(KEY_DAILY_WINS_ANY, 0L),
            careerWinsAny = prefs.getLong(KEY_CAREER_WINS_ANY, 0L),
            gamesSampledCount = sampledSet.size.toLong(),
            claimedIds = dailyClaimed + careerClaimed
        )
    }

    private fun emitStates(data: MissionsSnapshotData, profile: PlayerProfile) {
        val states = MissionsEvaluator.buildStates(data, profile)
        _missionStates.value = states
        _claimableCount.value = states.count { it.isClaimable }
    }

    companion object {
        private const val PREFS_NAME = "fire_kern_missions_prefs"
        private const val KEY_DAILY_DATE = "daily_date_key"
        private const val KEY_DAILY_ROUNDS_ANY = "daily_rounds_any"
        private const val KEY_DAILY_CARD_ROUNDS = "daily_card_rounds"
        private const val KEY_DAILY_WHEEL_REEL_ROUNDS = "daily_wheel_reel_rounds"
        private const val KEY_DAILY_WINS_ANY = "daily_wins_any"
        private const val KEY_DAILY_CLAIMED = "daily_claimed_ids"
        private const val KEY_CAREER_WINS_ANY = "career_wins_any"
        private const val KEY_CAREER_CLAIMED = "career_claimed_ids"
        private const val KEY_GAMES_SAMPLED = "career_games_sampled"
    }
}
