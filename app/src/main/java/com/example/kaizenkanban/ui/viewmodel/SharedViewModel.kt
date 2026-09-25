package com.example.kaizenkanban.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kaizenkanban.data.transfer.KairosTransferHelper
import com.example.kaizenkanban.domain.TaskCompletionGate
import com.example.kaizenkanban.domain.model.*
import com.example.kaizenkanban.domain.repository.KanbanRepository
import com.example.kaizenkanban.domain.usecase.*
import com.example.kaizenkanban.ui.i18n.KanbanNames
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class AppState(
    val projects: List<Project> = emptyList(),
    val boards: List<Board> = emptyList(),
    val categories: List<Category> = emptyList(),
    val columns: List<Column> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val columnComments: List<ColumnComment> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val recurringTemplates: List<RecurringTemplate> = emptyList(),
    val taskLinks: List<TaskLink> = emptyList(),
    val statsJournal: List<StatsJournalEntry> = emptyList(),
    val isInitialized: Boolean = false
)

private data class DeletedTaskSnapshot(
    val task: Task,
    val comments: List<Comment>,
    val links: List<TaskLink> = emptyList()
)

private data class CoreKanbanData(
    val projects: List<Project>,
    val boards: List<Board>,
    val categories: List<Category>,
    val columns: List<Column>,
    val tasks: List<Task>
)

