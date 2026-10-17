package com.example.kaizenkanban.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.data.local.GalleryNotePhotoStore
import com.example.kaizenkanban.data.local.TaskAttachmentStore
import com.example.kaizenkanban.data.transfer.KairosTransferHelper
import com.example.kaizenkanban.domain.TaskCompletionGate
import com.example.kaizenkanban.domain.commands.KairosApplyResult
import com.example.kaizenkanban.domain.commands.KairosApplyUndo
import com.example.kaizenkanban.domain.commands.KairosCommandApplier
import com.example.kaizenkanban.domain.commands.KairosCommandPlanner
import com.example.kaizenkanban.domain.commands.KairosCommandParser
import com.example.kaizenkanban.domain.commands.KairosPlan
import org.json.JSONArray
import org.json.JSONObject
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
import kotlinx.coroutines.withContext
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
    val taskAttachments: List<TaskAttachment> = emptyList(),
    val statsJournal: List<StatsJournalEntry> = emptyList(),
    val reminders: List<KairosReminder> = emptyList(),
    val projectNotes: List<ProjectNote> = emptyList(),
    val noteLines: List<NoteLine> = emptyList(),
    val notePhotos: List<NotePhoto> = emptyList(),
    val isInitialized: Boolean = false
)

/** Progress of [SharedViewModel.clearArchiveToJournal]; null when idle. */
data class ClearArchiveProgress(val processed: Int, val total: Int)

