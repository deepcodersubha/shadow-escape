package com.example.shadowescape.model

enum class ArmorCategory(val displayName: String) {
    LIGHT("Light Garb"),
    MEDIUM("Medium Armor"),
    HEAVY("Heavy Plate"),
    MAGICAL("Arcane Robes")
}

data class ArmorPiece(
    val id: String,
    val name: String,
    val description: String,
    val assetFile: String,
    val physicalDef: Float,      // % damage reduction (e.g. 20f = 20%)
    val magicNegation: Float,     // % magic damage reduction
    val poise: Float,             // +Poise bonus (stagger resistance)
    val weight: Float,            // Equipment weight
    val category: ArmorCategory,
    val rarityColorHex: String = "#38BDF8",
    val classAffinity: String = "Universal"
)

object Armors {

    // --- Starting Class Armors ---

    val GILDED_SUN_TEMPLAR = ArmorPiece(
        id = "gilded_sun_templar",
        name = "Gilded Sun Templar Plate",
        description = "Consecrated heavy brass and gilded plate blessed by the golden grace. Provides impenetrable defense and holy starlight protection.",
        assetFile = "07.png",
        physicalDef = 24f,
        magicNegation = 16f,
        poise = 45f,
        weight = 14.5f,
        category = ArmorCategory.HEAVY,
        rarityColorHex = "#F59E0B",
        classAffinity = "Paladin"
    )

    val GLINTSTONE_SPELLPLATE = ArmorPiece(
        id = "glintstone_spellplate",
        name = "Glintstone Spellplate",
        description = "Ornate silver cuirass embedded with radiant azure glintstones. Channels continuous mana flow while deflecting physical cuts.",
        assetFile = "01.png",
        physicalDef = 18f,
        magicNegation = 24f,
        poise = 30f,
        weight = 10.0f,
        category = ArmorCategory.MEDIUM,
        rarityColorHex = "#38BDF8",
        classAffinity = "Magic Swordsman"
    )

    val CRIMSON_EXECUTIONER = ArmorPiece(
        id = "crimson_executioner",
        name = "Crimson Executioner Carapace",
        description = "Brutal red-iron breastplate forged in abyssal slag. Heavy plating grants overwhelming poise to shrug off enemy stagger.",
        assetFile = "25.png",
        physicalDef = 28f,
        magicNegation = 8f,
        poise = 60f,
        weight = 16.0f,
        category = ArmorCategory.HEAVY,
        rarityColorHex = "#EF4444",
        classAffinity = "Warrior"
    )

    val ASTRAL_STARLIGHT_MANTLE = ArmorPiece(
        id = "astral_starlight_mantle",
        name = "Astral Starlight Mantle",
        description = "Cosmic silk robes woven with fallen star dust. Offers supreme arcane resistance and enhances spell potency.",
        assetFile = "150.png",
        physicalDef = 10f,
        magicNegation = 32f,
        poise = 16f,
        weight = 6.0f,
        category = ArmorCategory.MAGICAL,
        rarityColorHex = "#A855F7",
        classAffinity = "Astral Sorcerer"
    )

    val SHADOW_CHITIN_VEST = ArmorPiece(
        id = "shadow_chitin_vest",
        name = "Shadow Chitin Vest",
        description = "Lightweight treated carapaces woven with dark leather. Enables silent acrobatic movement and swift evasion.",
        assetFile = "115.png",
        physicalDef = 14f,
        magicNegation = 14f,
        poise = 20f,
        weight = 6.5f,
        category = ArmorCategory.LIGHT,
        rarityColorHex = "#00E5FF",
        classAffinity = "Shadow Shinobi"
    )

    val DARK_IRON_FULLPLATE = ArmorPiece(
        id = "dark_iron_fullplate",
        name = "Dark Iron Fullplate",
        description = "Towering fortress cuirass of layered iron. Turns away ferocious blows at the expense of heavier burden.",
        assetFile = "100.png",
        physicalDef = 30f,
        magicNegation = 10f,
        poise = 68f,
        weight = 18.0f,
        category = ArmorCategory.HEAVY,
        rarityColorHex = "#F59E0B",
        classAffinity = "Iron Vanguard"
    )

