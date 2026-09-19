package com.example.kaizenkanban.ui

import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.model.Board
import com.example.kaizenkanban.domain.model.Column

data class QuickAddTarget(
    val board: Board,
    val column: Column,
    val usesPrimaryBoardDefault: Boolean
)

/** Clears board/hub prefs that point at deleted or archived targets. */
fun sanitizeQuickAddPrefs(
    prefs: KairosPreferences,
    boards: List<Board>,
    columns: List<Column>
) {
    val activeBoards = boards.filter { !it.isArchived }
    val preferredBoardId = prefs.quickAddBoardId
    if (preferredBoardId != null && activeBoards.none { it.id == preferredBoardId }) {
        prefs.quickAddBoardId = null
        prefs.quickAddColumnId = null
        return
    }
    val board = preferredBoardId?.let { id -> activeBoards.find { it.id == id } }
        ?: activeBoards.find { it.isDefault }
        ?: activeBoards.firstOrNull()
        ?: return
    val boardColumns = columns.filter { it.boardId == board.id }
    val preferredColumnId = prefs.quickAddColumnId
    if (preferredColumnId != null && boardColumns.none { it.id == preferredColumnId }) {
        prefs.quickAddColumnId = null
    }
}

fun resolveQuickAddTarget(
    prefs: KairosPreferences,
    boards: List<Board>,
    columns: List<Column>
): QuickAddTarget? {
    val activeBoards = boards.filter { !it.isArchived }
    if (activeBoards.isEmpty()) return null

    val preferredBoardId = prefs.quickAddBoardId
    val preferredBoard = preferredBoardId?.let { id -> activeBoards.find { it.id == id } }
    val primaryBoard = activeBoards.find { it.isDefault }
    val board = preferredBoard ?: primaryBoard ?: activeBoards.first()
    val usesPrimaryDefault = preferredBoardId.isNullOrBlank()

    val boardColumns = columns.filter { it.boardId == board.id }.sortedBy { it.position }
    if (boardColumns.isEmpty()) return null

    val preferredColumnId = prefs.quickAddColumnId
    val column = preferredColumnId
        ?.let { id -> boardColumns.find { it.id == id } }
        ?: boardColumns.first()

    return QuickAddTarget(
        board = board,
        column = column,
        usesPrimaryBoardDefault = usesPrimaryDefault
    )
}
