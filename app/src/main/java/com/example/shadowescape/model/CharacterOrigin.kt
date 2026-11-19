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
    val startingLevel: Int,
    val traits: List<String> = emptyList(),
    val tacticalPlaystyle: String = "",
    val avatarId: String = id,
    val themeColorHex: String = "#38BDF8"
)

object CharacterOrigins {

    // --- Standard Archetype Definitions ---
    val SHADOW_SHINOBI = CharacterOrigin(
        id = "shinobi",
        name = "Shadow Shinobi",
        title = "Ghost of the Catacombs",
        icon = "🗡️",
        description = "Swift assassin accustomed to the dark. Specializes in rapid multi-hit flurry slashes, nimble rolls, and high Agility bleed scaling.",
        startingWeapon = Weapons.SHADOW_KATANA,
        vigor = 12,
        endurance = 14,
        strength = 10,
        agility = 16,
        intelligence = 8,
        startingLevel = 9,
        traits = listOf(
            "💨 Ghost Walk: Agile sprint (+15%), Roll stamina cost -20%",
            "⚡ Flurry Strikes: Katana attacks combo with accelerating speed",
            "🎯 Shadow Prowess: High Agility weapon damage scaling"
        ),
        tacticalPlaystyle = "Fluidly cancel attacks into acrobatic rolls. Chain rapid bleeds behind enemies.",
        avatarId = "shinobi",
        themeColorHex = "#00E5FF"
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
        startingLevel = 10,
        traits = listOf(
            "🛡️ Iron Bastion: +50 Poise, 15% physical damage reduction",
            "💥 Crushing Cleave: Heavy broadsword slashes stagger foes",
            "⚔️ Unyielding: Low stamina cost on defensive recoveries"
        ),
        tacticalPlaystyle = "Absorb brute force with poise. Break enemy guard using punishing heavy cleaves.",
        avatarId = "vanguard",
        themeColorHex = "#F59E0B"
    )

    val GRAVE_EXECUTIONER = CharacterOrigin(
        id = "executioner",
        name = "Warrior",
        title = "Colossus of the Abyss",
        icon = "🪓",
        description = "Brutal warrior who trades agility for pure destructive force. Smashes through enemy posture with colossal swings.",
        startingWeapon = Weapons.EXECUTIONER_GREATSWORD,
        vigor = 14,
        endurance = 12,
        strength = 17,
        agility = 9,
        intelligence = 7,
        startingLevel = 8,
        traits = listOf(
            "🩸 Blood Siphon: Stagger visceral attacks heal 12% HP",
            "🪓 Guard Shatter: Massive posture & poise damage per hit",
            "💀 Dread Aura: Increased critical strike chance on wounded foes"
        ),
        tacticalPlaystyle = "Unstoppable stagger juggernaut. Execute visceral finishers to siphon vital HP.",
        avatarId = "executioner",
        themeColorHex = "#EF4444"
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
        startingLevel = 7,
        traits = listOf(
            "🔮 Astral Conduit: Fires homing celestial starlight orbs",
            "💧 Font of Aether: Automatic continuous FP/Mana regeneration",
            "✨ Cosmic Resonance: Intelligence boosts all projectile spells"
        ),
        tacticalPlaystyle = "Arcane zoning artillery. Rain seeking starlight bolts while mana surges endlessly.",
        avatarId = "sorcerer",
        themeColorHex = "#A855F7"
    )

    val PALADIN = CharacterOrigin(
        id = "paladin",
        name = "Paladin",
        title = "Holy Templar of Golden Grace",
        icon = "🛡️✨",
        description = "A consecrated holy champion blending fortified defense with sacred starlight sorcery. Imbues heavy steel with radiant buffs and shrugs off punishing blows.",
        startingWeapon = Weapons.PALE_ECLIPSE_GREATSWORD,
        vigor = 14,
        endurance = 13,
        strength = 15,
        agility = 9,
        intelligence = 14,
        startingLevel = 10,
        traits = listOf(
            "🛡️ Holy Aegis: High poise defense, -20% physical damage taken",
            "✨ Sacred Attunement: Attacks restore both stamina and mana",
            "🕯️ Grace Blessing: Flask recovery potency increased by +25%"
        ),
        tacticalPlaystyle = "Stand firm against ferocious assaults. Retaliate with heavy magic-infused cleaves and consecrated poise.",
        avatarId = "paladin",
        themeColorHex = "#F59E0B"
    )

