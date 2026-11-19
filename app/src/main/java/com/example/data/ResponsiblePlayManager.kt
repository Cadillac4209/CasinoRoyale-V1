package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SpendingLimitAlertLevel {
    NONE,
    APPROACHING_THRESHOLD,
    LIMIT_REACHED
}

data class MonthlySpendingLimitState(
    val isEnabled: Boolean = true,
    val monthlyLimit: Long = MonthlySpendingLimitEngine.DEFAULT_MONTHLY_LIMIT,
    val monthlySpent: Long = 0L,
    val monthKey: String = MonthlySpendingLimitEngine.currentMonthKey(),
    val warningThresholdPercent: Int = MonthlySpendingLimitEngine.DEFAULT_WARNING_THRESHOLD_PERCENT,
    val pendingAlert: SpendingLimitAlertLevel = SpendingLimitAlertLevel.NONE,
    val acknowledgedApproachingMonth: Boolean = false,
    val acknowledgedLimitReachedMonth: Boolean = false
) {
    val usageFraction: Float
        get() = if (monthlyLimit <= 0L) 0f
        else (monthlySpent.toFloat() / monthlyLimit.toFloat()).coerceIn(0f, 1f)

    val usagePercent: Int
        get() = if (monthlyLimit <= 0L) 0
        else ((monthlySpent * 100L) / monthlyLimit).toInt().coerceAtLeast(0)

    val remainingBeforeLimit: Long
        get() = (monthlyLimit - monthlySpent).coerceAtLeast(0L)

    val warningThresholdAmount: Long
        get() = (monthlyLimit * warningThresholdPercent.toLong()) / 100L

    val isApproachingOrOver: Boolean
        get() = isEnabled && monthlyLimit > 0L && (monthlySpent * 100L) >= (monthlyLimit * warningThresholdPercent.toLong())

    val isOverLimit: Boolean
        get() = isEnabled && monthlyLimit > 0L && monthlySpent >= monthlyLimit
}

/**
 * Pure deterministic engine for tracking monthly virtual credit wagers against a
 * user-defined monthly spending limit and triggering confirmation alerts when
 * approaching (80%+) or reaching (100%+) the threshold.
 */
