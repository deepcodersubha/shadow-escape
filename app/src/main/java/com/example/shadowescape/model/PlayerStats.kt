package com.example.shadowescape.model

class PlayerStats {
    // Core Attributes (Elden Ring style)
    var level: Int = 1
    var vigor: Int = 10
    var endurance: Int = 10
    var strength: Int = 10
    var agility: Int = 10
    var intelligence: Int = 10

    // Currency (Souls / Runes)
    var essence: Int = 150

    // Site of Grace Shadow Pacts (Risk-Reward Modifiers)
    var bloodPactActive: Boolean = false    // Enemies deal +35% DMG, drop +60% Essence
    var asceticPactActive: Boolean = false  // Flasks disabled, Weapon Arts cost 50% less FP

    // Computed Maximums based on attributes
    val maxHp: Float get() = 100f + (vigor - 10) * 15f
    val maxMana: Float get() = 50f + (intelligence - 10) * 10f
    val maxStamina: Float get() = 80f + (endurance - 10) * 8f

    // Current Resource Values
    var currentHp: Float = 100f
    var currentMana: Float = 50f
    var currentStamina: Float = 80f

    // Souls Flask Economy (Crimson Tears & Cerulean Tears)
    var maxCrimsonFlasks: Int = 3
    var currentCrimsonFlasks: Int = maxCrimsonFlasks

    var maxCeruleanFlasks: Int = 2
    var currentCeruleanFlasks: Int = maxCeruleanFlasks

    var crimsonHealRatio: Float = 0.55f

    fun drinkCrimsonFlask(): Boolean {
        if (asceticPactActive) return false
        if (currentCrimsonFlasks > 0 && currentHp < maxHp) {
            currentCrimsonFlasks--
            heal(maxHp * crimsonHealRatio)
            return true
        }
        return false
    }

    fun drinkCeruleanFlask(): Boolean {
        if (asceticPactActive) return false
        if (currentCeruleanFlasks > 0 && currentMana < maxMana) {
            currentCeruleanFlasks--
            currentMana = (currentMana + maxMana * 0.65f).coerceAtMost(maxMana)
            return true
        }
        return false
    }

    fun refillFlasks() {
        currentCrimsonFlasks = maxCrimsonFlasks
        currentCeruleanFlasks = maxCeruleanFlasks
    }

    // Stamina Regeneration controls
    private val staminaRegenRate: Float = 40f // Points per second
    private val manaRegenRate: Float = 4f    // Points per second
    private var staminaRegenDelay: Float = 0f

    init {
        currentHp = maxHp
        currentMana = maxMana
        currentStamina = maxStamina
    }

    fun update(dt: Float) {
        // Stamina recovery
        if (staminaRegenDelay > 0f) {
            staminaRegenDelay -= dt
        } else if (currentStamina < maxStamina) {
            currentStamina = (currentStamina + staminaRegenRate * dt).coerceAtMost(maxStamina)
        }

        // Mana passive recovery
        if (currentMana < maxMana) {
            currentMana = (currentMana + manaRegenRate * dt).coerceAtMost(maxMana)
        }
    }

    fun takeDamage(amount: Float): Boolean {
        currentHp = (currentHp - amount).coerceAtLeast(0f)
        return currentHp <= 0f
    }

    fun heal(amount: Float) {
        currentHp = (currentHp + amount).coerceAtMost(maxHp)
    }

    fun useStamina(amount: Float): Boolean {
        if (currentStamina >= amount) {
            currentStamina -= amount
            staminaRegenDelay = 0.6f // Short delay before stamina starts regenerating
            return true
        }
        return false
    }

    fun useMana(amount: Float): Boolean {
        if (currentMana >= amount) {
            currentMana -= amount
            return true
        }
        return false
    }

    fun addEssence(amount: Int, applyPactBonus: Boolean = true) {
        val finalAmount = if (applyPactBonus && bloodPactActive) (amount * 1.6f).toInt() else amount
        essence += finalAmount
    }

    fun costForNextLevel(): Int {
        return (level * 80 + (level * level * 15))
    }

    fun levelUp(attribute: String): Boolean {
        val cost = costForNextLevel()
        if (essence >= cost) {
            essence -= cost
            level++
            when (attribute.uppercase()) {
                "VIG", "VIGOR" -> {
                    vigor++
                    currentHp = maxHp
                }
                "END", "ENDURANCE" -> {
                    endurance++
                    currentStamina = maxStamina
                }
                "STR", "STRENGTH" -> strength++
                "AGI", "AGILITY" -> agility++
                "INT", "INTELLIGENCE" -> {
                    intelligence++
                    currentMana = maxMana
                }
            }
            return true
        }
        return false
    }

    fun resetToFull() {
        currentHp = maxHp
        currentMana = maxMana
        currentStamina = maxStamina
        refillFlasks()
    }
}