    val TWILIGHT_PHANTOM_SHROUD = ArmorPiece(
        id = "twilight_phantom_shroud",
        name = "Twilight Phantom Shroud",
        description = "Midnight leather vestments cloaked with raven-black fibers. Infused with chilly frost mist that slips through enemy grasp.",
        assetFile = "175.png",
        physicalDef = 15f,
        magicNegation = 20f,
        poise = 22f,
        weight = 7.5f,
        category = ArmorCategory.LIGHT,
        rarityColorHex = "#818CF8",
        classAffinity = "Nightblade"
    )

    val MASTERLESS_RONIN_GAMBESON = ArmorPiece(
        id = "masterless_ronin_gambeson",
        name = "Masterless Ronin Gambeson",
        description = "Quilted traveler haori reinforced with lacquered bamboo plates. Perfectly balanced for both power cleaves and lightning draws.",
        assetFile = "200.png",
        physicalDef = 17f,
        magicNegation = 15f,
        poise = 32f,
        weight = 9.0f,
        category = ArmorCategory.MEDIUM,
        rarityColorHex = "#10B981",
        classAffinity = "Ronin"
    )

    val WEATHERED_TARNISHED_CUIRASS = ArmorPiece(
        id = "weathered_tarnished_cuirass",
        name = "Weathered Tarnished Cuirass",
        description = "Durable battered steel breastplate worn by relentless wanderers. Versatile protection for any perilous descent.",
        assetFile = "02.png",
        physicalDef = 13f,
        magicNegation = 12f,
        poise = 22f,
        weight = 8.5f,
        category = ArmorCategory.MEDIUM,
        rarityColorHex = "#CBD5E1",
        classAffinity = "Wanderer"
    )

    // --- Unlockable & High-Tier Armors ---

    val DRAGONBONE_COLOSSUS = ArmorPiece(
        id = "dragonbone_colossus",
        name = "Dragonbone Colossus Carapace",
        description = "Chiseled from the fossilized ribcage of an ancient abyssal dragon. Unmatched physical deflection and staggering poise.",
        assetFile = "20.png",
        physicalDef = 34f,
        magicNegation = 12f,
        poise = 75f,
        weight = 19.5f,
        category = ArmorCategory.HEAVY,
        rarityColorHex = "#F97316",
        classAffinity = "Colossus"
    )

    val ROYAL_SUN_AEGIS = ArmorPiece(
        id = "royal_sun_aegis",
        name = "Royal Sun Aegis",
        description = "Masterwork ceremonial cuirass plated with mirror-finish electrum. Deflects both brute steel and eldritch sorceries.",
        assetFile = "65.png",
        physicalDef = 26f,
        magicNegation = 26f,
        poise = 52f,
        weight = 13.0f,
        category = ArmorCategory.HEAVY,
        rarityColorHex = "#EAB308",
        classAffinity = "Holy"
    )

    val CRUCIBLE_CHAMPION_PLATE = ArmorPiece(
        id = "crucible_champion_plate",
        name = "Crucible Champion Plate",
        description = "Fiery red steel hammered on the primal anvil of the catacombs. He who wears it feels neither fatigue nor pain.",
        assetFile = "93.png",
        physicalDef = 36f,
        magicNegation = 14f,
        poise = 82f,
        weight = 21.0f,
        category = ArmorCategory.HEAVY,
        rarityColorHex = "#DC2626",
        classAffinity = "Warrior"
    )

    val VOID_KNIGHT_DREADPLATE = ArmorPiece(
        id = "void_knight_dreadplate",
        name = "Void Knight Dreadplate",
        description = "Dark purple abyssal alloy radiating gravity distortions. Swallows incoming spell energy whole.",
        assetFile = "182.png",
        physicalDef = 31f,
        magicNegation = 30f,
        poise = 66f,
        weight = 17.0f,
        category = ArmorCategory.HEAVY,
        rarityColorHex = "#9333EA",
        classAffinity = "Abyss"
    )

