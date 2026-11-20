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

    // 6 Frames of Relaxed Natural Idle Animation (Breathing loop, relaxed hanging hands)
    val IDLE_SOCKETS: List<FrameSocket> = listOf(
        FrameSocket(torsoXOffset = -1.3f, torsoYOffset = 51.1f, handXOffset = 12f, handYOffset = 36f, weaponAngleDeg = -15f),
        FrameSocket(torsoXOffset = -1.2f, torsoYOffset = 51.1f, handXOffset = 12f, handYOffset = 36f, weaponAngleDeg = -15f),
        FrameSocket(torsoXOffset = 0.1f,  torsoYOffset = 51.2f, handXOffset = 12.5f, handYOffset = 36.5f, weaponAngleDeg = -16f),
        FrameSocket(torsoXOffset = -1.1f, torsoYOffset = 51.1f, handXOffset = 12f, handYOffset = 36f, weaponAngleDeg = -17f),
        FrameSocket(torsoXOffset = -1.7f, torsoYOffset = 51.1f, handXOffset = 11.5f, handYOffset = 35.5f, weaponAngleDeg = -16f),
        FrameSocket(torsoXOffset = -1.3f, torsoYOffset = 51.1f, handXOffset = 12f, handYOffset = 36f, weaponAngleDeg = -15f)
    )

    // 8 Frames of Athletic Running Sprint Cycle (Natural arm swinging & foot push-off)
    val RUN_SOCKETS: List<FrameSocket> = listOf(
        FrameSocket(torsoXOffset = 1.3f,  torsoYOffset = 51.0f, handXOffset = 16f, handYOffset = 42f, weaponAngleDeg = -25f),
        FrameSocket(torsoXOffset = 2.1f,  torsoYOffset = 51.5f, handXOffset = 19f, handYOffset = 45f, weaponAngleDeg = -35f),
        FrameSocket(torsoXOffset = 3.7f,  torsoYOffset = 51.2f, handXOffset = 21f, handYOffset = 44f, weaponAngleDeg = -40f),
        FrameSocket(torsoXOffset = -1.0f, torsoYOffset = 51.7f, handXOffset = 14f, handYOffset = 38f, weaponAngleDeg = -15f),
        FrameSocket(torsoXOffset = -0.9f, torsoYOffset = 50.8f, handXOffset = 8f,  handYOffset = 36f, weaponAngleDeg = 0f),
        FrameSocket(torsoXOffset = -1.3f, torsoYOffset = 52.1f, handXOffset = 4f,  handYOffset = 39f, weaponAngleDeg = 15f),
        FrameSocket(torsoXOffset = -2.6f, torsoYOffset = 52.3f, handXOffset = 8f,  handYOffset = 41f, weaponAngleDeg = -5f),
        FrameSocket(torsoXOffset = -0.8f, torsoYOffset = 52.3f, handXOffset = 14f, handYOffset = 42f, weaponAngleDeg = -20f)
    )

    // 6 Frames of Attack Animation (Martial Swing)
    val ATTACK_SOCKETS: List<FrameSocket> = listOf(
        // Frame 0: Anticipation wind-up
        FrameSocket(torsoXOffset = -2.9f, torsoYOffset = 53.6f, handXOffset = -16f, handYOffset = 52f, weaponAngleDeg = 55f),
        // Frame 1: Peak backswing
        FrameSocket(torsoXOffset = 0.9f,  torsoYOffset = 51.6f, handXOffset = -22f, handYOffset = 56f, weaponAngleDeg = 75f),
        // Frame 2: Forward body thrust / lunge
        FrameSocket(torsoXOffset = -1.2f, torsoYOffset = 54.2f, handXOffset = 4f,   handYOffset = 48f, weaponAngleDeg = 25f),
        // Frame 3: Powerful forward slash impact
        FrameSocket(torsoXOffset = -1.1f, torsoYOffset = 54.2f, handXOffset = 26f,  handYOffset = 42f, weaponAngleDeg = -45f),
        // Frame 4: Extended follow-through
        FrameSocket(torsoXOffset = 6.5f,  torsoYOffset = 52.8f, handXOffset = 30f,  handYOffset = 34f, weaponAngleDeg = -85f),
        // Frame 5: Recovery to guard
        FrameSocket(torsoXOffset = -1.8f, torsoYOffset = 51.4f, handXOffset = 16f,  handYOffset = 40f, weaponAngleDeg = -25f)
    )

    // 6 Frames of Jump / Fall / Land
    val JUMP_SOCKETS: List<FrameSocket> = listOf(
        // Jump takeoff
        FrameSocket(torsoXOffset = -2.3f, torsoYOffset = 51.5f, handXOffset = 12f, handYOffset = 46f, weaponAngleDeg = -10f),
        // Rising apex
        FrameSocket(torsoXOffset = -1.9f, torsoYOffset = 51.1f, handXOffset = 16f, handYOffset = 52f, weaponAngleDeg = -25f),
        // High apex
        FrameSocket(torsoXOffset = -1.9f, torsoYOffset = 51.6f, handXOffset = 14f, handYOffset = 50f, weaponAngleDeg = -15f),
        // Falling descent
        FrameSocket(torsoXOffset = -0.3f, torsoYOffset = 51.3f, handXOffset = 12f, handYOffset = 46f, weaponAngleDeg = -5f),
        // Landing compression
        FrameSocket(torsoXOffset = -3.9f, torsoYOffset = 51.3f, handXOffset = 14f, handYOffset = 36f, weaponAngleDeg = -20f),
        // Landing recovery
        FrameSocket(torsoXOffset = -1.7f, torsoYOffset = 51.4f, handXOffset = 14f, handYOffset = 41f, weaponAngleDeg = -15f)
    )
}