private data class DeletedTaskSnapshot(
    val task: Task,
    val comments: List<Comment>,
    val links: List<TaskLink> = emptyList(),
    val recurringSkipKey: String? = null
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
    private val ensureRecurringInstancesUseCase: EnsureRecurringInstancesUseCase,
    private val attachmentStore: TaskAttachmentStore? = null,
    private val notePhotoStore: GalleryNotePhotoStore? = null,
    private val preferences: KairosPreferences? = null
) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    
    var hasAutoNavigated = false
    private val _pendingOpenTaskId = MutableStateFlow<String?>(null)
    val pendingOpenTaskId: StateFlow<String?> = _pendingOpenTaskId.asStateFlow()
    private val deletedSnapshots = mutableMapOf<String, DeletedTaskSnapshot>()
    private val kairosUndoMemory = mutableMapOf<String, ArrayDeque<KairosApplyUndo>>()
    private val _kairosUndoRevision = MutableStateFlow(0)
    val kairosUndoRevision: StateFlow<Int> = _kairosUndoRevision.asStateFlow()
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

    private val _pendingAddNote = MutableStateFlow(false)
    val pendingAddNote: StateFlow<Boolean> = _pendingAddNote.asStateFlow()

    private val _pendingVoiceNote = MutableStateFlow(false)
    val pendingVoiceNote: StateFlow<Boolean> = _pendingVoiceNote.asStateFlow()

    private val _clearArchiveProgress = MutableStateFlow<ClearArchiveProgress?>(null)
    val clearArchiveProgress: StateFlow<ClearArchiveProgress?> = _clearArchiveProgress.asStateFlow()

    fun requestQuickAdd() {
        _pendingQuickAdd.value = true
    }

    fun requestVoiceAssistant() {
        _pendingVoiceAssistant.value = true
    }

    fun requestAddTask() {
        _pendingAddTask.value = true
    }

    fun requestAddNote() {
        _pendingAddNote.value = true
    }

    fun requestVoiceNote() {
        _pendingVoiceNote.value = true
        _pendingAddNote.value = true
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

    fun consumePendingAddNote(): Boolean {
        if (!_pendingAddNote.value) return false
        _pendingAddNote.value = false
        return true
    }

    fun consumePendingVoiceNote(): Boolean {
        if (!_pendingVoiceNote.value) return false
        _pendingVoiceNote.value = false
        return true
    }

    fun clearPendingQuickAdd() {
        _pendingQuickAdd.value = false
    }

    fun clearPendingAddNote() {
        _pendingAddNote.value = false
        _pendingVoiceNote.value = false
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
            }.combine(repository.getAllTaskAttachments()) { pair, attachments ->
                Pair(pair, attachments)
            }.combine(repository.getAllStatsJournal()) { pair, journal ->
                Pair(pair, journal)
            }.combine(repository.getAllReminders()) { pair, reminders ->
                Pair(pair, reminders)
            }.combine(repository.getAllProjectNotes()) { pair, notes ->
                Pair(pair, notes)
            }.combine(repository.getAllNoteLines()) { pair, noteLines ->
                Pair(pair, noteLines)
            }.combine(repository.getAllNotePhotos()) { pair, notePhotos ->
                val (withLines, noteLines) = pair
                val (withNotes, notes) = withLines
                val (withReminders, reminders) = withNotes
                val (withJournal, journal) = withReminders
                val (withAttachments, attachments) = withJournal
                val (withLinks, links) = withAttachments
                val (withTemplates, templates) = withLinks
                val (withContacts, contacts) = withTemplates
                val (core, comments, colComments) = withContacts
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
                    taskAttachments = attachments,
                    statsJournal = journal,
                    reminders = reminders,
                    projectNotes = notes,
                    noteLines = noteLines,
                    notePhotos = notePhotos,
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

        viewModelScope.launch {
            state.collect { snapshot ->
                if (snapshot.isInitialized) markGoalsHubTasks(snapshot)
            }
        }
    }

    fun ensureTodayRecurringInstances() {
        viewModelScope.launch {
            val s = _state.value
            val prefs = preferences
            val now = System.currentTimeMillis()
            val templates = repository.getAllRecurringTemplates().first()
            ensureRecurringInstancesUseCase(
                templates = templates,
                columns = s.columns,
                now = now,
                defaultReminderMinutes = prefs?.reminderMinutesOfDay
                    ?: TaskReminderDefaults.DEFAULT_MINUTES,
                defaultSpawnMinutes = prefs?.spawnMinutesOfDay
                    ?: RecurringDefaults.DEFAULT_SPAWN_MINUTES,
                onlySlotsReadyByNow = true,
                primaryHubByBoardId = EnsureRecurringInstancesUseCase.resolvePrimaryHubs(
                    boards = s.boards,
                    columns = s.columns,
                    storedHubId = { boardId -> prefs?.getPrimaryHubId(boardId) }
                ),
                dueTodaySweptDay = prefs?.dueTodayPrimarySweepDay ?: 0L,
                onDueTodaySwept = { day -> prefs?.dueTodayPrimarySweepDay = day }
            )
            repository.getAllRecurringTemplates().first().forEach { refreshRecurringLeadReminders(it) }
        }
    }

    fun upsertRecurringTemplate(template: RecurringTemplate) {
        viewModelScope.launch {
            val existing = _state.value.recurringTemplates.any { it.id == template.id }
            if (existing) repository.updateRecurringTemplate(template)
            else repository.insertRecurringTemplate(template)
            _state.value.tasks
                .filter { it.recurringTemplateId == template.id && !it.isCompleted && !it.isBoardArchived }
                .forEach { task ->
                    val nextDeadline = task.dueDate?.let { start ->
                        EnsureRecurringInstancesUseCase.instanceDeadlineAt(
                            start,
                            template.deadlineOffsetDays,
                            template.deadlineOffsetHours
                        )
                    }
                    val nextTimer = template.timerMinutes?.takeIf { it > 0 }
                    if (task.deadlineAt != nextDeadline || task.timerMinutes != nextTimer) {
                        updateTaskUseCase(
                            task.copy(
                                deadlineAt = nextDeadline,
                                timerMinutes = nextTimer
                            )
                        )
                    }
                }
            refreshRecurringLeadReminders(template)
            ensureTodayRecurringInstances()
        }
    }

    /** Parse + plan a pasted KAIROS v1 command block (no DB writes). */
    fun planKairosCommands(
        raw: String,
        projectId: String,
        selectedGoalId: String? = null
    ): KairosPlan {
        val s = _state.value
        val parse = KairosCommandParser.parse(raw)
        val projectBoardIds = s.boards.filter { it.projectId == projectId }.map { it.id }.toSet()
        val projectColumns = s.columns.filter { it.boardId in projectBoardIds }
        val projectColumnIds = projectColumns.map { it.id }.toSet()
        val projectTasks = s.tasks.filter {
            !it.isHidden &&
                (it.columnId in projectColumnIds || it.linkedColumnIds.any { id -> id in projectColumnIds })
        }
        return KairosCommandPlanner.plan(
            parse = parse,
            ctx = KairosCommandPlanner.Context(
                projectId = projectId,
                boards = s.boards.filter { it.projectId == projectId },
                columns = projectColumns,
                tasks = projectTasks,
                links = s.taskLinks,
                selectedGoalId = selectedGoalId,
                defaultReminderMinutes = preferences?.reminderMinutesOfDay
                    ?: TaskReminderDefaults.DEFAULT_MINUTES
            )
        )
    }

    /** Apply a previously planned command block. Triggers recurring ensure after habits. */
    fun applyKairosPlan(
        plan: KairosPlan,
        projectId: String,
        onDone: (KairosApplyResult) -> Unit
    ) {
        viewModelScope.launch {
            val s = _state.value
            val projectBoardIds = s.boards.filter { it.projectId == projectId }.map { it.id }.toSet()
            val projectColumnIds = s.columns.filter { it.boardId in projectBoardIds }.map { it.id }.toSet()
            val projectTasks = s.tasks.filter {
                it.columnId in projectColumnIds || it.linkedColumnIds.any { id -> id in projectColumnIds }
            }
            val result = withContext(Dispatchers.IO) {
                KairosCommandApplier(repository).apply(
                    plan = plan,
                    projectId = projectId,
                    existingTasks = projectTasks,
                    existingLinks = s.taskLinks
                )
            }
            if (result.createdHabits > 0) ensureTodayRecurringInstances()
            if (result.createdTaskIds.isNotEmpty() ||
                result.createdHabitIds.isNotEmpty() ||
                result.createdLinksPairs.isNotEmpty()
            ) {
                pushKairosUndo(
                    projectId,
                    KairosApplyUndo(
                        taskIds = result.createdTaskIds,
                        habitIds = result.createdHabitIds,
                        links = result.createdLinksPairs
                    )
                )
            }
            onDone(result)
        }
    }

    fun kairosUndoCount(projectId: String): Int = readKairosUndos(projectId).size

    /** Removes the newest applied command block: created tasks, habits, and links. */
    fun undoLastKairosApply(projectId: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val batch = popKairosUndo(projectId)
            if (batch == null) {
                onDone(false)
                return@launch
            }
            val habitIds = batch.habitIds.toSet()
            val spawnedIds = _state.value.tasks
                .filter { task ->
                    val templateId = task.recurringTemplateId
                    !templateId.isNullOrBlank() && templateId in habitIds
                }
                .map { it.id }
            val taskIds = (batch.taskIds + spawnedIds).distinct()
            withContext(Dispatchers.IO) {
                batch.links.forEach { (parentId, childId) ->
                    repository.deleteTaskLink(parentId, childId)
                }
                taskIds.forEach { deleteTaskUseCase(it) }
                batch.habitIds.forEach { repository.deleteRecurringTemplate(it) }
            }
            val focusId = preferences?.inProgressTaskId
            if (focusId != null && focusId in taskIds) {
                preferences?.inProgressTaskId = null
            }
            onDone(true)
        }
    }

    private fun pushKairosUndo(projectId: String, batch: KairosApplyUndo) {
        val prefs = preferences
        if (prefs == null) {
            val stack = kairosUndoMemory.getOrPut(projectId) { ArrayDeque() }
            stack.addLast(batch)
            while (stack.size > 20) stack.removeFirst()
        } else {
            val arr = decodeKairosUndos(prefs.kairosUndoLog(projectId))
            arr.put(encodeKairosUndo(batch))
            while (arr.length() > 20) {
                val trimmed = JSONArray()
                for (i in 1 until arr.length()) trimmed.put(arr.getJSONObject(i))
                prefs.setKairosUndoLog(projectId, trimmed.toString())
                _kairosUndoRevision.value = _kairosUndoRevision.value + 1
                return
            }
            prefs.setKairosUndoLog(projectId, arr.toString())
        }
        _kairosUndoRevision.value = _kairosUndoRevision.value + 1
    }

    private fun popKairosUndo(projectId: String): KairosApplyUndo? {
        val prefs = preferences
        if (prefs == null) {
            val batch = kairosUndoMemory[projectId]?.removeLastOrNull()
            if (batch != null) _kairosUndoRevision.value = _kairosUndoRevision.value + 1
            return batch
        }
        val arr = decodeKairosUndos(prefs.kairosUndoLog(projectId))
        if (arr.length() == 0) return null
        val last = arr.getJSONObject(arr.length() - 1)
        val trimmed = JSONArray()
        for (i in 0 until arr.length() - 1) trimmed.put(arr.getJSONObject(i))
        prefs.setKairosUndoLog(projectId, trimmed.toString())
        _kairosUndoRevision.value = _kairosUndoRevision.value + 1
        return decodeKairosUndo(last)
    }

    private fun readKairosUndos(projectId: String): List<KairosApplyUndo> {
        val prefs = preferences ?: return kairosUndoMemory[projectId].orEmpty().toList()
        val arr = decodeKairosUndos(prefs.kairosUndoLog(projectId))
        return buildList {
            for (i in 0 until arr.length()) add(decodeKairosUndo(arr.getJSONObject(i)))
        }
    }

    private fun encodeKairosUndo(batch: KairosApplyUndo): JSONObject = JSONObject().apply {
        put("tasks", JSONArray(batch.taskIds))
        put("habits", JSONArray(batch.habitIds))
        put("links", JSONArray().apply {
            batch.links.forEach { (parentId, childId) -> put("$parentId|$childId") }
        })
    }

    private fun decodeKairosUndo(obj: JSONObject): KairosApplyUndo {
        val tasks = obj.optJSONArray("tasks")
        val habits = obj.optJSONArray("habits")
        val links = obj.optJSONArray("links")
        return KairosApplyUndo(
            taskIds = jsonStrings(tasks),
            habitIds = jsonStrings(habits),
            links = buildList {
                if (links == null) return@buildList
                for (i in 0 until links.length()) {
                    val raw = links.optString(i)
                    val split = raw.split('|', limit = 2)
                    if (split.size == 2 && split[0].isNotBlank() && split[1].isNotBlank()) {
                        add(split[0] to split[1])
                    }
                }
            }
        )
    }

    private fun decodeKairosUndos(raw: String): JSONArray {
        if (raw.isBlank()) return JSONArray()
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun jsonStrings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val value = array.optString(i)
                if (value.isNotBlank()) add(value)
            }
        }
    }

    fun setRecurringTemplateEnabled(templateId: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = _state.value.recurringTemplates.find { it.id == templateId } ?: return@launch
            val updated = current.copy(enabled = enabled)
            repository.updateRecurringTemplate(updated)
            refreshRecurringLeadReminders(updated)
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

    fun addContact(
        projectId: String,
        name: String,
        phone: String,
        email: String,
        role: String,
        social: String = ""
    ): String {
        val trimmed = name.trim()
        if (trimmed.isBlank() || projectId.isBlank()) return ""
        val id = UUID.randomUUID().toString()
        val nextPos = (_state.value.contacts
            .filter { it.projectId == projectId }
            .maxOfOrNull { it.position } ?: -1) + 1
        viewModelScope.launch {
            repository.insertContact(
                Contact(
                    id = id,
                    projectId = projectId,
                    name = trimmed,
                    phone = phone.trim(),
                    email = email.trim(),
                    role = role.trim(),
                    social = social.trim(),
                    position = nextPos
                )
            )
        }
        return id
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

    /** Settings DB browser: delete board even if default/protected. */
    fun hardDeleteBoard(id: String) {
        viewModelScope.launch {
            if (_state.value.boards.none { it.id == id }) return@launch
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
        startMinutesOfDay: Int? = null,
        deadlineAt: Long? = null,
        complexity: Int? = null,
        estimatedMinutes: Int? = null,
        parentIds: List<String> = emptyList(),
        childIds: List<String> = emptyList(),
        noteLeadAmount: Int? = null,
        noteLeadUnit: RemindLeadUnit? = null,
        noteText: String? = null,
        isGoal: Boolean = false,
        timerMinutes: Int? = null,
        cardType: String = com.example.kaizenkanban.domain.model.TaskCardKind.PLAIN,
        cardPayload: String = ""
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
                startMinutesOfDay = startMinutesOfDay,
                deadlineAt = deadlineAt,
                complexity = complexity,
                estimatedMinutes = estimatedMinutes,
                isGoal = isGoal,
                timerMinutes = timerMinutes,
                cardType = cardType,
                cardPayload = cardPayload
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
            insertTaskLeadReminder(
                taskId = taskId,
                columnId = columnId,
                title = title,
                dueDate = dueDate,
                reminderMinutesOfDay = startMinutesOfDay ?: reminderMinutesOfDay,
                amount = noteLeadAmount,
                unit = noteLeadUnit,
                text = noteText
            )
        }
    }

    fun setTaskLeadReminder(
        task: Task,
        amount: Int?,
        unit: RemindLeadUnit?,
        text: String
    ) {
        viewModelScope.launch {
            repository.deleteRemindersForTask(task.id)
            insertTaskLeadReminder(
                taskId = task.id,
                columnId = task.columnId,
                title = task.title,
                dueDate = task.dueDate,
                reminderMinutesOfDay = task.startMinutesOfDay ?: task.reminderMinutesOfDay,
                amount = amount,
                unit = unit,
                text = text
            )
        }
    }

    fun addStandaloneReminder(projectId: String, text: String, triggerAtMillis: Long) {
        viewModelScope.launch {
            val body = text.trim()
            if (projectId.isBlank() || body.isBlank()) return@launch
            repository.insertReminder(
                KairosReminder(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    text = body,
                    triggerAtMillis = triggerAtMillis,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch { repository.deleteReminder(id) }
    }

    fun upsertProjectNote(
        projectId: String,
        body: String,
        existingId: String? = null,
        pinned: Boolean = false,
        linkedTaskId: String? = null,
        reminderAtMillis: Long? = null,
        title: String = "",
        lines: List<NoteLine> = emptyList(),
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val text = body.trim()
            val heading = title.trim()
            if (projectId.isBlank() || (text.isBlank() && heading.isBlank() && lines.isEmpty())) return@launch
            val now = System.currentTimeMillis()
            val existing = existingId?.let { id ->
                _state.value.projectNotes.find { it.id == id }
            }
            val note = if (existing != null) {
                existing.copy(
                    body = text,
                    title = heading,
                    updatedAt = now,
                    pinned = pinned,
                    linkedTaskId = linkedTaskId,
                    reminderAtMillis = reminderAtMillis
                )
            } else {
                val nextPos = _state.value.projectNotes
                    .filter { it.projectId == projectId }
                    .maxOfOrNull { it.position }
                    ?.plus(1) ?: 0
                ProjectNote(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    body = text,
                    createdAt = now,
                    updatedAt = now,
                    position = nextPos,
                    pinned = pinned,
                    linkedTaskId = linkedTaskId,
                    reminderAtMillis = reminderAtMillis,
                    title = heading
                )
            }
            repository.insertProjectNote(note)
            repository.replaceNoteLines(note.id, lines.map { it.copy(noteId = note.id) })
            syncNoteReminder(note)
            onSaved(note.id)
        }
    }

    fun setNotePinned(noteId: String, pinned: Boolean) {
        viewModelScope.launch {
            val note = _state.value.projectNotes.find { it.id == noteId } ?: return@launch
            repository.insertProjectNote(note.copy(pinned = pinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun moveProjectNote(noteId: String, direction: Int) {
        viewModelScope.launch {
            val note = _state.value.projectNotes.find { it.id == noteId } ?: return@launch
            val siblings = _state.value.projectNotes
                .filter { it.projectId == note.projectId }
                .sortedWith(compareByDescending<ProjectNote> { it.pinned }.thenBy { it.position }.thenByDescending { it.createdAt })
            val idx = siblings.indexOfFirst { it.id == noteId }
            val target = idx + direction
            if (idx < 0 || target !in siblings.indices) return@launch
            val a = siblings[idx]
            val b = siblings[target]
            val now = System.currentTimeMillis()
            repository.insertProjectNote(a.copy(position = b.position, updatedAt = now))
            repository.insertProjectNote(b.copy(position = a.position, updatedAt = now))
        }
    }

    fun deleteProjectNote(noteId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteProjectNote(noteId)
            }
        }
    }

    fun addNotePhoto(noteId: String, lineId: String, uri: Uri, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val store = notePhotoStore ?: return@withContext "error"
                    val current = _state.value.notePhotos.count { it.noteId == noteId }
                    if (current >= NotePhoto.MAX_PER_NOTE) return@withContext "limit"
                    val id = UUID.randomUUID().toString()
                    val mediaUri = store.saveImageFromUri(uri, id)
                    repository.insertNotePhoto(
                        NotePhoto(
                            id = id,
                            noteId = noteId,
                            mediaUri = mediaUri,
                            createdAt = System.currentTimeMillis(),
                            sortOrder = current,
                            lineId = lineId
                        )
                    )
                    null
                } catch (_: Exception) {
                    "error"
                }
            }
            onResult(result)
        }
    }

    fun deleteNotePhoto(photoId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteNotePhoto(photoId)
            }
        }
    }

    /**
     * Creates a task from the note (body → title/description), copies gallery photos into
     * task storage, optionally links to [ProjectNote.linkedTaskId] if it is a goal, then deletes the note.
     * @return new task id or null
     */
    suspend fun convertNoteToTask(noteId: String): String? = withContext(Dispatchers.IO) {
        val note = _state.value.projectNotes.find { it.id == noteId } ?: return@withContext null
        val boards = _state.value.boards.filter { it.projectId == note.projectId && !it.isArchived }
        val board = boards.find { it.isDefault } ?: boards.firstOrNull() ?: return@withContext null
        val columns = _state.value.columns.filter { it.boardId == board.id }.sortedBy { it.position }
        if (columns.isEmpty()) return@withContext null
        val prefs = preferences
        val markedId = prefs?.getPrimaryHubId(board.id)
        val hub = columns.find { it.id == markedId } ?: columns.first()
        val lines = note.body.trim().lines().filter { it.isNotBlank() }
        val explicitTitle = note.title.trim()
        val title = if (explicitTitle.isNotEmpty()) {
            explicitTitle.take(120)
        } else {
            (lines.firstOrNull() ?: note.body).take(120)
        }
        val description = if (explicitTitle.isNotEmpty()) {
            note.body.trim()
        } else if (lines.size > 1) {
            lines.drop(1).joinToString("\n")
        } else {
            ""
        }
        val tasksInColumn = _state.value.tasks.filter { it.columnId == hub.id }
        val taskId = addTaskUseCase(
            title = title,
            columnId = hub.id,
            categoryId = null,
            dueDate = null,
            currentTasksInColumn = tasksInColumn
        ) ?: return@withContext null
        if (description.isNotBlank()) {
            val created = repository.getAllTasks().first().find { it.id == taskId }
            if (created != null) {
                updateTaskUseCase(created.copy(description = description))
            }
        }
        val linked = note.linkedTaskId?.let { id -> _state.value.tasks.find { it.id == id } }
        if (linked != null && linked.isGoal) {
            repository.insertTaskLink(
                TaskLink(parentId = linked.id, childId = taskId, createdAt = System.currentTimeMillis())
            )
        }
        val photos = repository.getPhotosForNote(noteId)
        val store = attachmentStore
        photos.forEachIndexed { index, photo ->
            if (store == null) return@forEachIndexed
            try {
                val uri = Uri.parse(photo.mediaUri)
                val attachmentId = UUID.randomUUID().toString()
                val relative = store.saveImageFromUri(uri, note.projectId, taskId, attachmentId)
                repository.insertTaskAttachment(
                    TaskAttachment(
                        id = attachmentId,
                        taskId = taskId,
                        relativePath = relative,
                        createdAt = System.currentTimeMillis(),
                        sortOrder = index
                    )
                )
            } catch (_: Exception) {
                // skip broken gallery refs
            }
        }
        repository.deleteProjectNote(noteId)
        taskId
    }

    private suspend fun syncNoteReminder(note: ProjectNote) {
        val reminderId = NoteReminderIds.forNote(note.id)
        repository.deleteReminder(reminderId)
        val at = note.reminderAtMillis ?: return
        val text = note.displayTitle().take(80)
        if (text.isBlank()) return
        repository.insertReminder(
            KairosReminder(
                id = reminderId,
                projectId = note.projectId,
                text = text,
                triggerAtMillis = at,
                createdAt = note.createdAt
            )
        )
    }

    private suspend fun insertTaskLeadReminder(
        taskId: String,
        columnId: String,
        title: String,
        dueDate: Long?,
        reminderMinutesOfDay: Int?,
        amount: Int?,
        unit: RemindLeadUnit?,
        text: String?
    ) {
        if (amount == null || unit == null || amount <= 0 || dueDate == null) return
        val projectId = projectIdForColumn(columnId) ?: return
        val body = text?.trim().orEmpty().ifBlank { title.trim() }
        if (body.isBlank()) return
        val anchor = RemindLeadCalculator.taskAnchor(
            dueDate,
            reminderMinutesOfDay,
            preferences?.reminderMinutesOfDay ?: TaskReminderDefaults.DEFAULT_MINUTES
        )
        repository.insertReminder(
            KairosReminder(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                text = body,
                triggerAtMillis = RemindLeadCalculator.subtract(anchor, amount, unit),
                taskId = taskId,
                leadAmount = amount,
                leadUnit = unit.id,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private fun projectIdForColumn(columnId: String): String? {
        val boardId = _state.value.columns.find { it.id == columnId }?.boardId ?: return null
        return _state.value.boards.find { it.id == boardId }?.projectId
    }

    private suspend fun refreshRecurringLeadReminders(template: RecurringTemplate) {
        repository.deleteRemindersForTemplate(template.id)
        val planned = com.example.kaizenkanban.domain.reminders.RecurringReminderPlanner.plan(template)
        if (planned.isNotEmpty()) repository.insertReminders(planned)
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
                        showEisenhowerButtons = task.showEisenhowerButtons,
                        hubGroupId = task.hubGroupId
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
                val next = if (!task.isGoal && isGoalByHub(task.columnId)) {
                    task.copy(isGoal = true)
                } else {
                    task
                }
                updateTaskUseCase(next)
            }
        }
    }

    /** True when [columnId] is the board's goals list, so its cards stay goals. */
    fun isGoalByHub(columnId: String): Boolean {
        val column = _state.value.columns.find { it.id == columnId } ?: return false
        val prefs = preferences
        val stored = prefs?.getGoalsHubId(column.boardId)
        return when {
            stored != null && column.id == stored -> true
            stored == null && KanbanNames.isMainGoalHub(column.title) -> true
            else -> false
        }
    }

    private suspend fun markGoalsHubTasks(snapshot: AppState) {
        val prefs = preferences ?: return
        val toMark = mutableListOf<Task>()
        snapshot.boards.forEach { board ->
            val columns = snapshot.columns
                .filter { it.boardId == board.id }
                .sortedBy { it.position }
            val stored = prefs.getGoalsHubId(board.id)
            val hub = if (stored != null && columns.any { it.id == stored }) {
                columns.first { it.id == stored }
            } else {
                if (stored != null) prefs.setGoalsHubId(board.id, null)
                columns.firstOrNull { KanbanNames.isMainGoalHub(it.title) }?.also { found ->
                    prefs.setGoalsHubId(board.id, found.id)
                }
            } ?: return@forEach
            snapshot.tasks.forEach { task ->
                if (task.columnId == hub.id && !task.isGoal && !task.isBoardArchived) {
                    toMark += task
                }
            }
        }
        if (toMark.isEmpty()) return
        toMark.forEach { updateTaskUseCase(it.copy(isGoal = true)) }
    }

    /**
     * Group recurring templates that share a hub. Spawned instances copy [RecurringTemplate.hubGroupId],
     * and open instances already on the hub join the same block.
     * Returns null when fewer than two templates are selected or they target different hubs.
     */
    fun groupRecurringTemplates(templateIds: Collection<String>): String? {
        val selected = templateIds.distinct().mapNotNull { id ->
            _state.value.recurringTemplates.find { it.id == id }
        }
        if (selected.isEmpty()) return null
        val columnId = selected.first().targetColumnId
        if (selected.any { it.targetColumnId != columnId }) return null
        val existingGroupId = selected.mapNotNull { it.hubGroupId?.takeIf { g -> g.isNotBlank() } }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
        if (existingGroupId == null && selected.size < 2) return null
        val groupId = existingGroupId ?: UUID.randomUUID().toString()
        val selectedIds = selected.map { it.id }.toSet()
        viewModelScope.launch {
            withAllTaskLocks {
                selected.forEach { template ->
                    repository.updateRecurringTemplate(template.copy(hubGroupId = groupId))
                }
                val tasks = _state.value.tasks
                val memberIds = tasks.filter {
                    it.columnId == columnId && !it.isCompleted && !it.isHidden &&
                        (it.recurringTemplateId in selectedIds || it.hubGroupId == groupId)
                }.map { it.id }.toSet()
                if (memberIds.isEmpty()) return@withAllTaskLocks
                val allActive = tasks
                    .filter { it.columnId == columnId && !it.isCompleted && !it.isHidden }
                    .sortedBy { it.position }
                val packed = HubGroupOrdering.packGroupAtTop(allActive, memberIds)
                val withFlag = tasks.map { task ->
                    if (task.id in memberIds) task.copy(hubGroupId = groupId) else task
                }
                repository.updateTasks(withFlag.filter { it.id in memberIds })
                moveTaskUseCase.reorderHomeTasks(columnId, packed.map { it.id }, withFlag)
            }
        }
        return groupId
    }

    /** Append recurring templates to an existing group. They must target the group's hub. */
    fun addRecurringTemplatesToGroup(groupId: String, templateIds: Collection<String>): Boolean {
        val gid = groupId.takeIf { it.isNotBlank() } ?: return false
        val seed = _state.value.recurringTemplates.firstOrNull { it.hubGroupId == gid } ?: return false
        val columnId = seed.targetColumnId
        val toAdd = templateIds.distinct().mapNotNull { id ->
            _state.value.recurringTemplates.find {
                it.id == id && it.targetColumnId == columnId && it.hubGroupId != gid
            }
        }
        if (toAdd.isEmpty()) return false
        val selectedIds = toAdd.map { it.id }.toSet()
        viewModelScope.launch {
            withAllTaskLocks {
                toAdd.forEach { template ->
                    repository.updateRecurringTemplate(template.copy(hubGroupId = gid))
                }
                val tasks = _state.value.tasks
                val memberIds = tasks.filter {
                    it.columnId == columnId && !it.isCompleted && !it.isHidden &&
                        (it.recurringTemplateId in selectedIds || it.hubGroupId == gid)
                }.map { it.id }.toSet()
                if (memberIds.isEmpty()) return@withAllTaskLocks
                val allActive = tasks
                    .filter { it.columnId == columnId && !it.isCompleted && !it.isHidden }
                    .sortedBy { it.position }
                val packed = HubGroupOrdering.packGroupAtTop(allActive, memberIds)
                val withFlag = tasks.map { task ->
                    if (task.id in memberIds) task.copy(hubGroupId = gid) else task
                }
                repository.updateTasks(withFlag.filter { it.id in memberIds })
                moveTaskUseCase.reorderHomeTasks(columnId, packed.map { it.id }, withFlag)
            }
        }
        return true
    }

    fun ungroupRecurringTemplates(templateIds: Collection<String>) {
        val ids = templateIds.toSet()
        val selected = _state.value.recurringTemplates.filter {
            it.id in ids && !it.hubGroupId.isNullOrBlank()
        }
        if (selected.isEmpty()) return
        val selectedIds = selected.map { it.id }.toSet()
        val groupIds = selected.mapNotNull { it.hubGroupId }.toSet()
        viewModelScope.launch {
            withAllTaskLocks {
                selected.forEach { template ->
                    repository.updateRecurringTemplate(template.copy(hubGroupId = null))
                }
                val cleared = _state.value.tasks.filter {
                    it.recurringTemplateId in selectedIds &&
                        it.hubGroupId in groupIds &&
                        !it.isCompleted
                }.map { it.copy(hubGroupId = null) }
                if (cleared.isNotEmpty()) repository.updateTasks(cleared)
            }
        }
    }

    /** Put selected hub tasks into one visual group at the top; merges into an existing group when selected. */
    fun groupHubTasks(taskIds: Collection<String>): String? {
        val unique = taskIds.distinct()
        if (unique.isEmpty()) return null
        val selected = unique.mapNotNull { id -> _state.value.tasks.find { it.id == id } }
        if (selected.isEmpty()) return null
        val homeColumn = selected.first().columnId
        if (selected.any { it.columnId != homeColumn }) return null

        val existingGroupId = selected.mapNotNull { it.hubGroupId?.takeIf { g -> g.isNotBlank() } }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        if (existingGroupId == null && unique.size < 2) return null

        val groupId = existingGroupId ?: UUID.randomUUID().toString()
        viewModelScope.launch {
            withAllTaskLocks {
                val allActive = _state.value.tasks
                    .filter { it.columnId == homeColumn && !it.isCompleted && !it.isHidden }
                    .sortedBy { it.position }
                val memberIds = (
                    unique + allActive.filter { it.hubGroupId == groupId }.map { it.id }
                    ).toSet()
                val packed = HubGroupOrdering.packGroupAtTop(allActive, memberIds)
                val taggedIds = packed.map { it.id }
                val withGroupFlag = _state.value.tasks.map { t ->
                    if (t.id in memberIds) t.copy(hubGroupId = groupId) else t
                }
                repository.updateTasks(
                    withGroupFlag.filter { it.id in memberIds }
                )
                moveTaskUseCase.reorderHomeTasks(homeColumn, taggedIds, withGroupFlag)
            }
        }
        return groupId
    }

    /** Append [taskIds] to an existing hub group and keep the block at the top. */
    fun addTasksToHubGroup(groupId: String, taskIds: Collection<String>): Boolean {
        val gid = groupId.takeIf { it.isNotBlank() } ?: return false
        val unique = taskIds.distinct()
        if (unique.isEmpty()) return false
        val seed = _state.value.tasks.firstOrNull { it.hubGroupId == gid } ?: return false
        val homeColumn = seed.columnId
        val toAdd = unique.mapNotNull { id ->
            _state.value.tasks.find { it.id == id && it.columnId == homeColumn }
        }
        if (toAdd.isEmpty()) return false
        viewModelScope.launch {
            withAllTaskLocks {
                val allActive = _state.value.tasks
                    .filter { it.columnId == homeColumn && !it.isCompleted && !it.isHidden }
                    .sortedBy { it.position }
                val memberIds = (
                    allActive.filter { it.hubGroupId == gid }.map { it.id } + toAdd.map { it.id }
                    ).toSet()
                val packed = HubGroupOrdering.packGroupAtTop(allActive, memberIds)
                val withGroupFlag = _state.value.tasks.map { t ->
                    if (t.id in memberIds) t.copy(hubGroupId = gid) else t
                }
                repository.updateTasks(withGroupFlag.filter { it.id in memberIds })
                moveTaskUseCase.reorderHomeTasks(homeColumn, packed.map { it.id }, withGroupFlag)
            }
        }
        return true
    }

    fun ungroupHubTasks(taskIds: Collection<String>): Set<String> {
        val groupIds = taskIds.mapNotNull { id ->
            _state.value.tasks.find { it.id == id }?.hubGroupId
        }.toSet()
        viewModelScope.launch {
            withAllTaskLocks {
                val updated = taskIds.mapNotNull { id ->
                    _state.value.tasks.find { it.id == id }?.copy(hubGroupId = null)
                }
                if (updated.isNotEmpty()) repository.updateTasks(updated)
            }
        }
        return groupIds
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
                val projectTasks = projectTasksFor(projectId)
                if (!task.statsExcluded) {
                    val kind = when {
                        task.isCompleted -> StatsJournalEntry.KIND_DONE
                        task.isNotDone -> StatsJournalEntry.KIND_NOT_DONE
                        else -> StatsJournalEntry.KIND_OPEN
                    }
                    val entry = com.example.kaizenkanban.domain.stats.ProjectStatsCalculator.journalFromTask(
                        task = task,
                        projectId = projectId,
                        links = _state.value.taskLinks,
                        projectTasks = projectTasks,
                        kind = kind
                    )
                    repository.insertStatsJournal(entry)
                }
                recordRecurringSkipIfNeeded(task)
                deleteTaskUseCase(task.id)
            }
        }
    }

    /** Journal all archived tasks (that count in stats), then delete them from Archive. */
    fun clearArchiveToJournal(projectId: String) {
        if (_clearArchiveProgress.value != null) return
        viewModelScope.launch {
            try {
                withAllTaskLocks {
                    val projectTasks = projectTasksFor(projectId)
                    val archived = projectTasks.filter { it.isBoardArchived }
                    if (archived.isEmpty()) return@withAllTaskLocks
                    val links = _state.value.taskLinks
                    val total = archived.size
                    _clearArchiveProgress.value = ClearArchiveProgress(processed = 0, total = total)
                    archived.forEachIndexed { index, task ->
                        if (!task.statsExcluded) {
                            val kind = when {
                                task.isCompleted -> StatsJournalEntry.KIND_DONE
                                task.isNotDone -> StatsJournalEntry.KIND_NOT_DONE
                                else -> StatsJournalEntry.KIND_OPEN
                            }
                            val entry = com.example.kaizenkanban.domain.stats.ProjectStatsCalculator.journalFromTask(
                                task = task,
                                projectId = projectId,
                                links = links,
                                projectTasks = projectTasks,
                                kind = kind
                            )
                            repository.insertStatsJournal(entry)
                        }
                        recordRecurringSkipIfNeeded(task)
                        deleteTaskUseCase(task.id)
                        _clearArchiveProgress.value = ClearArchiveProgress(
                            processed = index + 1,
                            total = total
                        )
                        kotlinx.coroutines.yield()
                    }
                }
            } finally {
                _clearArchiveProgress.value = null
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
            val prefs = preferences
            if (prefs != null && prefs.getGoalsHubId(column.boardId) == columnId) {
                prefs.setGoalsHubId(column.boardId, null)
            }
            deleteColumnUseCase(columnId)
        }
    }

    /** Settings DB browser: delete hub even if it is the last one on the board. */
    fun hardDeleteColumn(columnId: String) {
        viewModelScope.launch {
            val column = _state.value.columns.find { it.id == columnId } ?: return@launch
            val prefs = preferences
            if (prefs != null && prefs.getGoalsHubId(column.boardId) == columnId) {
                prefs.setGoalsHubId(column.boardId, null)
            }
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
                val skipKey = recordRecurringSkipIfNeeded(task)
                deletedSnapshots[taskId] = DeletedTaskSnapshot(
                    task = task,
                    comments = _state.value.comments.filter { it.taskId == taskId },
                    links = _state.value.taskLinks.filter { it.parentId == taskId || it.childId == taskId },
                    recurringSkipKey = skipKey
                )
                deleteTaskUseCase(taskId)
            }
        }
    }

    /** Settings DB browser: permanent delete with no undo snapshot. */
    fun hardDeleteTask(taskId: String) {
        viewModelScope.launch {
            withAllTaskLocks {
                val task = _state.value.tasks.find { it.id == taskId } ?: return@withAllTaskLocks
                recordRecurringSkipIfNeeded(task)
                deleteTaskUseCase(taskId)
                deletedSnapshots.remove(taskId)
            }
        }
    }

    fun undoDelete(taskId: String) {
        viewModelScope.launch {
            withAllTaskLocks {
                val snapshot = deletedSnapshots.remove(taskId) ?: return@withAllTaskLocks
                snapshot.recurringSkipKey?.let { key ->
                    val templateId = snapshot.task.recurringTemplateId
                    if (!templateId.isNullOrBlank()) {
                        val template = _state.value.recurringTemplates.find { it.id == templateId }
                        if (template != null) {
                            repository.updateRecurringTemplate(
                                EnsureRecurringInstancesUseCase.withoutSkippedOccurrence(template, key)
                            )
                        }
                    }
                }
                repository.insertTask(snapshot.task)
                snapshot.comments.forEach { repository.insertComment(it) }
                snapshot.links.forEach { repository.insertTaskLink(it) }
            }
        }
    }

    /**
     * If [task] is a recurring instance, mark that day/time as skipped so ensure
     * does not recreate it until the next period. Returns the skip key if recorded.
     */
    private suspend fun recordRecurringSkipIfNeeded(task: Task): String? {
        val templateId = task.recurringTemplateId?.takeIf { it.isNotBlank() } ?: return null
        val key = EnsureRecurringInstancesUseCase.occurrenceKeyForTask(task) ?: return null
        val template = _state.value.recurringTemplates.find { it.id == templateId } ?: return null
        repository.updateRecurringTemplate(
            EnsureRecurringInstancesUseCase.withSkippedOccurrence(template, key)
        )
        return key
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

    /**
     * @return null on success, or an error key: "limit" | "pro" | "error"
     * Caller checks Pro before calling; this enforces the per-task limit.
     */
    fun addTaskPhoto(taskId: String, projectId: String, uri: Uri, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val store = attachmentStore ?: return@withContext "error"
                    val current = _state.value.taskAttachments.count { it.taskId == taskId }
                    if (current >= TaskAttachment.MAX_PER_TASK) return@withContext "limit"
                    val id = UUID.randomUUID().toString()
                    val relative = store.saveImageFromUri(uri, projectId, taskId, id)
                    val attachment = TaskAttachment(
                        id = id,
                        taskId = taskId,
                        relativePath = relative,
                        mimeType = "image/jpeg",
                        createdAt = System.currentTimeMillis(),
                        sortOrder = current
                    )
                    repository.insertTaskAttachment(attachment)
                    null
                } catch (_: Exception) {
                    "error"
                }
            }
            onResult(result)
        }
    }

    fun deleteTaskPhoto(attachmentId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteTaskAttachment(attachmentId)
            }
        }
    }

    fun attachmentAbsolutePath(relativePath: String): String? =
        attachmentStore?.absoluteFile(relativePath)?.absolutePath

    /** Content URI or absolute file path for preview/thumbnail. */
    fun attachmentPreviewRef(relativePath: String): String? {
        val store = attachmentStore ?: return null
        return if (store.isContentUri(relativePath)) relativePath
        else store.absoluteFile(relativePath)?.absolutePath
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
            taskLinks = full.taskLinks.filter { it.parentId in taskIds && it.childId in taskIds },
            projectNotes = full.projectNotes.filter { it.projectId == projectId },
            noteLines = full.noteLines.filter { line ->
                full.projectNotes.any { it.id == line.noteId && it.projectId == projectId }
            },
            notePhotos = full.notePhotos.filter { photo ->
                full.projectNotes.any { it.id == photo.noteId && it.projectId == projectId }
            }
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
    private val ensureRecurringInstancesUseCase: EnsureRecurringInstancesUseCase,
    private val attachmentStore: TaskAttachmentStore? = null,
    private val notePhotoStore: GalleryNotePhotoStore? = null,
    private val preferences: KairosPreferences? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SharedViewModel(
                repository, initializeDatabaseUseCase, addTaskUseCase, updateTaskUseCase, moveTaskUseCase,
                addColumnUseCase, deleteColumnUseCase, renameColumnUseCase, deleteTaskUseCase, setDefaultBoardUseCase,
                addCommentUseCase, deleteCommentUseCase, addColumnCommentUseCase, deleteColumnCommentUseCase,
                moveTaskToBoardUseCase, exportDataUseCase, importDataUseCase, ensureRecurringInstancesUseCase,
                attachmentStore,
                notePhotoStore,
                preferences
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}