class SharedViewModel(
    private val repository: KanbanRepository,
    private val initializeDatabaseUseCase: InitializeDatabaseUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val moveTaskUseCase: MoveTaskUseCase,
    private val addColumnUseCase: AddColumnUseCase,
    private val deleteColumnUseCase: DeleteColumnUseCase,
    private val renameColumnUseCase: RenameColumnUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val setDefaultBoardUseCase: SetDefaultBoardUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val addColumnCommentUseCase: AddColumnCommentUseCase,
    private val deleteColumnCommentUseCase: DeleteColumnCommentUseCase,
    private val moveTaskToBoardUseCase: MoveTaskToBoardUseCase,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val ensureRecurringInstancesUseCase: EnsureRecurringInstancesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    
    var hasAutoNavigated = false
    private val _pendingOpenTaskId = MutableStateFlow<String?>(null)
    val pendingOpenTaskId: StateFlow<String?> = _pendingOpenTaskId.asStateFlow()
    private val deletedSnapshots = mutableMapOf<String, DeletedTaskSnapshot>()
    private val taskMutationMutex = Mutex()

    /** Completion gate first, then mutation mutex — same order everywhere to avoid deadlocks / stale writes. */
    private suspend inline fun <T> withAllTaskLocks(crossinline block: suspend () -> T): T =
        TaskCompletionGate.mutex.withLock {
            taskMutationMutex.withLock { block() }
        }

    fun requestOpenTask(taskId: String) {
        if (taskId.isNotBlank()) {
            _pendingOpenTaskId.value = taskId
        }
    }

    fun clearPendingOpenTask() {
        _pendingOpenTaskId.value = null
    }

    private val _pendingQuickAdd = MutableStateFlow(false)
    val pendingQuickAdd: StateFlow<Boolean> = _pendingQuickAdd.asStateFlow()

    private val _pendingVoiceAssistant = MutableStateFlow(false)
    val pendingVoiceAssistant: StateFlow<Boolean> = _pendingVoiceAssistant.asStateFlow()

    private val _pendingAddTask = MutableStateFlow(false)
    val pendingAddTask: StateFlow<Boolean> = _pendingAddTask.asStateFlow()

    fun requestQuickAdd() {
        _pendingQuickAdd.value = true
    }

    fun requestVoiceAssistant() {
        _pendingVoiceAssistant.value = true
    }

    fun requestAddTask() {
        _pendingAddTask.value = true
    }

    /** Returns true once if a voice request was pending (clears the flag). */
    fun consumePendingVoiceAssistant(): Boolean {
        if (!_pendingVoiceAssistant.value) return false
        _pendingVoiceAssistant.value = false
        return true
    }

    fun clearPendingVoiceAssistant() {
        _pendingVoiceAssistant.value = false
    }

    fun consumePendingAddTask(): Boolean {
        if (!_pendingAddTask.value) return false
        _pendingAddTask.value = false
        return true
    }

    fun clearPendingQuickAdd() {
        _pendingQuickAdd.value = false
    }

    private data class CompletedUndo(
        val snapshot: Task,
        val completedAt: Long,
        val spawnedTaskId: String?
    )

    private val completedUndos = LinkedHashMap<String, CompletedUndo>()

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            completeOrReopen(task)
        }
    }

    /** Returns true if the task was marked done (caller may show undo). */
    suspend fun completeOrReopen(task: Task): Boolean = withAllTaskLocks {
        // Read DB under the shared gate so widget/notification spawns are visible.
        val latest = repository.getAllTasks().first().find { it.id == task.id }
        if (latest == null) {
            false
        } else {
            val willBeCompleted = !latest.isCompleted
            if (willBeCompleted) {
                val completedAt = System.currentTimeMillis()
                updateTaskUseCase(
                    latest.copy(
                        isCompleted = true,
                        completedAt = completedAt,
                        workflowStatus = TaskWorkflow.DONE
                    )
                )
                // Legacy repeatRule auto-spawn disabled (recurring templates replace it).
                completedUndos[latest.id] = CompletedUndo(latest, completedAt, spawnedTaskId = null)
                trimCompletedUndos()
            } else {
                completedUndos.remove(latest.id)
                updateTaskUseCase(
                    latest.copy(
                        isCompleted = false,
                        completedAt = null,
                        workflowStatus = TaskWorkflow.OPEN,
                        completionQuality = null
                    )
                )
            }
            willBeCompleted
        }
    }

    fun markTaskNotDone(task: Task) {
        viewModelScope.launch {
            withAllTaskLocks {
                val latest = repository.getAllTasks().first().find { it.id == task.id } ?: return@withAllTaskLocks
                updateTaskUseCase(
                    latest.copy(
                        isCompleted = false,
                        completedAt = null,
                        workflowStatus = TaskWorkflow.NOT_DONE,
                        completionQuality = null
                    )
                )
            }
        }
    }

    fun clearTaskNotDone(task: Task) {
        viewModelScope.launch {
            withAllTaskLocks {
                val latest = repository.getAllTasks().first().find { it.id == task.id } ?: return@withAllTaskLocks
                if (latest.workflowStatus != TaskWorkflow.NOT_DONE) return@withAllTaskLocks
                updateTaskUseCase(latest.copy(workflowStatus = TaskWorkflow.OPEN))
            }
        }
    }

    fun setTaskCompletionQuality(taskId: String, quality: Int?) {
        viewModelScope.launch {
            withAllTaskLocks {
                val latest = repository.getAllTasks().first().find { it.id == taskId } ?: return@withAllTaskLocks
                if (!latest.isCompleted) return@withAllTaskLocks
                updateTaskUseCase(
                    latest.copy(completionQuality = quality?.coerceIn(1, 5))
                )
            }
        }
    }

    fun undoComplete(taskId: String) {
        viewModelScope.launch {
            withAllTaskLocks {
                val undo = completedUndos.remove(taskId) ?: return@withAllTaskLocks
                updateTaskUseCase(
                    undo.snapshot.copy(
                        isCompleted = false,
                        completedAt = null,
                        workflowStatus = TaskWorkflow.OPEN,
                        completionQuality = null
                    )
                )
                undo.spawnedTaskId?.let { deleteTaskUseCase(it) }
            }
        }
    }

    private fun trimCompletedUndos(maxEntries: Int = 20) {
        while (completedUndos.size > maxEntries) {
            val oldest = completedUndos.keys.firstOrNull() ?: break
            completedUndos.remove(oldest)
        }
    }

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                initializeDatabaseUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            combine(
                repository.getProjects(),
                repository.getAllBoards(),
                repository.getCategories(),
                repository.getAllColumns(),
                repository.getAllTasks()
            ) { projects, boards, categories, columns, tasks ->
                CoreKanbanData(projects, boards, categories, columns, tasks)
            }.combine(repository.getAllComments()) { core, comments ->
                Pair(core, comments)
            }.combine(repository.getAllColumnComments()) { (core, comments), colComments ->
                Triple(core, comments, colComments)
            }.combine(repository.getAllContacts()) { triple, contacts ->
                val (core, comments, colComments) = triple
                Pair(Triple(core, comments, colComments), contacts)
            }.combine(repository.getAllRecurringTemplates()) { pair, templates ->
                Pair(pair, templates)
            }.combine(repository.getAllTaskLinks()) { pair, links ->
                Pair(pair, links)
            }.combine(repository.getAllStatsJournal()) { pair, journal ->
                val (triplePair, links) = pair
                val (inner, templates) = triplePair
                val (triple, contacts) = inner
                val (core, comments, colComments) = triple
                AppState(
                    projects = core.projects,
                    boards = core.boards,
                    categories = core.categories,
                    columns = core.columns,
                    tasks = core.tasks,
                    comments = comments,
                    columnComments = colComments,
                    contacts = contacts,
                    recurringTemplates = templates,
                    taskLinks = links,
                    statsJournal = journal,
                    isInitialized = true
                )
            }.collect { fullState ->
                _state.value = fullState
            }
        }

        viewModelScope.launch {
            state
                .map { it.isInitialized }
                .distinctUntilChanged()
                .collect { ready ->
                    if (ready) ensureTodayRecurringInstances()
                }
        }
    }

    fun ensureTodayRecurringInstances() {
        viewModelScope.launch {
            val s = _state.value
            ensureRecurringInstancesUseCase(
                templates = s.recurringTemplates,
                tasks = s.tasks,
                columns = s.columns
            )
        }
    }

    fun upsertRecurringTemplate(template: RecurringTemplate) {
        viewModelScope.launch {
            val existing = _state.value.recurringTemplates.any { it.id == template.id }
            if (existing) repository.updateRecurringTemplate(template)
            else repository.insertRecurringTemplate(template)
            ensureTodayRecurringInstances()
        }
    }

    fun setRecurringTemplateEnabled(templateId: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = _state.value.recurringTemplates.find { it.id == templateId } ?: return@launch
            repository.updateRecurringTemplate(current.copy(enabled = enabled))
            if (enabled) ensureTodayRecurringInstances()
        }
    }

    fun deleteRecurringTemplate(templateId: String) {
        viewModelScope.launch {
            repository.deleteRecurringTemplate(templateId)
        }
    }

    /** Returns false if the link would create a cycle (or is invalid). */
    fun addTaskLink(parentId: String, childId: String): Boolean {
        val links = _state.value.taskLinks
        if (TaskLinkGraph.wouldCreateCycle(links, parentId, childId)) return false
        if (links.any { it.parentId == parentId && it.childId == childId }) return true
        viewModelScope.launch {
            repository.insertTaskLink(
                TaskLink(
                    parentId = parentId,
                    childId = childId,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        return true
    }

    fun removeTaskLink(parentId: String, childId: String) {
        viewModelScope.launch {
            repository.deleteTaskLink(parentId, childId)
        }
    }

    fun wouldCreateTaskLinkCycle(parentId: String, childId: String): Boolean =
        TaskLinkGraph.wouldCreateCycle(_state.value.taskLinks, parentId, childId)


    // Projects
    fun addProject(name: String) {
        viewModelScope.launch {
            val nextPos = (_state.value.projects.maxOfOrNull { it.position } ?: -1) + 1
            repository.insertProject(Project(UUID.randomUUID().toString(), name, nextPos))
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            if (!canDeleteProject(id)) return@launch
            val hasProtected = _state.value.boards.any { it.projectId == id && isProtectedBoard(it) }
            if (hasProtected) return@launch
            repository.deleteProject(id)
        }
    }

    fun addContact(projectId: String, name: String, phone: String, email: String, role: String) {
        viewModelScope.launch {
            val nextPos = (_state.value.contacts
                .filter { it.projectId == projectId }
                .maxOfOrNull { it.position } ?: -1) + 1
            repository.insertContact(
                Contact(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    name = name.trim(),
                    phone = phone.trim(),
                    email = email.trim(),
                    role = role.trim(),
                    position = nextPos
                )
            )
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch { repository.insertContact(contact) }
    }

    fun deleteContact(id: String) {
        viewModelScope.launch { repository.deleteContact(id) }
    }
    
    fun renameProject(id: String, newName: String) {
        viewModelScope.launch { repository.renameProject(id, newName) }
    }

    fun moveProject(projectId: String, direction: Int) {
        viewModelScope.launch {
            val currentProjects = _state.value.projects.sortedBy { it.position }.toMutableList()
            val index = currentProjects.indexOfFirst { it.id == projectId }
            val targetIndex = index + direction
            if (index != -1 && targetIndex in currentProjects.indices) {
                val item = currentProjects.removeAt(index)
                currentProjects.add(targetIndex, item)
                val updated = currentProjects.mapIndexed { idx, p -> p.copy(position = idx) }
                repository.insertProjects(updated)
            }
        }
    }

    fun reorderProjects(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentProjects = _state.value.projects.sortedBy { it.position }.toMutableList()
            if (fromIndex in currentProjects.indices && toIndex in currentProjects.indices && fromIndex != toIndex) {
                val item = currentProjects.removeAt(fromIndex)
                currentProjects.add(toIndex, item)
                val updated = currentProjects.mapIndexed { idx, p -> p.copy(position = idx) }
                repository.insertProjects(updated)
            }
        }
    }

    // Boards
    fun addBoard(projectId: String, name: String, template: BoardTemplate = BoardTemplate.INBOX) {
        viewModelScope.launch {
            val board = Board(UUID.randomUUID().toString(), projectId, name)
            repository.insertBoard(board)
            template.hubTitles().forEachIndexed { index, title ->
                repository.insertColumn(
                    Column(
                        id = UUID.randomUUID().toString(),
                        boardId = board.id,
                        title = title,
                        position = index
                    )
                )
            }
        }
    }

    fun deleteBoard(id: String) {
        viewModelScope.launch {
            val board = _state.value.boards.find { it.id == id } ?: return@launch
            if (isProtectedBoard(board)) return@launch
            repository.deleteBoard(id)
        }
    }

    fun isProtectedBoard(board: Board): Boolean {
        if (board.isDefault) return true
        val n = board.name
        return KanbanNames.isOkrBoard(n) ||
            KanbanNames.isEisenhowerBoard(n)
    }

    fun canDeleteProject(projectId: String): Boolean =
        _state.value.projects.size > 1 &&
            _state.value.projects.none { it.id == projectId && it.position == 0 } &&
            _state.value.boards.none { it.projectId == projectId && isProtectedBoard(it) }

    fun renameBoard(id: String, newName: String) {
        viewModelScope.launch { repository.renameBoard(id, newName) }
    }
    
    fun setDefaultBoard(id: String) {
        viewModelScope.launch { setDefaultBoardUseCase(id) }
    }

    fun clearDefaultBoard() {
        viewModelScope.launch {
            val boards = _state.value.boards.filter { !it.isArchived }
            val current = boards.find { it.isDefault }
            repository.clearDefaultBoards()
            val candidates = boards.filter { it.id != current?.id }
            val next = candidates.firstOrNull {
                current != null &&
                    it.projectId == current.projectId &&
                    KanbanNames.isOkrBoard(it.name)
            }
                ?: candidates.firstOrNull { current != null && it.projectId == current.projectId }
                ?: candidates.firstOrNull { KanbanNames.isOkrBoard(it.name) }
                ?: candidates.firstOrNull()
                ?: current
            next?.let { repository.setDefaultBoard(it.id) }
        }
    }

    fun setBoardArchived(id: String, archived: Boolean) {
        viewModelScope.launch { repository.setBoardArchived(id, archived) }
    }

    // Categories
    fun addCategory(name: String, color: Long) {
        viewModelScope.launch {
            repository.insertCategory(Category(UUID.randomUUID().toString(), name, color))
        }
    }
    
    fun updateCategory(category: Category) {
        viewModelScope.launch { repository.updateCategory(category) }
    }
    
    fun deleteCategory(id: String) {
        viewModelScope.launch {
            val affected = _state.value.tasks.filter { it.categoryId == id }
            if (affected.isNotEmpty()) {
                repository.updateTasks(affected.map { it.copy(categoryId = null) })
            }
            repository.deleteCategory(id)
        }
    }

    // Board Data specific
    fun getColumnsForBoard(boardId: String): Flow<List<Column>> = repository.getColumnsByBoard(boardId)

    fun addTask(
        title: String,
        columnId: String,
        categoryId: String?,
        dueDate: Long?,
        currentTasks: List<Task>,
        eisenhowerQuadrant: String? = null,
        showEisenhowerButtons: Boolean = true,
        reminderMinutesOfDay: Int? = null,
        complexity: Int? = null,
        estimatedMinutes: Int? = null,
        parentIds: List<String> = emptyList(),
        childIds: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val taskId = addTaskUseCase(
                title,
                columnId,
                categoryId,
                dueDate,
                currentTasks,
                eisenhowerQuadrant,
                showEisenhowerButtons,
                reminderMinutesOfDay = reminderMinutesOfDay,
                complexity = complexity,
                estimatedMinutes = estimatedMinutes
            ) ?: return@launch
            val working = _state.value.taskLinks.toMutableList()
            parentIds.distinct().forEach { parentId ->
                if (parentId == taskId) return@forEach
                if (TaskLinkGraph.wouldCreateCycle(working, parentId, taskId)) return@forEach
                val link = TaskLink(
                    parentId = parentId,
                    childId = taskId,
                    createdAt = System.currentTimeMillis()
                )
                repository.insertTaskLink(link)
                working += link
            }
            childIds.distinct().forEach { childId ->
                if (childId == taskId) return@forEach
                if (TaskLinkGraph.wouldCreateCycle(working, taskId, childId)) return@forEach
                val link = TaskLink(
                    parentId = taskId,
                    childId = childId,
                    createdAt = System.currentTimeMillis()
                )
                repository.insertTaskLink(link)
                working += link
            }
        }
    }

    fun setTaskQuadrant(task: Task, quadrant: String?) {
        viewModelScope.launch {
            withAllTaskLocks {
                val latest = _state.value.tasks.find { it.id == task.id } ?: return@withAllTaskLocks
                updateTaskUseCase(latest.copy(eisenhowerQuadrant = quadrant))
            }
        }
    }

    fun moveTask(
        task: Task,
        newColumnId: String,
        newPosition: Int,
        allTasks: List<Task>,
        changeHome: Boolean = false,
        fromBoardId: String? = null,
        targetVisibleOrder: List<Task>? = null
    ) {
        viewModelScope.launch {
            withAllTaskLocks {
                val latest = _state.value.tasks.find { it.id == task.id } ?: task
                // Keep fresher links from state; apply quadrant/UI flags from the drag payload.
                relocateTaskAppearance(
                    latest.copy(
                        eisenhowerQuadrant = task.eisenhowerQuadrant,
                        showEisenhowerButtons = task.showEisenhowerButtons
                    ),
                    newColumnId,
                    newPosition,
                    _state.value.tasks,
                    changeHome,
                    fromBoardId,
                    targetVisibleOrder
                )
            }
        }
    }

    /** Apply hub sort: rewrite home-task positions for [columnId] to [orderedActiveIds]. */
    fun reorderHubHomeTasks(columnId: String, orderedActiveIds: List<String>) {
        viewModelScope.launch {
            withAllTaskLocks {
                moveTaskUseCase.reorderHomeTasks(columnId, orderedActiveIds, _state.value.tasks)
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            withAllTaskLocks {
                updateTaskUseCase(task)
            }
        }
    }

    fun toggleTaskHidden(task: Task) {
        viewModelScope.launch {
            withAllTaskLocks {
                val latest = _state.value.tasks.find { it.id == task.id } ?: task
                updateTaskUseCase(latest.copy(isHidden = !latest.isHidden))
            }
        }
    }

    fun clearCompletedTasks(columnId: String) {
        viewModelScope.launch {
            completedTasksInHub(columnId).forEach { task ->
                archiveTaskOffHub(task, columnId, completed = true)
            }
        }
    }

    fun clearNotDoneTasks(columnId: String) {
        viewModelScope.launch {
            notDoneTasksInHub(columnId).forEach { task ->
                archiveTaskOffHub(task, columnId, completed = false)
            }
        }
    }

    private fun completedTasksInHub(columnId: String): List<Task> {
        val columns = _state.value.columns
        val boards = _state.value.boards
        val column = columns.find { it.id == columnId } ?: return emptyList()
        val board = boards.find { it.id == column.boardId }
        val isEisenhower = isEisenhowerBoardName(board?.name)
        val boardCols = columns.filter { it.boardId == column.boardId }.sortedBy { it.position }
        val index = boardCols.indexOfFirst { it.id == columnId }
        val quadrant = if (isEisenhower && index >= 0) quadrantForHub(column, index) else null
        return _state.value.tasks.filter { task ->
            if (!task.isCompleted || task.isBoardArchived) return@filter false
            if (isEisenhower && quadrant != null) {
                val homeProjectId = columns.find { it.id == task.columnId }
                    ?.let { col -> boards.find { it.id == col.boardId }?.projectId }
                (homeProjectId == null || homeProjectId == board?.projectId) && (
                    (task.columnId == columnId && task.eisenhowerQuadrant == null) ||
                        task.eisenhowerQuadrant == quadrant ||
                        task.linkedColumnIds.contains(columnId)
                    )
            } else {
                task.isOnColumn(columnId)
            }
        }
    }

    private fun notDoneTasksInHub(columnId: String): List<Task> {
        val columns = _state.value.columns
        val boards = _state.value.boards
        val column = columns.find { it.id == columnId } ?: return emptyList()
        val board = boards.find { it.id == column.boardId }
        val isEisenhower = isEisenhowerBoardName(board?.name)
        val boardCols = columns.filter { it.boardId == column.boardId }.sortedBy { it.position }
        val index = boardCols.indexOfFirst { it.id == columnId }
        val quadrant = if (isEisenhower && index >= 0) quadrantForHub(column, index) else null
        return _state.value.tasks.filter { task ->
            if (!task.isNotDone || task.isBoardArchived) return@filter false
            if (isEisenhower && quadrant != null) {
                val homeProjectId = columns.find { it.id == task.columnId }
                    ?.let { col -> boards.find { it.id == col.boardId }?.projectId }
                (homeProjectId == null || homeProjectId == board?.projectId) && (
                    (task.columnId == columnId && task.eisenhowerQuadrant == null) ||
                        task.eisenhowerQuadrant == quadrant ||
                        task.linkedColumnIds.contains(columnId)
                    )
            } else {
                task.isOnColumn(columnId)
            }
        }
    }

    private suspend fun archiveTaskOffHub(task: Task, columnId: String, completed: Boolean) {
        val columns = _state.value.columns
        val boards = _state.value.boards
        val column = columns.find { it.id == columnId } ?: return
        val board = boards.find { it.id == column.boardId }
        val isEisenhower = isEisenhowerBoardName(board?.name)
        val boardCols = columns.filter { it.boardId == column.boardId }.sortedBy { it.position }
        val index = boardCols.indexOfFirst { it.id == columnId }
        val quadrant = if (isEisenhower && index >= 0) quadrantForHub(column, index) else null

        if (task.columnId == columnId) {
            val otherLinks = task.linkedColumnIds.filter { it != columnId }
            when {
                otherLinks.isEmpty() -> {
                    updateTaskUseCase(
                        task.copy(
                            isBoardArchived = true,
                            linkedColumnIds = emptyList(),
                            eisenhowerQuadrant = if (completed) null else task.eisenhowerQuadrant
                        )
                    )
                }
                else -> {
                    val newHome = otherLinks.first()
                    moveTaskUseCase(
                        task.copy(
                            linkedColumnIds = otherLinks.drop(1),
                            isBoardArchived = true,
                            isCompleted = completed || task.isCompleted
                        ),
                        newHome,
                        _state.value.tasks.count { it.columnId == newHome && it.id != task.id },
                        _state.value.tasks
                    )
                }
            }
            return
        }
        var updated = task
        var changed = false
        if (task.linkedColumnIds.contains(columnId)) {
            updated = updated.copy(linkedColumnIds = updated.linkedColumnIds.filter { it != columnId })
            changed = true
        }
        if (isEisenhower && quadrant != null && task.eisenhowerQuadrant == quadrant) {
            updated = updated.copy(eisenhowerQuadrant = null)
            changed = true
        }
        // If no remaining board presence for this appearance-only clear, still leave task;
        // full archive when home was cleared above.
        if (changed) updateTaskUseCase(updated)
    }

    fun resetGoalStatsEpoch(goalId: String) {
        viewModelScope.launch {
            withAllTaskLocks {
                val latest = _state.value.tasks.find { it.id == goalId } ?: return@withAllTaskLocks
                if (!latest.isGoal) return@withAllTaskLocks
                updateTaskUseCase(latest.copy(goalStatsEpochMillis = System.currentTimeMillis()))
            }
        }
    }

    fun setTaskStatsExcluded(taskId: String, excluded: Boolean) {
        viewModelScope.launch {
            withAllTaskLocks {
                val latest = _state.value.tasks.find { it.id == taskId } ?: return@withAllTaskLocks
                updateTaskUseCase(latest.copy(statsExcluded = excluded))
            }
        }
    }

    fun deleteArchivedTaskToJournal(taskId: String, projectId: String) {
        viewModelScope.launch {
            withAllTaskLocks {
                val task = _state.value.tasks.find { it.id == taskId } ?: return@withAllTaskLocks
                val kind = when {
                    task.isCompleted -> StatsJournalEntry.KIND_DONE
                    task.isNotDone -> StatsJournalEntry.KIND_NOT_DONE
                    else -> null
                }
                val projectTasks = projectTasksFor(projectId)
                if (!task.statsExcluded && kind != null) {
                    val entry = com.example.kaizenkanban.domain.stats.ProjectStatsCalculator.journalFromTask(
                        task = task,
                        projectId = projectId,
                        links = _state.value.taskLinks,
                        projectTasks = projectTasks,
                        kind = kind
                    )
                    repository.insertStatsJournal(entry)
                }
                deleteTaskUseCase(task.id)
            }
        }
    }

    /** Journal all archived tasks (that count in stats), then delete them from Archive. */
    fun clearArchiveToJournal(projectId: String) {
        viewModelScope.launch {
            withAllTaskLocks {
                val projectTasks = projectTasksFor(projectId)
                val archived = projectTasks.filter { it.isBoardArchived }
                if (archived.isEmpty()) return@withAllTaskLocks
                val links = _state.value.taskLinks
                archived.forEach { task ->
                    if (!task.statsExcluded) {
                        val kind = when {
                            task.isCompleted -> StatsJournalEntry.KIND_DONE
                            task.isNotDone -> StatsJournalEntry.KIND_NOT_DONE
                            else -> null
                        }
                        if (kind != null) {
                            val entry = com.example.kaizenkanban.domain.stats.ProjectStatsCalculator.journalFromTask(
                                task = task,
                                projectId = projectId,
                                links = links,
                                projectTasks = projectTasks,
                                kind = kind
                            )
                            repository.insertStatsJournal(entry)
                        }
                    }
                    deleteTaskUseCase(task.id)
                }
            }
        }
    }

    private fun projectTasksFor(projectId: String): List<Task> {
        val boardIds = _state.value.boards.filter { it.projectId == projectId }.map { it.id }.toSet()
        val columnIds = _state.value.columns.filter { it.boardId in boardIds }.map { it.id }.toSet()
        return _state.value.tasks.filter { it.columnId in columnIds }
    }

    fun addColumn(title: String, boardId: String, currentColumns: List<Column>) {
        viewModelScope.launch {
            addColumnUseCase(title, boardId, currentColumns)
        }
    }

    fun deleteColumn(columnId: String) {
        viewModelScope.launch {
            val column = _state.value.columns.find { it.id == columnId } ?: return@launch
            val hubsOnBoard = _state.value.columns.count { it.boardId == column.boardId }
            if (hubsOnBoard <= 1) return@launch
            deleteColumnUseCase(columnId)
        }
    }

    fun renameColumn(columnId: String, newTitle: String) {
        viewModelScope.launch { renameColumnUseCase(columnId, newTitle) }
    }

    fun moveColumn(columnId: String, direction: Int, boardId: String) {
        viewModelScope.launch {
            val boardColumns = _state.value.columns
                .filter { it.boardId == boardId }
                .sortedBy { it.position }
                .toMutableList()
            val index = boardColumns.indexOfFirst { it.id == columnId }
            val targetIndex = index + direction
            if (index != -1 && targetIndex in boardColumns.indices) {
                val columnToMove = boardColumns.removeAt(index)
                boardColumns.add(targetIndex, columnToMove)
                val updated = boardColumns.mapIndexed { idx, col -> col.copy(position = idx) }
                repository.insertColumns(updated)
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            withAllTaskLocks {
                val task = _state.value.tasks.find { it.id == taskId } ?: return@withAllTaskLocks
                deletedSnapshots[taskId] = DeletedTaskSnapshot(
                    task = task,
                    comments = _state.value.comments.filter { it.taskId == taskId },
                    links = _state.value.taskLinks.filter { it.parentId == taskId || it.childId == taskId }
                )
                deleteTaskUseCase(taskId)
            }
        }
    }

    fun undoDelete(taskId: String) {
        viewModelScope.launch {
            withAllTaskLocks {
                val snapshot = deletedSnapshots.remove(taskId) ?: return@withAllTaskLocks
                repository.insertTask(snapshot.task)
                snapshot.comments.forEach { repository.insertComment(it) }
                snapshot.links.forEach { repository.insertTaskLink(it) }
            }
        }
    }

    fun discardDeletedSnapshot(taskId: String) {
        deletedSnapshots.remove(taskId)
    }

    // Comments
    fun addComment(taskId: String, text: String) {
        viewModelScope.launch { addCommentUseCase(taskId, text) }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch { deleteCommentUseCase(commentId) }
    }

    // Column Comments (Hub comments)
    fun addColumnComment(columnId: String, text: String) {
        viewModelScope.launch { addColumnCommentUseCase(columnId, text) }
    }

    fun deleteColumnComment(commentId: String) {
        viewModelScope.launch { deleteColumnCommentUseCase(commentId) }
    }

    // Move task across boards
    fun moveTaskToBoard(
        task: Task,
        targetBoardId: String,
        targetColumnId: String? = null,
        fromBoardId: String? = null
    ) {
        viewModelScope.launch {
            withAllTaskLocks {
                val latest = _state.value.tasks.find { it.id == task.id } ?: task
                val targetCols = _state.value.columns.filter { it.boardId == targetBoardId }.sortedBy { it.position }
                val targetCol = targetColumnId?.let { id -> targetCols.find { it.id == id } } ?: targetCols.firstOrNull() ?: return@withAllTaskLocks
                val position = _state.value.tasks.count { it.columnId == targetCol.id }
                relocateTaskAppearance(
                    latest,
                    targetCol.id,
                    position,
                    _state.value.tasks,
                    changeHome = true,
                    fromBoardId = fromBoardId
                )
            }
        }
    }

    // Mirror the same task onto another hub/board (like Eisenhower buttons)
    fun copyTaskToColumn(task: Task, targetColumnId: String) {
        viewModelScope.launch {
            withAllTaskLocks {
                linkTaskToColumn(task, targetColumnId)
            }
        }
    }

    fun copyTaskToBoard(task: Task, targetBoardId: String, targetColumnId: String? = null) {
        viewModelScope.launch {
            withAllTaskLocks {
                val targetCols = _state.value.columns.filter { it.boardId == targetBoardId }.sortedBy { it.position }
                val targetCol = targetColumnId?.let { id -> targetCols.find { it.id == id } } ?: targetCols.firstOrNull() ?: return@withAllTaskLocks
                linkTaskToColumn(task, targetCol.id)
            }
        }
    }

    private suspend fun relocateTaskAppearance(
        task: Task,
        targetColumnId: String,
        newPosition: Int,
        allTasks: List<Task>,
        changeHome: Boolean = false,
        fromBoardId: String? = null,
        targetVisibleOrder: List<Task>? = null
    ) {
        val columns = _state.value.columns
        val boards = _state.value.boards
        val targetCol = columns.find { it.id == targetColumnId } ?: return
        val targetBoard = boards.find { it.id == targetCol.boardId }
        val homeBoardId = columns.find { it.id == task.columnId }?.boardId
        val targetBoardId = targetCol.boardId
        val fromBoard = fromBoardId?.let { id -> boards.find { it.id == id } }
        val homeBoard = boards.find { it.id == homeBoardId }

        if (isEisenhowerBoardName(targetBoard?.name)) {
            val boardCols = columns.filter { it.boardId == targetBoardId }.sortedBy { it.position }
            val index = boardCols.indexOfFirst { it.id == targetColumnId }.coerceAtLeast(0)
            val quadrant = quadrantForHub(targetCol, index)
            if (changeHome || homeBoardId == targetBoardId) {
                val cleanedLinks = if (changeHome && homeBoardId != targetBoardId) {
                    task.linkedColumnIds.filter { id ->
                        val boardId = columns.find { it.id == id }?.boardId
                        boardId != homeBoardId && boardId != targetBoardId && id != targetColumnId
                    }
                } else {
                    task.linkedColumnIds.filter { it != targetColumnId }
                }
                moveTaskUseCase(
                    task.copy(
                        eisenhowerQuadrant = quadrant,
                        showEisenhowerButtons = true,
                        linkedColumnIds = cleanedLinks
                    ),
                    targetColumnId,
                    newPosition,
                    allTasks,
                    targetVisibleOrder
                )
            } else {
                updateTaskUseCase(
                    task.copy(
                        eisenhowerQuadrant = quadrant,
                        showEisenhowerButtons = true
                    )
                )
            }
            return
        }

        if (changeHome) {
            val leaveMatrix = isEisenhowerBoardName(fromBoard?.name) || isEisenhowerBoardName(homeBoard?.name)
            val cleanedLinks = task.linkedColumnIds.filter { id ->
                val linkBoardId = columns.find { it.id == id }?.boardId
                when {
                    id == targetColumnId -> false
                    linkBoardId == null -> false
                    linkBoardId == targetBoardId -> false
                    fromBoardId != null && fromBoardId != targetBoardId && linkBoardId == fromBoardId -> false
                    leaveMatrix && isEisenhowerBoardName(boards.find { it.id == linkBoardId }?.name) -> false
                    else -> true
                }
            }
            moveTaskUseCase(
                task.copy(
                    linkedColumnIds = cleanedLinks,
                    eisenhowerQuadrant = if (leaveMatrix) null else task.eisenhowerQuadrant,
                    showEisenhowerButtons = if (leaveMatrix) false else task.showEisenhowerButtons
                ),
                targetColumnId,
                newPosition,
                allTasks,
                targetVisibleOrder
            )
            return
        }

        if (task.columnId == targetColumnId) {
            moveTaskUseCase(task, targetColumnId, newPosition, allTasks, targetVisibleOrder)
            return
        }

        val linkedOnTargetBoard = task.linkedColumnIds.filter { id ->
            columns.find { it.id == id }?.boardId == targetBoardId
        }
        if (homeBoardId != targetBoardId && (linkedOnTargetBoard.isNotEmpty() || task.linkedColumnIds.contains(targetColumnId))) {
            val keptLinks = task.linkedColumnIds.filter { id ->
                columns.find { it.id == id }?.boardId != targetBoardId
            }
            // Link-only move: never rewrite home-column `position` from a foreign hub index.
            updateTaskUseCase(
                task.copy(
                    linkedColumnIds = (keptLinks + targetColumnId).distinct()
                )
            )
            return
        }

        moveTaskUseCase(task, targetColumnId, newPosition, allTasks, targetVisibleOrder)
    }

    private suspend fun linkTaskToColumn(task: Task, targetColumnId: String) {
        val latest = _state.value.tasks.find { it.id == task.id } ?: task
        val columns = _state.value.columns
        val targetCol = columns.find { it.id == targetColumnId } ?: return
        val targetBoard = _state.value.boards.find { it.id == targetCol.boardId }
        val homeBoardId = columns.find { it.id == latest.columnId }?.boardId

        if (isEisenhowerBoardName(targetBoard?.name)) {
            val index = columns
                .filter { it.boardId == targetCol.boardId }
                .sortedBy { it.position }
                .indexOfFirst { it.id == targetColumnId }
                .coerceAtLeast(0)
            val quadrant = quadrantForHub(targetCol, index)
            if (homeBoardId == targetCol.boardId) {
                if (latest.columnId == targetColumnId && latest.eisenhowerQuadrant == quadrant) return
                val position = _state.value.tasks.count { it.columnId == targetColumnId && it.id != latest.id }
                moveTaskUseCase(
                    latest.copy(
                        eisenhowerQuadrant = quadrant,
                        showEisenhowerButtons = true
                    ),
                    targetColumnId,
                    position,
                    _state.value.tasks
                )
            } else {
                if (latest.eisenhowerQuadrant == quadrant) return
                updateTaskUseCase(
                    latest.copy(
                        eisenhowerQuadrant = quadrant,
                        showEisenhowerButtons = true
                    )
                )
            }
            return
        }

        if (latest.columnId == targetColumnId || latest.linkedColumnIds.contains(targetColumnId)) return
        updateTaskUseCase(latest.copy(linkedColumnIds = latest.linkedColumnIds + targetColumnId))
    }

    fun removeTaskFromBoard(task: Task, boardId: String) {
        viewModelScope.launch {
            val columns = _state.value.columns
            val boards = _state.value.boards
            val board = boards.find { it.id == boardId } ?: return@launch
            val boardCols = columns.filter { it.boardId == boardId }.sortedBy { it.position }
            if (boardCols.isEmpty()) return@launch

            val homeBoardId = columns.find { it.id == task.columnId }?.boardId
            val linksOnBoard = task.linkedColumnIds.filter { id -> columns.find { it.id == id }?.boardId == boardId }
            val isEisenhower = isEisenhowerBoardName(board.name)
            val appearsViaQuadrant = isEisenhower && task.eisenhowerQuadrant != null

            if (homeBoardId == boardId) {
                val otherLink = task.linkedColumnIds.firstOrNull { id ->
                    columns.find { it.id == id }?.boardId != boardId
                }
                val otherMatrixBoard = if (task.eisenhowerQuadrant != null) {
                    boards.firstOrNull {
                        it.id != boardId &&
                            isEisenhowerBoardName(it.name) &&
                            it.projectId == board.projectId
                    }
                } else null
                val newHomeCol = when {
                    otherLink != null -> columns.find { it.id == otherLink }
                    otherMatrixBoard != null -> {
                        val cols = columns.filter { it.boardId == otherMatrixBoard.id }.sortedBy { it.position }
                        val idx = when (task.eisenhowerQuadrant) {
                            Eisenhower.Q1 -> 0
                            Eisenhower.Q2 -> 1
                            Eisenhower.Q3 -> 2
                            else -> 3
                        }
                        cols.getOrNull(idx) ?: cols.firstOrNull()
                    }
                    else -> null
                }
                if (newHomeCol == null) {
                    // Only exists on this board — full delete would be wrong for "remove"; keep task here
                    return@launch
                }
                val cleanedLinks = task.linkedColumnIds.filter { id ->
                    id != newHomeCol.id && columns.find { it.id == id }?.boardId != boardId
                }
                val leaveMatrix = isEisenhower
                moveTaskUseCase(
                    task.copy(
                        linkedColumnIds = cleanedLinks,
                        eisenhowerQuadrant = if (leaveMatrix) {
                            if (isEisenhowerBoardName(boards.find { it.id == newHomeCol.boardId }?.name)) {
                                task.eisenhowerQuadrant
                            } else null
                        } else task.eisenhowerQuadrant,
                        showEisenhowerButtons = if (leaveMatrix && !isEisenhowerBoardName(boards.find { it.id == newHomeCol.boardId }?.name)) {
                            false
                        } else task.showEisenhowerButtons
                    ),
                    newHomeCol.id,
                    _state.value.tasks.count { it.columnId == newHomeCol.id && it.id != task.id },
                    _state.value.tasks
                )
            } else {
                var updated = task
                if (linksOnBoard.isNotEmpty()) {
                    updated = updated.copy(linkedColumnIds = updated.linkedColumnIds.filterNot { it in linksOnBoard })
                }
                if (appearsViaQuadrant) {
                    updated = updated.copy(eisenhowerQuadrant = null)
                }
                if (updated != task) {
                    updateTaskUseCase(updated)
                }
            }
        }
    }

    private fun isEisenhowerBoardName(name: String?): Boolean =
        KanbanNames.isEisenhowerBoard(name)

    private fun quadrantForHub(col: Column, colIndex: Int): String = when {
        KanbanNames.isQ1Hub(col.title) || colIndex == 0 -> Eisenhower.Q1
        KanbanNames.isQ2Hub(col.title) || colIndex == 1 -> Eisenhower.Q2
        KanbanNames.isQ3Hub(col.title) || colIndex == 2 -> Eisenhower.Q3
        else -> Eisenhower.Q4
    }

    // Export & Import
    suspend fun exportData(): KairosTransferData = exportDataUseCase()

    suspend fun exportProjectData(projectId: String): KairosTransferData {
        val full = exportDataUseCase()
        val boards = full.boards.filter { it.projectId == projectId }
        val boardIds = boards.map { it.id }.toSet()
        val columns = full.columns.filter { it.boardId in boardIds }
        val columnIds = columns.map { it.id }.toSet()
        // Only tasks whose home column belongs to this project (avoid orphan columnIds on import).
        val tasks = full.tasks.filter { it.columnId in columnIds }
        val taskIds = tasks.map { it.id }.toSet()
        val categoryIds = tasks.mapNotNull { it.categoryId }.toSet()
        return full.copy(
            projects = full.projects.filter { it.id == projectId },
            boards = boards,
            columns = columns,
            tasks = tasks.map { task ->
                task.copy(linkedColumnIds = task.linkedColumnIds.filter { it in columnIds })
            },
            comments = full.comments.filter { it.taskId in taskIds },
            columnComments = full.columnComments.filter { it.columnId in columnIds },
            contacts = full.contacts.filter { it.projectId == projectId },
            categories = full.categories.filter { it.id in categoryIds },
            taskLinks = full.taskLinks.filter { it.parentId in taskIds && it.childId in taskIds }
        )
    }

    suspend fun importData(data: KairosTransferData): Int = importDataUseCase(data)

    suspend fun exportToJson(): String {
        val data = exportDataUseCase()
        return KairosTransferHelper.toJson(data)
    }

    suspend fun exportProjectToJson(projectId: String): String {
        return KairosTransferHelper.toJson(exportProjectData(projectId))
    }

    suspend fun importFromJson(jsonString: String): Int {
        val data = KairosTransferHelper.fromJson(jsonString)
        return importDataUseCase(data)
    }

    suspend fun importFromDeepLink(uri: Uri): Int {
        val data = KairosTransferHelper.parseFromDeepLink(uri)
            ?: throw IllegalArgumentException("Invalid import link")
        return importDataUseCase(data)
    }
}

class SharedViewModelFactory(
    private val repository: KanbanRepository,
    private val initializeDatabaseUseCase: InitializeDatabaseUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val moveTaskUseCase: MoveTaskUseCase,
    private val addColumnUseCase: AddColumnUseCase,
    private val deleteColumnUseCase: DeleteColumnUseCase,
    private val renameColumnUseCase: RenameColumnUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val setDefaultBoardUseCase: SetDefaultBoardUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val addColumnCommentUseCase: AddColumnCommentUseCase,
    private val deleteColumnCommentUseCase: DeleteColumnCommentUseCase,
    private val moveTaskToBoardUseCase: MoveTaskToBoardUseCase,
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val ensureRecurringInstancesUseCase: EnsureRecurringInstancesUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SharedViewModel(
                repository, initializeDatabaseUseCase, addTaskUseCase, updateTaskUseCase, moveTaskUseCase,
                addColumnUseCase, deleteColumnUseCase, renameColumnUseCase, deleteTaskUseCase, setDefaultBoardUseCase,
                addCommentUseCase, deleteCommentUseCase, addColumnCommentUseCase, deleteColumnCommentUseCase,
                moveTaskToBoardUseCase, exportDataUseCase, importDataUseCase, ensureRecurringInstancesUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}