    val MAGIC_SWORDSMAN = CharacterOrigin(
        id = "magic_swordsman",
        name = "Magic Swordsman",
        title = "Weaver of Blade & Starlight",
        icon = "⚔️🔮",
        description = "An agile mystic duelist attuned to both physical combat and astral sorcery. Fluidly interweaves melee slashes with piercing arcane spell strikes.",
        startingWeapon = Weapons.PALE_ECLIPSE_GREATSWORD,
        vigor = 11,
        endurance = 11,
        strength = 14,
        agility = 10,
        intelligence = 14,
        startingLevel = 9,
        traits = listOf(
            "✨ Spellstrike: Melee attacks ignite with starlight energy",
            "🔮 Dual Attunement: Damage scales with both physical and mental power",
            "💧 Font of Aether: Continuous passive FP regeneration"
        ),
        tacticalPlaystyle = "Harass from medium distance with spells, then dash in for devastating magic-empowered sword combos.",
        avatarId = "magic_swordsman",
        themeColorHex = "#38BDF8"
    )

    val NIGHTBLADE = CharacterOrigin(
        id = "nightblade",
        name = "Nightblade",
        title = "Phantom of the Twilight Veil",
        icon = "🌙🗡️",
        description = "A lethal shadow-weaver who melds high Agility daggers with cryptic frost sorcery. Strikes from the shadows with chilling precision.",
        startingWeapon = Weapons.OBSIDIAN_DAGGER,
        vigor = 11,
        endurance = 12,
        strength = 9,
        agility = 15,
        intelligence = 14,
        startingLevel = 9,
        traits = listOf(
            "🌙 Twilight Step: Invulnerable rolls leave icy mist",
            "❄️ Frostbite Edge: Attacks inflict rapid Frost & Bleed buildup",
            "✨ Shadow Sorcery: Dagger attacks crit for +40% bonus damage"
        ),
        tacticalPlaystyle = "Slip behind enemies with low-cost rolls, inflict frostbite, and harvest essence with swift critical daggers.",
        avatarId = "nightblade",
        themeColorHex = "#818CF8"
    )

    val THE_FORSAKEN = CharacterOrigin(
        id = "ronin",
        name = "Ronin",
        title = "Masterless Wanderer",
        icon = "🎋",
        description = "A masterless wandering swordsman who cast aside clan and crest, roaming the realm with disciplined steel, balance, and deadly iaijutsu poise.",
        startingWeapon = Weapons.NIGHTPROWLER_TALON,
        vigor = 12,
        endurance = 12,
        strength = 14,
        agility = 14,
        intelligence = 8,
        startingLevel = 8,
        traits = listOf(
            "🎋 Iaijutsu Poise: First strike in a combo deals 2.5x posture damage",
            "⚡ Fluid Blade: Chains light and heavy attacks without interruption",
            "⚔️ Dual Scaling: Master of both heavy greatswords and nimble katanas"
        ),
        tacticalPlaystyle = "Switch stances dynamically to adapt to any foe. Out-space opponents with superior reach and timing.",
        avatarId = "ronin",
        themeColorHex = "#10B981"
    )

    val WANDERER = CharacterOrigin(
        id = "wanderer",
        name = "Wanderer",
        title = "The Unshackled Soul",
        icon = "⚜️",
        description = "A resilient traveler with equal distribution across all attributes. Bound by no single discipline, adapting to any weapon or spell found in the shadows.",
        startingWeapon = Weapons.HARDWOOD_CLUB,
        vigor = 10,
        endurance = 10,
        strength = 10,
        agility = 10,
        intelligence = 10,
        startingLevel = 1,
        traits = listOf(
            "🍂 Scavenger's Grit: +35% Flask drop rate from chests & enemies",
            "🗡️ Desperate Edge: +30% attack damage when health is below 40%",
            "🦴 Raw Adaptability: Equal stat distribution for any build path"
        ),
        tacticalPlaystyle = "Resilient abyssal brawler. Adapt to any weapon and strike harder as health depletes.",
        avatarId = "wanderer",
        themeColorHex = "#CBD5E1"
    )

