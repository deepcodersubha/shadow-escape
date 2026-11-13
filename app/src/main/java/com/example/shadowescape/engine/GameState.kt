package com.example.shadowescape.engine

enum class GameStateMode {
    PLAYING,
    PAUSED,
    GAME_OVER,
    VICTORY
}

class GameState {
    var mode: GameStateMode = GameStateMode.PLAYING
    var score: Int = 0
}
