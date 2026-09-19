package com.example.kaizenkanban.domain.model

/**
 * Hub titles stored in English keys so [com.example.kaizenkanban.ui.i18n.AppStrings.localized] works.
 */
enum class BoardTemplate {
    INBOX,
    FLOW,
    EISENHOWER;

    fun hubTitles(): List<String> = when (this) {
        INBOX -> listOf("Inbox")
        FLOW -> listOf("Inbox", "Today", "This week", "Actions")
        EISENHOWER -> listOf(
            "Urgent and important",
            "Important, not urgent",
            "Urgent, not important",
            "Not urgent and not important"
        )
    }
}
