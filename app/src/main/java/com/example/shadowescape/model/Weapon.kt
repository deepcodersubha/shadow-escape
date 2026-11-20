package com.example.shadowescape.model

enum class WeaponType {
    KATANA,
    SWORD,
    GREATSWORD,
    STAFF,
    CLUB,
    BOW,
    SCYTHE,
    GAUNTLET
}

enum class StatusEffect(val displayName: String, val colorHex: String) {
    NONE("None", "#94A3B8"),
    BLEED("Bleed", "#EF4444"),
    FROSTBITE("Frostbite", "#38BDF8"),
    BURN("Burn", "#F97316"),
    POISON("Poison", "#22C55E")
}

enum class WieldStance(val displayName: String) {
    ONE_HANDED("1-Handed"),
    TWO_HANDED("2-Handed")
}

enum class ItemRarity(val displayName: String, val colorHex: String) {
    COMMON("Common", "#94A3B8"),
    RARE("Rare", "#38BDF8"),
    EPIC("Epic", "#A855F7"),
    LEGENDARY("Legendary", "#F59E0B")
}

data class Weapon(
    val id: String,
    val name: String,
    val type: WeaponType,
    val description: String,
    val baseDamage: Float,
    val strScaling: Float,
    val agiScaling: Float,
    val intScaling: Float,
    val staminaCost: Float,
    val manaCost: Float = 0f,
    val attackDuration: Float = 0.25f,
    val range: Float = 75f,
    val rarity: ItemRarity = ItemRarity.COMMON,
    val skillName: String = "Combat Skill",
    val skillManaCost: Float = 14f,
    val skillDescription: String = "A martial weapon art.",
    val statusEffect: StatusEffect = StatusEffect.NONE,
    val statusBuildup: Float = 0f,
    val stance: WieldStance = WieldStance.ONE_HANDED,
    val reqStr: Int = 0,
    val reqAgi: Int = 0,
    val reqInt: Int = 0
) {
    val isTwoHanded: Boolean get() = stance == WieldStance.TWO_HANDED

    fun calculateEffectiveDamage(stats: PlayerStats): Float {
        val totalReq = reqStr + reqAgi + reqInt
        if (totalReq <= 0) return baseDamage

        val deficitStr = maxOf(0, reqStr - stats.strength)
        val deficitAgi = maxOf(0, reqAgi - stats.agility)
        val deficitInt = maxOf(0, reqInt - stats.intelligence)
        val totalDeficit = deficitStr + deficitAgi + deficitInt

        if (totalDeficit <= 0) return baseDamage

        val pointValue = baseDamage / totalReq.toFloat()
        val penalty = totalDeficit * 2f * pointValue
        return maxOf(1f, baseDamage - penalty)
    }

    fun calculateStatPenalty(stats: PlayerStats): Float {
        val totalReq = reqStr + reqAgi + reqInt
        if (totalReq <= 0) return 0f

        val deficitStr = maxOf(0, reqStr - stats.strength)
        val deficitAgi = maxOf(0, reqAgi - stats.agility)
        val deficitInt = maxOf(0, reqInt - stats.intelligence)
        val totalDeficit = deficitStr + deficitAgi + deficitInt

        if (totalDeficit <= 0) return 0f

        val pointValue = baseDamage / totalReq.toFloat()
        return (totalDeficit * 2f * pointValue).coerceAtMost(baseDamage - 1f)
    }

    fun hasStatRequirementsMet(stats: PlayerStats): Boolean {
        return stats.strength >= reqStr && stats.agility >= reqAgi && stats.intelligence >= reqInt
    }

    fun getRequirementString(): String {
        val parts = mutableListOf<String>()
        if (reqStr > 0) parts.add("$reqStr STR")
        if (reqAgi > 0) parts.add("$reqAgi AGI")
        if (reqInt > 0) parts.add("$reqInt INT")
        return if (parts.isEmpty()) "None" else parts.joinToString(" • ")
    }

    fun calculateDamage(stats: PlayerStats): Float {
        val effectiveBase = calculateEffectiveDamage(stats)
        val strBonus = (stats.strength - 10).coerceAtLeast(0) * strScaling
        val agiBonus = (stats.agility - 10).coerceAtLeast(0) * agiScaling
        val intBonus = (stats.intelligence - 10).coerceAtLeast(0) * intScaling
        val metRatio = if (hasStatRequirementsMet(stats)) 1.0f else (effectiveBase / baseDamage.coerceAtLeast(1f))
        return effectiveBase + (strBonus + agiBonus + intBonus) * metRatio
    }

    private fun scalingToGrade(scaling: Float): String {
        return when {
            scaling >= 2.0f -> "S"
            scaling >= 1.5f -> "A"
            scaling >= 1.0f -> "B"
            scaling >= 0.6f -> "C"
            scaling >= 0.25f -> "D"
            scaling > 0f -> "E"
            else -> "-"
        }
    }

    val strGrade: String get() = scalingToGrade(strScaling)
    val agiGrade: String get() = scalingToGrade(agiScaling)
    val intGrade: String get() = scalingToGrade(intScaling)
}

