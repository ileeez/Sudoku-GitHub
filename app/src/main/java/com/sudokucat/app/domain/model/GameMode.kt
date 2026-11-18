package com.sudokucat.app.domain.model

enum class GameMode {
    CLASSIC,
    GUIDED,
    PHOTO;

    companion object {
        fun from(value: String?): GameMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CLASSIC
    }
}

enum class GameStatus {
    PLAYING,
    WON,
    LOST;

    companion object {
        fun from(value: String?): GameStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PLAYING
    }
}
