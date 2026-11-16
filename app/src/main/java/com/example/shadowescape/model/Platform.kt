package com.example.shadowescape.model

import com.example.shadowescape.physics.AABB

enum class PlatformType {
    NORMAL,
    ONE_WAY,
    HAZARD,
    SWITCH,
    DOOR,
    EXIT,
    PRESSURE_PLATE,
    CHECKPOINT
}

class Platform(
    val id: Int,
    val bounds: AABB,
    val type: PlatformType,
    var isActivated: Boolean = false,
    val isGraceMimic: Boolean = false
) {
    var isMimicAwakened: Boolean = false
    var isMimicDefeated: Boolean = false

    fun updateState(activated: Boolean) {
        if (type == PlatformType.SWITCH || type == PlatformType.DOOR || type == PlatformType.PRESSURE_PLATE || type == PlatformType.CHECKPOINT) {
            isActivated = activated
        }
    }
}