    val PALE_STAR_AEGIS = ArmorPiece(
        id = "pale_star_aegis",
        name = "Pale Star Aegis",
        description = "Gleaming celestial plate etched with cosmic constellations. Grants superior mobility and starlight defense.",
        assetFile = "49.png",
        physicalDef = 25f,
        magicNegation = 28f,
        poise = 46f,
        weight = 11.5f,
        category = ArmorCategory.MEDIUM,
        rarityColorHex = "#38BDF8",
        classAffinity = "Spellblade"
    )

    val GILDED_DRAGON_LORD = ArmorPiece(
        id = "gilded_dragon_lord",
        name = "Gilded Dragon Lord Plate",
        description = "Sacred gold armor adorned with draconic crests. Imbued with imperial majesty and unyielding resilience.",
        assetFile = "27.png",
        physicalDef = 33f,
        magicNegation = 22f,
        poise = 72f,
        weight = 18.5f,
        category = ArmorCategory.HEAVY,
        rarityColorHex = "#F59E0B",
        classAffinity = "Paladin"
    )

    val FROSTBORNE_REGALIA = ArmorPiece(
        id = "frostborne_regalia",
        name = "Frostborne Archmage Regalia",
        description = "Chilling crystalline mantle that emits a permanent frost barrier. Shatters enemy magical surges on contact.",
        assetFile = "161.png",
        physicalDef = 12f,
        magicNegation = 36f,
        poise = 22f,
        weight = 6.0f,
        category = ArmorCategory.MAGICAL,
        rarityColorHex = "#67E8F9",
        classAffinity = "Sorcerer"
    )

    val SHADOW_STALKER_SUIT = ArmorPiece(
        id = "shadow_stalker_suit",
        name = "Shadow Stalker Nightsuit",
        description = "Obsidian-dyed soft cuirass fitted with silent joints. Favored by unseen phantom blades.",
        assetFile = "188.png",
        physicalDef = 19f,
        magicNegation = 21f,
        poise = 32f,
        weight = 8.0f,
        category = ArmorCategory.LIGHT,
        rarityColorHex = "#818CF8",
        classAffinity = "Assassin"
    )

    val ALL_ARMORS = listOf(
        GILDED_SUN_TEMPLAR,
        GLINTSTONE_SPELLPLATE,
        CRIMSON_EXECUTIONER,
        ASTRAL_STARLIGHT_MANTLE,
        SHADOW_CHITIN_VEST,
        DARK_IRON_FULLPLATE,
        TWILIGHT_PHANTOM_SHROUD,
        MASTERLESS_RONIN_GAMBESON,
        WEATHERED_TARNISHED_CUIRASS,
        DRAGONBONE_COLOSSUS,
        ROYAL_SUN_AEGIS,
        CRUCIBLE_CHAMPION_PLATE,
        VOID_KNIGHT_DREADPLATE,
        PALE_STAR_AEGIS,
        GILDED_DRAGON_LORD,
        FROSTBORNE_REGALIA,
        SHADOW_STALKER_SUIT
    )

    fun getStartingArmorForOrigin(originId: String): ArmorPiece {
        return when (originId) {
            "paladin" -> GILDED_SUN_TEMPLAR
            "magic_swordsman" -> GLINTSTONE_SPELLPLATE
            "executioner" -> CRIMSON_EXECUTIONER
            "sorcerer" -> ASTRAL_STARLIGHT_MANTLE
            "shinobi" -> SHADOW_CHITIN_VEST
            "vanguard" -> DARK_IRON_FULLPLATE
            "nightblade" -> TWILIGHT_PHANTOM_SHROUD
            "ronin" -> MASTERLESS_RONIN_GAMBESON
            else -> WEATHERED_TARNISHED_CUIRASS
        }
    }

    fun findById(id: String): ArmorPiece? {
        return ALL_ARMORS.find { it.id == id }
    }
}
