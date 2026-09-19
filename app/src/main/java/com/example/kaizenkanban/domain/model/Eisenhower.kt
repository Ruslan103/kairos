package com.example.kaizenkanban.domain.model

object Eisenhower {
    const val Q1 = "Q1" // Срочно и важно
    const val Q2 = "Q2" // Важно, не срочно
    const val Q3 = "Q3" // Срочно, не важно
    const val Q4 = "Q4" // Не срочно и не важно

    fun getTitle(quadrant: String?): String = when (quadrant) {
        Q1 -> "Срочно и важно"
        Q2 -> "Важно, не срочно"
        Q3 -> "Срочно, не важно"
        Q4 -> "Не срочно и не важно"
        else -> ""
    }

    fun getBadge(quadrant: String?): String = when (quadrant) {
        Q1 -> "Q1"
        Q2 -> "Q2"
        Q3 -> "Q3"
        Q4 -> "Q4"
        else -> ""
    }

    fun getShortLabel(quadrant: String?): String = when (quadrant) {
        Q1 -> "Срочно/Важно"
        Q2 -> "Важно"
        Q3 -> "Срочно"
        Q4 -> "Прочее"
        else -> ""
    }

    /** Default berry palette; UI uses [com.example.kaizenkanban.ui.theme.LocalEisenhowerPalette]. */
    fun getColor(quadrant: String?): Long = when (quadrant) {
        Q1 -> 0xFFE11D48
        Q2 -> 0xFF7C3AED
        Q3 -> 0xFFC13584
        Q4 -> 0xFF64748B
        else -> 0xFF8E8E8E
    }

    fun getRank(quadrant: String?): Int = when (quadrant) {
        Q1 -> 0
        Q2 -> 1
        Q3 -> 2
        Q4 -> 3
        else -> 4
    }
}
