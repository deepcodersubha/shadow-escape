package com.example.shadowescape.model

data class CharacterOrigin(
    val id: String,
    val name: String,
    val title: String,
    val icon: String,
    val description: String,
    val startingWeapon: Weapon,
    val vigor: Int,
    val endurance: Int,
    val strength: Int,
    val agility: Int,
    val intelligence: Int,
    val startingLevel: Int
)

object CharacterOrigins {
    val SHADOW_SHINOBI = CharacterOrigin(
        id = "shinobi",
        name = "Shadow Shinobi",
        title = "Ghost of the Catacombs",
        icon = "🗡️",
        description = "Swift assassin accustomed to the dark. Specializes in rapid multi-hit slashes, nimble rolls, and high Agility scaling.",
        startingWeapon = Weapons.SHADOW_KATANA,
        vigor = 12,
        endurance = 14,
        strength = 10,
        agility = 16,
        intelligence = 8,
        startingLevel = 9
    )

    val IRON_VANGUARD = CharacterOrigin(
        id = "vanguard",
        name = "Iron Vanguard",
        title = "Bulwark of the Fallen",
        icon = "🛡️",
        description = "Heavily armored knight with deep vigor and balanced sword mastery. Staggers foes and withstands fierce punishment.",
        startingWeapon = Weapons.STEEL_BROADSWORD,
        vigor = 15,
        endurance = 13,
        strength = 15,
        agility = 11,
        intelligence = 7,
        startingLevel = 10
    )

    val GRAVE_EXECUTIONER = CharacterOrigin(
        id = "executioner",
        name = "Grave Executioner",
        title = "Colossus of the Abyss",
        icon = "⚔️",
        description = "Brutal warrior who trades agility for pure destructive force. Smashes through enemy posture with colossal swings.",
        startingWeapon = Weapons.EXECUTIONER_GREATSWORD,
        vigor = 14,
        endurance = 12,
        strength = 17,
        agility = 9,
        intelligence = 7,
        startingLevel = 8
    )

    val ASTRAL_SORCERER = CharacterOrigin(
        id = "sorcerer",
        name = "Astral Sorcerer",
        title = "Weaver of Starlight",
        icon = "🔮",
        description = "Cosmic scholar attuned to celestial mana. Obliterates dangers from safe range with piercing astral projectiles.",
        startingWeapon = Weapons.ASTRAL_STAFF,
        vigor = 10,
        endurance = 10,
        strength = 8,
        agility = 11,
        intelligence = 17,
        startingLevel = 7
    )

    val THE_FORSAKEN = CharacterOrigin(
        id = "forsaken",
        name = "Ronin",
        title = "Masterless Wanderer",
        icon = "🎋",
        description = "A masterless wandering swordsman who cast aside clan and crest, roaming the shadow realm with disciplined steel and deadly iaijutsu poise.",
        startingWeapon = Weapons.HARDWOOD_CLUB,
        vigor = 10,
        endurance = 10,
        strength = 10,
        agility = 10,
        intelligence = 10,
        startingLevel = 1
    )

    val ALL = listOf(
        SHADOW_SHINOBI,
        IRON_VANGUARD,
        GRAVE_EXECUTIONER,
        ASTRAL_SORCERER,
        THE_FORSAKEN
    )
}