class MonthlySpendingLimitEngine(
    initialEnabled: Boolean = true,
    initialMonthlyLimit: Long = DEFAULT_MONTHLY_LIMIT,
    initialMonthlySpent: Long = 0L,
    initialMonthKey: String = currentMonthKey(),
    initialAcknowledgedApproaching: Boolean = false,
    initialAcknowledgedLimitReached: Boolean = false,
    val warningThresholdPercent: Int = DEFAULT_WARNING_THRESHOLD_PERCENT
) {
    var state: MonthlySpendingLimitState = MonthlySpendingLimitState(
        isEnabled = initialEnabled,
        monthlyLimit = initialMonthlyLimit.coerceIn(MIN_MONTHLY_LIMIT, MAX_MONTHLY_LIMIT),
        monthlySpent = initialMonthlySpent.coerceAtLeast(0L),
        monthKey = initialMonthKey,
        warningThresholdPercent = warningThresholdPercent,
        pendingAlert = SpendingLimitAlertLevel.NONE,
        acknowledgedApproachingMonth = initialAcknowledgedApproaching,
        acknowledgedLimitReachedMonth = initialAcknowledgedLimitReached
    )
        private set

    fun syncMonth(monthKey: String = currentMonthKey()): Boolean {
        if (monthKey == state.monthKey) return false
        state = state.copy(
            monthlySpent = 0L,
            monthKey = monthKey,
            pendingAlert = SpendingLimitAlertLevel.NONE,
            acknowledgedApproachingMonth = false,
            acknowledgedLimitReachedMonth = false
        )
        return true
    }

    fun configureLimit(
        limitAmount: Long,
        enabled: Boolean,
        monthKey: String = currentMonthKey()
    ): MonthlySpendingLimitState {
        syncMonth(monthKey)
        val clampedLimit = limitAmount.coerceIn(MIN_MONTHLY_LIMIT, MAX_MONTHLY_LIMIT)
        val isTightenedLimit = clampedLimit < state.monthlyLimit

        val isAtOrOver100 = state.monthlySpent >= clampedLimit
        val isAtOrOver80 = (state.monthlySpent * 100L) >= (clampedLimit * warningThresholdPercent.toLong())

        val nextAckApproaching = when {
            !isAtOrOver80 -> false
            isTightenedLimit -> false
            else -> state.acknowledgedApproachingMonth
        }
        val nextAckLimitReached = when {
            !isAtOrOver100 -> false
            isTightenedLimit -> false
            else -> state.acknowledgedLimitReachedMonth
        }

        val nextPending = if (!enabled) {
            SpendingLimitAlertLevel.NONE
        } else {
            resolvePendingAlert(
                monthlySpent = state.monthlySpent,
                monthlyLimit = clampedLimit,
                ackApproaching = nextAckApproaching,
                ackLimitReached = nextAckLimitReached
            )
        }

        state = state.copy(
            isEnabled = enabled,
            monthlyLimit = clampedLimit,
            pendingAlert = nextPending,
            acknowledgedApproachingMonth = nextAckApproaching,
            acknowledgedLimitReachedMonth = nextAckLimitReached
        )
        return state
    }

    fun recordWager(
        wagerAmount: Long,
        monthKey: String = currentMonthKey()
    ): SpendingLimitAlertLevel {
        syncMonth(monthKey)
        if (wagerAmount <= 0L) return state.pendingAlert

        val nextSpent = (state.monthlySpent + wagerAmount).coerceAtLeast(0L)
        val nextAlert = if (!state.isEnabled) {
            SpendingLimitAlertLevel.NONE
        } else {
            resolvePendingAlert(
                monthlySpent = nextSpent,
                monthlyLimit = state.monthlyLimit,
                ackApproaching = state.acknowledgedApproachingMonth,
                ackLimitReached = state.acknowledgedLimitReachedMonth
            )
        }

        state = state.copy(
            monthlySpent = nextSpent,
            pendingAlert = nextAlert
        )
        return nextAlert
    }

    fun acknowledgeAlert(monthKey: String = currentMonthKey()): MonthlySpendingLimitState {
        syncMonth(monthKey)
        val ackApproaching = state.acknowledgedApproachingMonth ||
            state.pendingAlert == SpendingLimitAlertLevel.APPROACHING_THRESHOLD ||
            state.pendingAlert == SpendingLimitAlertLevel.LIMIT_REACHED
        val ackLimitReached = state.acknowledgedLimitReachedMonth ||
            state.pendingAlert == SpendingLimitAlertLevel.LIMIT_REACHED

        state = state.copy(
            pendingAlert = SpendingLimitAlertLevel.NONE,
            acknowledgedApproachingMonth = ackApproaching,
            acknowledgedLimitReachedMonth = ackLimitReached
        )
        return state
    }

    private fun resolvePendingAlert(
        monthlySpent: Long,
        monthlyLimit: Long,
        ackApproaching: Boolean,
        ackLimitReached: Boolean
    ): SpendingLimitAlertLevel {
        if (monthlyLimit <= 0L) return SpendingLimitAlertLevel.NONE
        return when {
            monthlySpent >= monthlyLimit && !ackLimitReached -> SpendingLimitAlertLevel.LIMIT_REACHED
            (monthlySpent * 100L) >= (monthlyLimit * warningThresholdPercent.toLong()) && !ackApproaching ->
                SpendingLimitAlertLevel.APPROACHING_THRESHOLD
            else -> SpendingLimitAlertLevel.NONE
        }
    }

    companion object {
        const val DEFAULT_MONTHLY_LIMIT = 50_000L
        const val MIN_MONTHLY_LIMIT = 1_000L
        const val MAX_MONTHLY_LIMIT = 100_000_000L
        const val DEFAULT_WARNING_THRESHOLD_PERCENT = 80
        val PRESET_LIMITS = listOf(10_000L, 25_000L, 50_000L, 100_000L, 250_000L)

        fun currentMonthKey(nowMs: Long = System.currentTimeMillis()): String {
            return SimpleDateFormat("yyyy-MM", Locale.US).format(Date(nowMs))
        }
    }
}

