package com.example.shadowescape.animation

data class FrameSocket(
    val torsoXOffset: Float,      // Offset relative to center (0f = center)
    val torsoYOffset: Float,      // Offset relative to ground (positive = upwards)
    val torsoScale: Float = 1.0f, // Scale factor for equipped armor
    val handXOffset: Float,       // Hand socket X offset relative to center
    val handYOffset: Float,       // Hand socket Y offset relative to ground
    val weaponAngleDeg: Float = 0f// Dynamic weapon rotation angle
)

object ModularFrameSockets {

    // 6 Frames of Idle Animation
    val IDLE_SOCKETS: List<FrameSocket> = listOf(
        FrameSocket(torsoXOffset = 0f, torsoYOffset = 48f, handXOffset = 14f, handYOffset = 42f, weaponAngleDeg = -15f),
        FrameSocket(torsoXOffset = 0f, torsoYOffset = 47.5f, handXOffset = 14f, handYOffset = 41.5f, weaponAngleDeg = -15f),
        FrameSocket(torsoXOffset = 0f, torsoYOffset = 47f, handXOffset = 13.5f, handYOffset = 41f, weaponAngleDeg = -16f),
        FrameSocket(torsoXOffset = 0f, torsoYOffset = 46.5f, handXOffset = 13f, handYOffset = 40.5f, weaponAngleDeg = -17f),
        FrameSocket(torsoXOffset = 0f, torsoYOffset = 47f, handXOffset = 13.5f, handYOffset = 41f, weaponAngleDeg = -16f),
        FrameSocket(torsoXOffset = 0f, torsoYOffset = 47.8f, handXOffset = 14f, handYOffset = 41.8f, weaponAngleDeg = -15f)
    )

    // 6 Frames of Run Animation
    val RUN_SOCKETS: List<FrameSocket> = listOf(
        FrameSocket(torsoXOffset = 2f, torsoYOffset = 48f, handXOffset = 18f, handYOffset = 44f, weaponAngleDeg = -30f),
        FrameSocket(torsoXOffset = 4f, torsoYOffset = 50f, handXOffset = 22f, handYOffset = 48f, weaponAngleDeg = -45f),
        FrameSocket(torsoXOffset = 2f, torsoYOffset = 47f, handXOffset = 12f, handYOffset = 42f, weaponAngleDeg = -20f),
        FrameSocket(torsoXOffset = -1f, torsoYOffset = 48f, handXOffset = -6f, handYOffset = 40f, weaponAngleDeg = 15f),
        FrameSocket(torsoXOffset = 0f, torsoYOffset = 50f, handXOffset = 6f, handYOffset = 45f, weaponAngleDeg = -10f),
        FrameSocket(torsoXOffset = 2f, torsoYOffset = 47f, handXOffset = 16f, handYOffset = 43f, weaponAngleDeg = -25f)
    )

    // 6 Frames of Attack Animation (Martial Swing)
    val ATTACK_SOCKETS: List<FrameSocket> = listOf(
        // Frame 0: Anticipation wind-up
        FrameSocket(torsoXOffset = -4f, torsoYOffset = 46f, handXOffset = -16f, handYOffset = 52f, weaponAngleDeg = 55f),
        // Frame 1: Peak backswing
        FrameSocket(torsoXOffset = -6f, torsoYOffset = 47f, handXOffset = -22f, handYOffset = 56f, weaponAngleDeg = 75f),
        // Frame 2: Forward body thrust / lunge
        FrameSocket(torsoXOffset = 3f, torsoYOffset = 45f, handXOffset = 4f, handYOffset = 48f, weaponAngleDeg = 25f),
        // Frame 3: Powerful forward slash impact
        FrameSocket(torsoXOffset = 8f, torsoYOffset = 44f, handXOffset = 26f, handYOffset = 42f, weaponAngleDeg = -45f),
        // Frame 4: Extended follow-through
        FrameSocket(torsoXOffset = 6f, torsoYOffset = 43f, handXOffset = 30f, handYOffset = 34f, weaponAngleDeg = -85f),
        // Frame 5: Recovery to guard
        FrameSocket(torsoXOffset = 2f, torsoYOffset = 46f, handXOffset = 16f, handYOffset = 40f, weaponAngleDeg = -25f)
    )

    // 6 Frames of Jump / Fall / Land
    val JUMP_SOCKETS: List<FrameSocket> = listOf(
        // Jump takeoff
        FrameSocket(torsoXOffset = 1f, torsoYOffset = 46f, handXOffset = 12f, handYOffset = 46f, weaponAngleDeg = -10f),
        // Rising apex
        FrameSocket(torsoXOffset = 2f, torsoYOffset = 52f, handXOffset = 16f, handYOffset = 52f, weaponAngleDeg = -25f),
        // High apex
        FrameSocket(torsoXOffset = 0f, torsoYOffset = 51f, handXOffset = 14f, handYOffset = 50f, weaponAngleDeg = -15f),
        // Falling descent
        FrameSocket(torsoXOffset = -1f, torsoYOffset = 49f, handXOffset = 12f, handYOffset = 46f, weaponAngleDeg = -5f),
        // Landing compression
        FrameSocket(torsoXOffset = 1f, torsoYOffset = 43f, handXOffset = 14f, handYOffset = 36f, weaponAngleDeg = -20f),
        // Landing recovery
        FrameSocket(torsoXOffset = 0f, torsoYOffset = 47f, handXOffset = 14f, handYOffset = 41f, weaponAngleDeg = -15f)
    )
}