object Weapons {
    val SHADOW_KATANA = Weapon(
        id = "katana",
        name = "Shadow Katana",
        type = WeaponType.KATANA,
        description = "A swift curved blade forged in midnight steel. High Agility scaling with rapid multi-hit combos.",
        baseDamage = 22f,
        strScaling = 0.4f,
        agiScaling = 1.9f,
        intScaling = 0.0f,
        staminaCost = 12f,
        attackDuration = 0.22f,
        range = 80f,
        skillName = "Phantom Dash",
        skillManaCost = 14f,
        skillDescription = "Dashes forward through shadow, slicing all enemies in path with invulnerability."
    )

    val STEEL_BROADSWORD = Weapon(
        id = "broadsword",
        name = "Steel Broadsword",
        type = WeaponType.SWORD,
        description = "A trusty double-edged straight sword with balanced Strength and Agility scaling. Solid poise damage.",
        baseDamage = 28f,
        strScaling = 1.4f,
        agiScaling = 1.1f,
        intScaling = 0.0f,
        staminaCost = 14f,
        attackDuration = 0.24f,
        range = 85f,
        skillName = "Stalwart Guardbreaker",
        skillManaCost = 12f,
        skillDescription = "Braces with hyper-armor and unleashes an upward thrust that shatters poise."
    )

    val EXECUTIONER_GREATSWORD = Weapon(
        id = "greatsword",
        name = "Executioner Slab",
        type = WeaponType.GREATSWORD,
        description = "A colossal slab of unrefined iron. Crushes enemy poise and posture with heavy, screen-shaking cleaves.",
        baseDamage = 52f,
        strScaling = 2.6f,
        agiScaling = 0.2f,
        intScaling = 0.0f,
        staminaCost = 28f,
        attackDuration = 0.45f,
        range = 105f,
        skillName = "Titan Slam",
        skillManaCost = 18f,
        skillDescription = "Leaps into a crushing downward earth-slam creating a wide radial shockwave."
    )

    val ASTRAL_STAFF = Weapon(
        id = "staff",
        name = "Astral Staff",
        type = WeaponType.STAFF,
        description = "A runic conduit carved from meteorite crystal. Fires piercing celestial projectiles that scale with Intelligence.",
        baseDamage = 30f,
        strScaling = 0.0f,
        agiScaling = 0.2f,
        intScaling = 2.4f,
        staminaCost = 8f,
        manaCost = 14f,
        attackDuration = 0.32f,
        range = 400f,
        skillName = "Supernova Burst",
        skillManaCost = 22f,
        skillDescription = "Condenses ambient mana into an expanding astral explosion around the caster."
    )

    val HARDWOOD_CLUB = Weapon(
        id = "club",
        name = "Hardwood Club",
        type = WeaponType.CLUB,
        description = "A dense oak branch bound with rough twine. Crude yet formidable in the hands of a determined wanderer.",
        baseDamage = 26f,
        strScaling = 1.8f,
        agiScaling = 0.3f,
        intScaling = 0.0f,
        staminaCost = 15f,
        attackDuration = 0.28f,
        range = 75f,
        skillName = "Earth Tremor",
        skillManaCost = 10f,
        skillDescription = "Vigorously bashes the bedrock, sending concussive vibrations that stagger nearby foes.",
        stance = WieldStance.ONE_HANDED,
        reqStr = 10
    )

    val GREAT_HAMMER = Weapon(
        id = "great_hammer",
        name = "Great Hammer",
        type = WeaponType.CLUB,
        description = "A colossal warhammer requiring tremendous Strength. Can only be wielded two-handed. Punishes lacking attributes severely.",
        baseDamage = 150f,
        strScaling = 2.4f,
        agiScaling = 0.0f,
        intScaling = 0.0f,
        staminaCost = 28f,
        attackDuration = 0.42f,
        range = 95f,
        rarity = ItemRarity.EPIC,
        stance = WieldStance.TWO_HANDED,
        reqStr = 15,
        skillName = "Earthshatter Slam",
        skillManaCost = 18f,
        skillDescription = "Bring down colossal concussive force that knocks down all surrounding enemies."
    )