    val ALL = listOf(
        SHADOW_SHINOBI,
        IRON_VANGUARD,
        GRAVE_EXECUTIONER,
        ASTRAL_SORCERER,
        PALADIN,
        MAGIC_SWORDSMAN,
        NIGHTBLADE,
        THE_FORSAKEN,
        WANDERER
    )

    /**
     * Dynamically determines the player's class/archetype based on their allocated stat ranges.
     */
    fun deriveClass(
        vigor: Int,
        endurance: Int,
        strength: Int,
        agility: Int,
        intelligence: Int
    ): CharacterOrigin {
        val totalAllocated = (vigor - 8) + (endurance - 8) + (strength - 8) + (agility - 8) + (intelligence - 8)
        val startLevel = (1 + totalAllocated / 2).coerceAtLeast(1)

        // 1. PALADIN: High Strength + High Intelligence/Magic + High Defense (Vigor/Endurance)
        // Wields both magic and defense buffs, heavy armor/poise, and holy/arcane blade
        val isPaladin = (strength >= 12 && intelligence >= 11 && (vigor >= 12 || endurance >= 12)) ||
            (vigor >= 13 && intelligence >= 12 && strength >= 11)

        if (isPaladin) {
            return CharacterOrigin(
                id = "paladin",
                name = "Paladin",
                title = "Holy Templar of Golden Grace",
                icon = "🛡️✨",
                description = "A consecrated holy champion who blends fortified defense with sacred starlight sorcery. Imbues heavy steel with radiant buffs and shrugs off punishing blows.",
                startingWeapon = Weapons.PALE_ECLIPSE_GREATSWORD,
                vigor = vigor,
                endurance = endurance,
                strength = strength,
                agility = agility,
                intelligence = intelligence,
                startingLevel = startLevel,
                traits = listOf(
                    "🛡️ Holy Aegis: High poise defense, -20% physical damage taken",
                    "✨ Sacred Attunement: Attacks restore both stamina and mana",
                    "🕯️ Grace Blessing: Flask recovery potency increased by +25%"
                ),
                tacticalPlaystyle = "Stand firm against ferocious assaults. Retaliate with heavy magic-infused cleaves and consecrated poise.",
                avatarId = "paladin",
                themeColorHex = "#F59E0B"
            )
        }

        // 2. MAGIC SWORDSMAN: Balanced martial prowess (Strength) and Arcane Sorcery
        val isMagicSwordsman = strength >= 11 && intelligence >= 11 && kotlin.math.abs(strength - intelligence) <= 3

        if (isMagicSwordsman) {
            return CharacterOrigin(
                id = "magic_swordsman",
                name = "Magic Swordsman",
                title = "Weaver of Blade & Starlight",
                icon = "⚔️🔮",
                description = "An agile mystic duelist attuned to both physical combat and astral sorcery. Fluidly interweaves melee slashes with piercing arcane spell strikes.",
                startingWeapon = Weapons.PALE_ECLIPSE_GREATSWORD,
                vigor = vigor,
                endurance = endurance,
                strength = strength,
                agility = agility,
                intelligence = intelligence,
                startingLevel = startLevel,
                traits = listOf(
                    "✨ Spellstrike: Melee attacks ignite with starlight energy",
                    "🔮 Dual Attunement: Damage scales with both physical and mental power",
                    "💧 Font of Aether: Continuous passive FP regeneration"
                ),
                tacticalPlaystyle = "Harass from medium distance with spells, then dash in for devastating magic-empowered sword combos.",
                avatarId = "magic_swordsman",
                themeColorHex = "#38BDF8"
            )
        }

        // 3. NIGHTBLADE: Agility + Intelligence (Shadow magic, critical stealth, frost)
        val isNightblade = agility >= 11 && intelligence >= 11 && kotlin.math.abs(agility - intelligence) <= 3

        if (isNightblade) {
            return CharacterOrigin(
                id = "nightblade",
                name = "Nightblade",
                title = "Phantom of the Twilight Veil",
                icon = "🌙🗡️",
                description = "A lethal shadow-weaver who melds high Agility daggers with cryptic frost sorcery. Strikes from the shadows with chilling precision.",
                startingWeapon = Weapons.OBSIDIAN_DAGGER,
                vigor = vigor,
                endurance = endurance,
                strength = strength,
                agility = agility,
                intelligence = intelligence,
                startingLevel = startLevel,
                traits = listOf(
                    "🌙 Twilight Step: Invulnerable rolls leave icy mist",
                    "❄️ Frostbite Edge: Attacks inflict rapid Frost & Bleed buildup",
                    "✨ Shadow Sorcery: Dagger attacks crit for +40% bonus damage"
                ),
                tacticalPlaystyle = "Slip behind enemies with low-cost rolls, inflict frostbite, and harvest essence with swift critical daggers.",
                avatarId = "nightblade",
                themeColorHex = "#818CF8"
            )
        }

        // 4. RONIN: Quality build (Balanced Strength + Agility)
        val isRonin = strength >= 12 && agility >= 12 && kotlin.math.abs(strength - agility) <= 2

        if (isRonin) {
            return CharacterOrigin(
                id = "ronin",
                name = "Ronin",
                title = "Masterless Wanderer",
                icon = "🎋",
                description = "A masterless wandering swordsman who cast aside clan and crest, roaming the realm with disciplined steel, balance, and deadly iaijutsu poise.",
                startingWeapon = Weapons.NIGHTPROWLER_TALON,
                vigor = vigor,
                endurance = endurance,
                strength = strength,
                agility = agility,
                intelligence = intelligence,
                startingLevel = startLevel,
                traits = listOf(
                    "🎋 Iaijutsu Poise: First strike in a combo deals 2.5x posture damage",
                    "⚡ Fluid Blade: Chains light and heavy attacks without interruption",
                    "⚔️ Dual Scaling: Master of both heavy greatswords and nimble katanas"
                ),
                tacticalPlaystyle = "Switch stances dynamically to adapt to any foe. Out-space opponents with superior reach and timing.",
                avatarId = "ronin",
                themeColorHex = "#10B981"
            )
        }

        // 5. IRON VANGUARD: Pure Defense & Fortitude (High Vigor & Endurance)
        val isTank = (vigor >= 13 && endurance >= 12 && (vigor + endurance >= 26)) || (vigor >= 14 && endurance >= 11)

        if (isTank) {
            return CharacterOrigin(
                id = "vanguard",
                name = "Iron Vanguard",
                title = "Bulwark of the Fallen",
                icon = "🛡️",
                description = "Heavily armored knight with deep vigor and balanced sword mastery. Staggers foes and withstands fierce punishment.",
                startingWeapon = Weapons.STEEL_BROADSWORD,
                vigor = vigor,
                endurance = endurance,
                strength = strength,
                agility = agility,
                intelligence = intelligence,
                startingLevel = startLevel,
                traits = listOf(
                    "🛡️ Iron Bastion: +50 Poise, 15% physical damage reduction",
                    "💥 Crushing Cleave: Heavy broadsword slashes stagger foes",
                    "⚔️ Unyielding: Low stamina cost on defensive recoveries"
                ),
                tacticalPlaystyle = "Absorb brute force with poise. Break enemy guard using punishing heavy cleaves.",
                avatarId = "vanguard",
                themeColorHex = "#F59E0B"
            )
        }

        // 6. WARRIOR / BERSERKER: High Strength dominant
        val isWarrior = strength >= 11 && strength >= agility && strength >= intelligence

        if (isWarrior) {
            return CharacterOrigin(
                id = "executioner",
                name = "Warrior",
                title = "Colossus of the Abyss",
                icon = "🪓",
                description = "Brutal warrior who trades agility for pure destructive force. Smashes through enemy posture with colossal swings.",
                startingWeapon = Weapons.EXECUTIONER_GREATSWORD,
                vigor = vigor,
                endurance = endurance,
                strength = strength,
                agility = agility,
                intelligence = intelligence,
                startingLevel = startLevel,
                traits = listOf(
                    "🩸 Blood Siphon: Stagger visceral attacks heal 12% HP",
                    "🪓 Guard Shatter: Massive posture & poise damage per hit",
                    "💀 Dread Aura: Increased critical strike chance on wounded foes"
                ),
                tacticalPlaystyle = "Unstoppable stagger juggernaut. Execute visceral finishers to siphon vital HP.",
                avatarId = "executioner",
                themeColorHex = "#EF4444"
            )
        }

        // 7. SORCERER: High Intelligence dominant
        val isSorcerer = intelligence >= 11 && intelligence >= strength && intelligence >= agility

        if (isSorcerer) {
            return CharacterOrigin(
                id = "sorcerer",
                name = "Astral Sorcerer",
                title = "Weaver of Starlight",
                icon = "🔮",
                description = "Cosmic scholar attuned to celestial mana. Obliterates dangers from safe range with piercing astral projectiles.",
                startingWeapon = Weapons.ASTRAL_STAFF,
                vigor = vigor,
                endurance = endurance,
                strength = strength,
                agility = agility,
                intelligence = intelligence,
                startingLevel = startLevel,
                traits = listOf(
                    "🔮 Astral Conduit: Fires homing celestial starlight orbs",
                    "💧 Font of Aether: Automatic continuous FP/Mana regeneration",
                    "✨ Cosmic Resonance: Intelligence boosts all projectile spells"
                ),
                tacticalPlaystyle = "Arcane zoning artillery. Rain seeking starlight bolts while mana surges endlessly.",
                avatarId = "sorcerer",
                themeColorHex = "#A855F7"
            )
        }

        // 8. SHADOW SHINOBI: High Agility dominant
        val isShinobi = agility >= 11 && agility >= strength && agility >= intelligence

        if (isShinobi) {
            return CharacterOrigin(
                id = "shinobi",
                name = "Shadow Shinobi",
                title = "Ghost of the Catacombs",
                icon = "🗡️",
                description = "Swift assassin accustomed to the dark. Specializes in rapid multi-hit flurry slashes, nimble rolls, and high Agility scaling.",
                startingWeapon = Weapons.SHADOW_KATANA,
                vigor = vigor,
                endurance = endurance,
                strength = strength,
                agility = agility,
                intelligence = intelligence,
                startingLevel = startLevel,
                traits = listOf(
                    "💨 Ghost Walk: Agile sprint (+15%), Roll stamina cost -20%",
                    "⚡ Flurry Strikes: Katana attacks combo with accelerating speed",
                    "🎯 Shadow Prowess: High Agility weapon damage scaling"
                ),
                tacticalPlaystyle = "Fluidly cancel attacks into acrobatic rolls. Chain rapid bleeds behind enemies.",
                avatarId = "shinobi",
                themeColorHex = "#00E5FF"
            )
        }

        // 9. WANDERER (Default / Evenly balanced)
        return CharacterOrigin(
            id = "wanderer",
            name = "Wanderer",
            title = "The Unshackled Soul",
            icon = "⚜️",
            description = "A resilient traveler with equal distribution across all attributes. Bound by no single discipline, adapting to any weapon or spell found in the shadows.",
            startingWeapon = Weapons.HARDWOOD_CLUB,
            vigor = vigor,
            endurance = endurance,
            strength = strength,
            agility = agility,
            intelligence = intelligence,
            startingLevel = startLevel,
            traits = listOf(
                "🍂 Scavenger's Grit: +35% Flask drop rate from chests & enemies",
                "🗡️ Desperate Edge: +30% attack damage when health is below 40%",
                "🦴 Raw Adaptability: Equal stat distribution for any build path"
            ),
            tacticalPlaystyle = "Resilient abyssal brawler. Adapt to any weapon and strike harder as health depletes.",
            avatarId = "wanderer",
            themeColorHex = "#CBD5E1"
        )
    }
}
