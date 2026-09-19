package com.example.kaizenkanban.ui.i18n

object KanbanNames {
    fun isEisenhowerBoard(name: String?): Boolean {
        val n = name.orEmpty()
        return n.contains("Эйзенхауэр", ignoreCase = true) ||
            n.contains("Eisenhower", ignoreCase = true)
    }

    fun isOkrBoard(name: String?): Boolean {
        val n = name.orEmpty()
        return n.contains("OKR", ignoreCase = true)
    }

    fun isUncategorized(name: String?): Boolean {
        val n = name.orEmpty()
        return n.equals("Без категории", ignoreCase = true) ||
            n.equals("Uncategorized", ignoreCase = true) ||
            n.equals("No category", ignoreCase = true)
    }

    fun isQ1Hub(title: String): Boolean =
        title.contains("Срочно и важно", ignoreCase = true) ||
            title.contains("Urgent and important", ignoreCase = true)

    fun isQ2Hub(title: String): Boolean =
        title.contains("Важно, не срочно", ignoreCase = true) ||
            title.contains("Important, not urgent", ignoreCase = true)

    fun isQ3Hub(title: String): Boolean =
        title.contains("Срочно, не важно", ignoreCase = true) ||
            title.contains("Urgent, not important", ignoreCase = true)

    fun isQ4Hub(title: String): Boolean =
        title.contains("Не срочно и не важно", ignoreCase = true) ||
            title.contains("Not urgent and not important", ignoreCase = true)
}