    val GIGIS_DARK_STAFF = Weapon(
        id = "gigis_dark_staff",
        name = "Gigi's Dark Staff",
        type = WeaponType.STAFF,
        description = "A legendary ancient catalyst channeling void stars. Demands both deep Intelligence and Strength to control its celestial power.",
        baseDamage = 300f,
        strScaling = 1.0f,
        agiScaling = 0.0f,
        intScaling = 2.8f,
        staminaCost = 16f,
        manaCost = 28f,
        attackDuration = 0.38f,
        range = 450f,
        rarity = ItemRarity.LEGENDARY,
        stance = WieldStance.TWO_HANDED,
        reqStr = 10,
        reqInt = 20,
        skillName = "Abyssal Singularity",
        skillManaCost = 32f,
        skillDescription = "Summons a gravity well of dark stars that compresses and eradicates foes."
    )

    // --- DISCOVERABLE LOOT & CHEST WEAPONS (100% Original IP) ---
    val NIGHTPROWLER_TALON = Weapon(
        id = "nightprowler_talon",
        name = "Nightprowler Talon",
        type = WeaponType.GREATSWORD,
        description = "Curved greatsword fashioned from the obsidian talon of an abyss beast. Swift slashes and fierce bleed force.",
        baseDamage = 42f,
        strScaling = 1.2f, // B
        agiScaling = 2.1f, // S
        intScaling = 0.0f,
        staminaCost = 22f,
        attackDuration = 0.34f,
        range = 95f,
        rarity = ItemRarity.RARE,
        skillName = "Shadow Somersault",
        skillManaCost = 16f,
        skillDescription = "Acrobatic upward crescent sweep followed by an evasive backward air-spring."
    )

    val WYRM_WING_HALBERD = Weapon(
        id = "wyrm_wing_halberd",
        name = "Wyrm Wing Halberd",
        type = WeaponType.SWORD,
        description = "Ancient martial polearm fashioned from a dragon wyrm's wing-crest. Sweeping cleaves that pierce defenses.",
        baseDamage = 38f,
        strScaling = 1.7f, // A
        agiScaling = 1.3f, // B
        intScaling = 0.0f,
        staminaCost = 18f,
        attackDuration = 0.30f,
        range = 95f,
        rarity = ItemRarity.EPIC,
        skillName = "Vortical Tempest",
        skillManaCost = 18f,
        skillDescription = "Spins into a 360-degree whirlwind slash deflecting incoming attacks."
    )

    val PALE_ECLIPSE_GREATSWORD = Weapon(
        id = "pale_eclipse_gs",
        name = "Pale Eclipse Greatsword",
        type = WeaponType.GREATSWORD,
        description = "Consecrated astral blade forged under a pale solar eclipse. Radiates celestial frost sorcery that devastates poise.",
        baseDamage = 48f,
        strScaling = 0.8f, // C
        agiScaling = 0.3f, // D
        intScaling = 2.8f, // S
        staminaCost = 26f,
        manaCost = 10f,
        attackDuration = 0.40f,
        range = 110f,
        rarity = ItemRarity.LEGENDARY,
        skillName = "Frostfall Wave",
        skillManaCost = 24f,
        skillDescription = "Cleaves the earth to send forth an erupting cascade of freezing crystal spikes.",
        statusEffect = StatusEffect.FROSTBITE,
        statusBuildup = 42f
    )

    val CRIMSON_VEIN_KATANA = Weapon(
        id = "crimson_vein",
        name = "Crimson Vein Katana",
        type = WeaponType.KATANA,
        description = "Cursed blade bathed in dark ichor. Unrivaled speed and scarlet razor flurries slicing effortlessly through armor.",
        baseDamage = 35f,
        strScaling = 0.6f, // C
        agiScaling = 2.4f, // S
        intScaling = 1.2f, // B
        staminaCost = 13f,
        attackDuration = 0.20f,
        range = 85f,
        rarity = ItemRarity.LEGENDARY,
        skillName = "Scarlet Cascade",
        skillManaCost = 22f,
        skillDescription = "Executes an explosive 5-hit crimson flurry cross-slashing the air.",
        statusEffect = StatusEffect.BLEED,
        statusBuildup = 38f
    )