data class ResponsiblePlayState(
    val isBreakReminderEnabled: Boolean = true,
    val continuousPlayMs: Long = 0L,
    val breakThresholdMs: Long = ContinuousPlayTimerEngine.DEFAULT_BREAK_THRESHOLD_MS,
    val isBreakPromptVisible: Boolean = false,
    val completedBreaksCount: Int = 0
) {
    val progressFraction: Float
        get() = if (breakThresholdMs <= 0L) 0f
        else (continuousPlayMs.toFloat() / breakThresholdMs.toFloat()).coerceIn(0f, 1f)

    val remainingMs: Long
        get() = (breakThresholdMs - continuousPlayMs).coerceAtLeast(0L)
}

/**
 * Pure deterministic engine for tracking continuous active gameplay and inactivity breaks.
 * - Accumulates [continuousPlayMs] while app is in foreground and user has interacted recently.
 * - If the user is inactive or backgrounded for >= [inactivityResetThresholdMs] (default 5 minutes),
 *   the continuous session resets cleanly because a meaningful break occurred.
 * - Once [continuousPlayMs] reaches [breakThresholdMs] (default 60 minutes), triggers the break prompt.
 */
class ContinuousPlayTimerEngine(
    val breakThresholdMs: Long = DEFAULT_BREAK_THRESHOLD_MS,
    val inactivityResetThresholdMs: Long = DEFAULT_INACTIVITY_RESET_MS,
    initialNowMs: Long = 0L
) {
    var continuousPlayMs: Long = 0L
        private set

    var isBreakPromptVisible: Boolean = false
        private set

    var isForeground: Boolean = true
        private set

    var lastActivityMs: Long = initialNowMs
        private set

    private var lastTickMs: Long = initialNowMs
    private var backgroundStartedMs: Long? = null

    fun setForeground(foreground: Boolean, nowMs: Long) {
        if (foreground == isForeground) return
        if (foreground) {
            val bgStart = backgroundStartedMs
            backgroundStartedMs = null
            if (bgStart != null && (nowMs - bgStart) >= inactivityResetThresholdMs) {
                resetSession(nowMs)
            } else {
                lastTickMs = nowMs
                lastActivityMs = nowMs
            }
            isForeground = true
        } else {
            tick(nowMs, isReminderEnabled = true)
            isForeground = false
            backgroundStartedMs = nowMs
        }
    }

    fun recordActivity(nowMs: Long) {
        if (isForeground && (nowMs - lastActivityMs) >= inactivityResetThresholdMs) {
            resetSession(nowMs)
        }
        lastActivityMs = nowMs
        if (lastTickMs == 0L || nowMs < lastTickMs) {
            lastTickMs = nowMs
        }
    }

    fun tick(nowMs: Long, isReminderEnabled: Boolean): Boolean {
        if (lastTickMs == 0L || nowMs < lastTickMs) {
            lastTickMs = nowMs
            return false
        }

        val deltaMs = (nowMs - lastTickMs).coerceAtLeast(0L)
        lastTickMs = nowMs

        if (!isForeground || isBreakPromptVisible) {
            return false
        }

        val idleDurationMs = (nowMs - lastActivityMs).coerceAtLeast(0L)
        if (idleDurationMs >= inactivityResetThresholdMs) {
            if (continuousPlayMs > 0L) {
                continuousPlayMs = 0L
            }
            return false
        }

        if (!isReminderEnabled) {
            return false
        }

        // Cap single tick delta to avoid clock jumps falsely spiking a single frame
        val boundedDelta = deltaMs.coerceAtMost(MAX_TICK_DELTA_MS)
        continuousPlayMs = (continuousPlayMs + boundedDelta).coerceAtMost(breakThresholdMs)

        if (continuousPlayMs >= breakThresholdMs) {
            isBreakPromptVisible = true
            return true
        }
        return false
    }

    fun showBreakPromptNow() {
        isBreakPromptVisible = true
    }

    fun dismissAndReset(nowMs: Long) {
        isBreakPromptVisible = false
        resetSession(nowMs)
    }

    fun resetSession(nowMs: Long) {
        continuousPlayMs = 0L
        lastActivityMs = nowMs
        lastTickMs = nowMs
    }

    companion object {
        const val DEFAULT_BREAK_THRESHOLD_MS = 60L * 60L * 1000L // 60 minutes
        const val DEFAULT_INACTIVITY_RESET_MS = 5L * 60L * 1000L // 5 minutes idle/away resets continuous timer
        const val MAX_TICK_DELTA_MS = 15_000L
    }
}

class ResponsiblePlayManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val engine = ContinuousPlayTimerEngine(
        initialNowMs = System.currentTimeMillis()
    )

    private val spendingEngine: MonthlySpendingLimitEngine

    private val _state = MutableStateFlow(
        ResponsiblePlayState(
            isBreakReminderEnabled = prefs.getBoolean(KEY_BREAK_REMINDER_ENABLED, true),
            continuousPlayMs = 0L,
            breakThresholdMs = engine.breakThresholdMs,
            isBreakPromptVisible = false,
            completedBreaksCount = prefs.getInt(KEY_COMPLETED_BREAKS_COUNT, 0)
        )
    )
    val state: StateFlow<ResponsiblePlayState> = _state.asStateFlow()

    private val _spendingLimitState: MutableStateFlow<MonthlySpendingLimitState>
    val spendingLimitState: StateFlow<MonthlySpendingLimitState>

    init {
        val currentMonth = MonthlySpendingLimitEngine.currentMonthKey()
        val savedMonth = prefs.getString(KEY_MONTHLY_SPEND_MONTH_KEY, currentMonth) ?: currentMonth
        spendingEngine = MonthlySpendingLimitEngine(
            initialEnabled = prefs.getBoolean(KEY_MONTHLY_LIMIT_ENABLED, true),
            initialMonthlyLimit = prefs.getLong(
                KEY_MONTHLY_LIMIT_AMOUNT,
                MonthlySpendingLimitEngine.DEFAULT_MONTHLY_LIMIT
            ),
            initialMonthlySpent = prefs.getLong(KEY_MONTHLY_SPENT_AMOUNT, 0L),
            initialMonthKey = savedMonth,
            initialAcknowledgedApproaching = prefs.getBoolean(KEY_MONTHLY_ACK_80, false),
            initialAcknowledgedLimitReached = prefs.getBoolean(KEY_MONTHLY_ACK_100, false)
        )
        if (spendingEngine.syncMonth(currentMonth)) {
            persistSpendingState(spendingEngine.state)
        }
        _spendingLimitState = MutableStateFlow(spendingEngine.state)
        spendingLimitState = _spendingLimitState.asStateFlow()
    }

    fun setBreakReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BREAK_REMINDER_ENABLED, enabled).apply()
        if (!enabled && _state.value.isBreakPromptVisible) {
            engine.dismissAndReset(System.currentTimeMillis())
        }
        publishState()
    }

    fun onAppForeground(nowMs: Long = System.currentTimeMillis()) {
        engine.setForeground(true, nowMs)
        publishState()
        val monthKey = MonthlySpendingLimitEngine.currentMonthKey(nowMs)
        if (spendingEngine.syncMonth(monthKey)) {
            publishSpendingState()
        }
    }

    fun onAppBackground(nowMs: Long = System.currentTimeMillis()) {
        engine.setForeground(false, nowMs)
        publishState()
    }

    fun recordUserActivity(nowMs: Long = System.currentTimeMillis()) {
        engine.recordActivity(nowMs)
    }

    fun tick(nowMs: Long = System.currentTimeMillis()) {
        engine.tick(nowMs, isReminderEnabled = _state.value.isBreakReminderEnabled)
        publishState()
    }

    fun openSupportResourcesDialog() {
        engine.showBreakPromptNow()
        publishState()
    }

    fun takeBreakAndReset(nowMs: Long = System.currentTimeMillis()) {
        val nextBreakCount = _state.value.completedBreaksCount + 1
        prefs.edit().putInt(KEY_COMPLETED_BREAKS_COUNT, nextBreakCount).apply()
        engine.dismissAndReset(nowMs)
        _state.value = ResponsiblePlayState(
            isBreakReminderEnabled = _state.value.isBreakReminderEnabled,
            continuousPlayMs = engine.continuousPlayMs,
            breakThresholdMs = engine.breakThresholdMs,
            isBreakPromptVisible = false,
            completedBreaksCount = nextBreakCount
        )
    }

    fun dismissAndContinue(nowMs: Long = System.currentTimeMillis()) {
        engine.dismissAndReset(nowMs)
        publishState()
    }

    fun resetSessionTimer(nowMs: Long = System.currentTimeMillis()) {
        engine.resetSession(nowMs)
        publishState()
    }

    fun setMonthlyLimitEnabled(enabled: Boolean, nowMs: Long = System.currentTimeMillis()) {
        spendingEngine.configureLimit(
            limitAmount = spendingEngine.state.monthlyLimit,
            enabled = enabled,
            monthKey = MonthlySpendingLimitEngine.currentMonthKey(nowMs)
        )
        publishSpendingState()
    }

    fun setMonthlyLimit(
        limitAmount: Long,
        enabled: Boolean = spendingEngine.state.isEnabled,
        nowMs: Long = System.currentTimeMillis()
    ) {
        spendingEngine.configureLimit(
            limitAmount = limitAmount,
            enabled = enabled,
            monthKey = MonthlySpendingLimitEngine.currentMonthKey(nowMs)
        )
        publishSpendingState()
    }

    fun recordMonthlySpend(wagerAmount: Long, nowMs: Long = System.currentTimeMillis()) {
        if (wagerAmount <= 0L) return
        spendingEngine.recordWager(
            wagerAmount = wagerAmount,
            monthKey = MonthlySpendingLimitEngine.currentMonthKey(nowMs)
        )
        publishSpendingState()
    }

    fun acknowledgeSpendingLimitAlert(nowMs: Long = System.currentTimeMillis()) {
        spendingEngine.acknowledgeAlert(MonthlySpendingLimitEngine.currentMonthKey(nowMs))
        publishSpendingState()
    }

    private fun publishState() {
        _state.value = _state.value.copy(
            continuousPlayMs = engine.continuousPlayMs,
            isBreakPromptVisible = engine.isBreakPromptVisible
        )
    }

    private fun publishSpendingState() {
        val updated = spendingEngine.state
        persistSpendingState(updated)
        _spendingLimitState.value = updated
    }

    private fun persistSpendingState(state: MonthlySpendingLimitState) {
        prefs.edit().apply {
            putBoolean(KEY_MONTHLY_LIMIT_ENABLED, state.isEnabled)
            putLong(KEY_MONTHLY_LIMIT_AMOUNT, state.monthlyLimit)
            putLong(KEY_MONTHLY_SPENT_AMOUNT, state.monthlySpent)
            putString(KEY_MONTHLY_SPEND_MONTH_KEY, state.monthKey)
            putBoolean(KEY_MONTHLY_ACK_80, state.acknowledgedApproachingMonth)
            putBoolean(KEY_MONTHLY_ACK_100, state.acknowledgedLimitReachedMonth)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "fire_kern_responsible_play_prefs"
        private const val KEY_BREAK_REMINDER_ENABLED = "break_reminder_enabled"
        private const val KEY_COMPLETED_BREAKS_COUNT = "completed_breaks_count"
        private const val KEY_MONTHLY_LIMIT_ENABLED = "monthly_limit_enabled"
        private const val KEY_MONTHLY_LIMIT_AMOUNT = "monthly_limit_amount"
        private const val KEY_MONTHLY_SPENT_AMOUNT = "monthly_spent_amount"
        private const val KEY_MONTHLY_SPEND_MONTH_KEY = "monthly_spend_month_key"
        private const val KEY_MONTHLY_ACK_80 = "monthly_limit_ack_80"
        private const val KEY_MONTHLY_ACK_100 = "monthly_limit_ack_100"

        fun formatDuration(ms: Long): String {
            val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            return String.format(Locale.US, "%02dm %02ds", minutes, seconds)
        }
    }
}
