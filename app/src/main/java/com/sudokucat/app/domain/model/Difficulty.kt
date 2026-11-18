package com.sudokucat.app.domain.model

enum class Difficulty(
    val label: String,
    val shortLabel: String,
    val targetClues: Int,
    val xpReward: Int,
    val description: String
) {
    EASY("Facile", "Facile", 40, 60, "Per rilassarsi e prendere confidenza"),
    MEDIUM("Medio", "Medio", 34, 100, "Il classico equilibrio tra logica e sfida"),
    HARD("Difficile", "Difficile", 29, 150, "Serve pazienza e sguardo attento"),
    EXPERT("Esperto", "Esperto", 25, 220, "Solo per veri detective dei numeri");

    companion object {
        fun from(value: String?): Difficulty =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
    }
}