    val OBSIDIAN_DAGGER = Weapon(
        id = "obsidian_dagger",
        name = "Obsidian Dagger",
        type = WeaponType.KATANA,
        description = "Short volcanic-glass dirk with emerald-set hilt. Ultra-fast close-range slices.",
        baseDamage = 20f,
        strScaling = 0.2f,
        agiScaling = 2.2f,
        intScaling = 0.0f,
        staminaCost = 8f,
        attackDuration = 0.16f,
        range = 58f,
        rarity = ItemRarity.RARE,
        skillName = "Viper Rend",
        skillManaCost = 12f,
        skillDescription = "Dashes inside an enemy's guard with rapid vitals slashes."
    )

    val IRON_SPEAR = Weapon(
        id = "iron_spear",
        name = "Iron Spear",
        type = WeaponType.SWORD,
        description = "Long steel-tipped spear with unmatched forward thrust reach to keep foes at bay.",
        baseDamage = 30f,
        strScaling = 1.3f,
        agiScaling = 1.4f,
        intScaling = 0.0f,
        staminaCost = 14f,
        attackDuration = 0.26f,
        range = 108f,
        rarity = ItemRarity.COMMON,
        skillName = "Piercing Lunge",
        skillManaCost = 14f,
        skillDescription = "Propels forward with an armor-piercing spear charge."
    )

    val IRON_BATTLE_AXE = Weapon(
        id = "battle_axe",
        name = "Iron Battle Axe",
        type = WeaponType.GREATSWORD,
        description = "Heavy double-bit bearded axe that cleaves through shields with brutal downward momentum.",
        baseDamage = 46f,
        strScaling = 2.3f,
        agiScaling = 0.3f,
        intScaling = 0.0f,
        staminaCost = 24f,
        attackDuration = 0.38f,
        range = 92f,
        rarity = ItemRarity.RARE,
        skillName = "Whirlwind Cleave",
        skillManaCost = 16f,
        skillDescription = "Swings both heavy axe blades in a devastating wide arc."
    )

    val GOLDEN_WARHAMMER = Weapon(
        id = "war_hammer",
        name = "Golden Warhammer",
        type = WeaponType.CLUB,
        description = "Gilded war-maul inlaid with sapphire counterweights. Shatters stone and heavy armor alike.",
        baseDamage = 44f,
        strScaling = 2.5f,
        agiScaling = 0.0f,
        intScaling = 0.2f,
        staminaCost = 22f,
        attackDuration = 0.34f,
        range = 86f,
        rarity = ItemRarity.EPIC,
        skillName = "Judgement Slam",
        skillManaCost = 16f,
        skillDescription = "Crushes the earth with gilded force, sending shockwaves outward."
    )

    val CRYSTAL_MACE = Weapon(
        id = "crystal_mace",
        name = "Crystal Mace",
        type = WeaponType.CLUB,
        description = "Spiked morningstar forged from resonant arcane crystal. Combines blunt stagger with magic resonance.",
        baseDamage = 32f,
        strScaling = 1.5f,
        agiScaling = 0.3f,
        intScaling = 1.2f,
        staminaCost = 16f,
        attackDuration = 0.28f,
        range = 76f,
        rarity = ItemRarity.RARE,
        skillName = "Shatter Nova",
        skillManaCost = 14f,
        skillDescription = "Strikes the ground to detonate crystal shockwaves."
    )

    val SUN_BOW = Weapon(
        id = "sun_bow",
        name = "Sunfire Greatbow",
        type = WeaponType.BOW,
        description = "Gilded warbow strung with solar sinew. Launches blazing arrows that ignite foes from afar.",
        baseDamage = 26f,
        strScaling = 0.6f,
        agiScaling = 2.0f,
        intScaling = 0.4f,
        staminaCost = 11f,
        attackDuration = 0.24f,
        range = 420f,
        rarity = ItemRarity.RARE,
        skillName = "Solar Volley",
        skillManaCost = 16f,
        skillDescription = "Fires a fanned triple-arrow burst of sunfire bolts.",
        statusEffect = StatusEffect.BURN,
        statusBuildup = 34f
    )

