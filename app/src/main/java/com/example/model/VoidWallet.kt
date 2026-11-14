package com.example.model

enum class VoidPowerTier(
    val title: String,
    val requiredStake: Long,
    val xpMultiplier: Float,
    val bonusMultiplier: Float,
    val perkDescription: String,
    val badgeColorHex: Long
) {
    NONE(
        "Standard Power",
        0L,
        1.0f,
        1.0f,
        "No active Void Power. Stake \$VOID to supercharge gameplay.",
        0xFF64748B
    ),
    QUANTUM(
        "Quantum Drive",
        5L,
        1.25f,
        1.15f,
        "+25% XP earnings & +15% Daily Wheel bonus.",
        0xFF06B6D4
    ),
    SUPERNOVA(
        "Supernova Surge",
        15L,
        1.50f,
        1.35f,
        "+50% XP earnings, +35% Daily Wheel bonus & Lucky Wild Slot surge.",
        0xFF8B5CF6
    ),
    SINGULARITY(
        "Void Singularity",
        30L,
        2.00f,
        1.60f,
        "2X XP Double Overdrive, +60% Daily Wheel bonus & boosted VIP luck.",
        0xFFF43F5E
    );

    companion object {
        fun tierForStake(amount: Long): VoidPowerTier = when {
            amount >= SINGULARITY.requiredStake -> SINGULARITY
            amount >= SUPERNOVA.requiredStake -> SUPERNOVA
            amount >= QUANTUM.requiredStake -> QUANTUM
            else -> NONE
        }
    }
}

enum class VoidTxType(val displayName: String, val isCredit: Boolean) {
    MINT_REWARD("Gameplay Reward", true),
    SWAP_CHIPS_TO_VOID("Forge Chips to \$VOID", true),
    SWAP_VOID_TO_CHIPS("Convert \$VOID to Chips", false),
    STAKE_POWER("Staked for Void Power", false),
    UNSTAKE("Unstaked \$VOID", true)
}

data class VoidTransaction(
    val id: String,
    val type: VoidTxType,
    val amount: Long,
    val txHash: String,
    val timestamp: Long,
    val note: String
)

data class VoidWallet(
    val solanaAddress: String = "Vo1d8K9xP2mQ7nWsJ3vLtRtYhBnFzMcE",
    val balance: Long = 10L, // 10 initial starter $VOID tokens!
    val stakedAmount: Long = 0L,
    val transactions: List<VoidTransaction> = listOf(
        VoidTransaction(
            id = "genesis_tx",
            type = VoidTxType.MINT_REWARD,
            amount = 10L,
            txHash = "5K8pXwL9Yt2nQ7vR3mZ1aBcDeFgHiJkLm",
            timestamp = System.currentTimeMillis() - 3600000,
            note = "Genesis Void Airdrop: 10 \$VOID Welcoming Grant"
        )
    )
) {
    val totalVoid: Long get() = balance + stakedAmount
    val activePowerTier: VoidPowerTier get() = VoidPowerTier.tierForStake(stakedAmount)
}