    val SHADOW_BONE_BOW = Weapon(
        id = "shadow_bone_bow",
        name = "Necro-Bone Bow",
        type = WeaponType.BOW,
        description = "Carved from abyssal bone. Fires silent, armor-piercing bolts that inflict hemorrhagic bleed.",
        baseDamage = 31f,
        strScaling = 0.4f,
        agiScaling = 2.2f,
        intScaling = 0.8f,
        staminaCost = 12f,
        attackDuration = 0.23f,
        range = 440f,
        rarity = ItemRarity.EPIC,
        skillName = "Phantom Volley",
        skillManaCost = 18f,
        skillDescription = "Unleashes three cursed bone arrows in rapid succession.",
        statusEffect = StatusEffect.BLEED,
        statusBuildup = 36f
    )

    val SOUL_SCYTHE = Weapon(
        id = "soul_scythe",
        name = "Frost Reaver Scythe",
        type = WeaponType.SCYTHE,
        description = "Crescent arcane reaper that harvests warmth and poise in wide sweeping arcs.",
        baseDamage = 36f,
        strScaling = 0.7f,
        agiScaling = 1.6f,
        intScaling = 1.4f,
        staminaCost = 17f,
        attackDuration = 0.30f,
        range = 108f,
        rarity = ItemRarity.EPIC,
        skillName = "Reaper's Crescent",
        skillManaCost = 18f,
        skillDescription = "Lunges forward with a freezing circular reap that cleaves all nearby foes.",
        statusEffect = StatusEffect.FROSTBITE,
        statusBuildup = 40f
    )

    val ROYAL_SCYTHE = Weapon(
        id = "royal_scythe",
        name = "Crimson Harvester",
        type = WeaponType.SCYTHE,
        description = "Royal executioner's scythe inlaid with blood rubies. Tears through flesh with deep Bleed buildup.",
        baseDamage = 39f,
        strScaling = 1.0f,
        agiScaling = 1.8f,
        intScaling = 0.0f,
        staminaCost = 18f,
        attackDuration = 0.31f,
        range = 110f,
        rarity = ItemRarity.RARE,
        skillName = "Blood Harvest",
        skillManaCost = 16f,
        skillDescription = "A sweeping crimson arc that rips poise and triggers heavy bleed.",
        statusEffect = StatusEffect.BLEED,
        statusBuildup = 45f
    )

    val MOLTEN_GAUNTLETS = Weapon(
        id = "molten_gauntlets",
        name = "Cinderfist Gauntlets",
        type = WeaponType.GAUNTLET,
        description = "Volcanic iron knuckles glowing with forgefire. Unleashes relentless 4-hit brawler flurries.",
        baseDamage = 21f,
        strScaling = 1.9f,
        agiScaling = 1.5f,
        intScaling = 0.0f,
        staminaCost = 7f,
        attackDuration = 0.14f,
        range = 62f,
        rarity = ItemRarity.RARE,
        skillName = "Magma Uppercut",
        skillManaCost = 14f,
        skillDescription = "Explosive rising fist that shatters poise and ignites enemies.",
        statusEffect = StatusEffect.BURN,
        statusBuildup = 32f
    )

    val CRYSTAL_CLAWS = Weapon(
        id = "crystal_claws",
        name = "Glacier Claw Gauntlets",
        type = WeaponType.GAUNTLET,
        description = "Razor-edged frost crystal gauntlets. Ultra-fast punches that freeze foes solid.",
        baseDamage = 23f,
        strScaling = 1.1f,
        agiScaling = 1.9f,
        intScaling = 1.1f,
        staminaCost = 7f,
        attackDuration = 0.14f,
        range = 64f,
        rarity = ItemRarity.EPIC,
        skillName = "Shatter Barrage",
        skillManaCost = 15f,
        skillDescription = "Rapid crystal fist flurry with heavy Frostbite buildup.",
        statusEffect = StatusEffect.FROSTBITE,
        statusBuildup = 34f
    )

    val ALL = listOf(
        SHADOW_KATANA,
        OBSIDIAN_DAGGER,
        STEEL_BROADSWORD,
        IRON_SPEAR,
        IRON_BATTLE_AXE,
        EXECUTIONER_GREATSWORD,
        GOLDEN_WARHAMMER,
        GREAT_HAMMER,
        CRYSTAL_MACE,
        ASTRAL_STAFF,
        GIGIS_DARK_STAFF,
        HARDWOOD_CLUB,
        NIGHTPROWLER_TALON,
        WYRM_WING_HALBERD,
        PALE_ECLIPSE_GREATSWORD,
        CRIMSON_VEIN_KATANA,
        SUN_BOW,
        SHADOW_BONE_BOW,
        SOUL_SCYTHE,
        ROYAL_SCYTHE,
        MOLTEN_GAUNTLETS,
        CRYSTAL_CLAWS
    )
}
