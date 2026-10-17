package com.example.kaizenkanban.ui.aiprompts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.commands.KairosPlan
import com.example.kaizenkanban.domain.model.TaskLinkGraph
import com.example.kaizenkanban.domain.stats.GoalAiPromptBuilder
import com.example.kaizenkanban.domain.stats.GoalAiPromptKind
import com.example.kaizenkanban.domain.stats.GoalProgressCalculator
import com.example.kaizenkanban.domain.stats.ProjectStatsCalculator
import com.example.kaizenkanban.ui.components.MarqueeDropdownField
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPromptsScreen(
    projectId: String,
    viewModel: SharedViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val prefs = remember { KairosPreferences(context) }
    val project = state.projects.find { it.id == projectId }

    val projectBoardIds = remember(state.boards, projectId) {
        state.boards.filter { it.projectId == projectId }.map { it.id }.toSet()
    }
    val projectColumns = remember(state.columns, projectBoardIds) {
        state.columns.filter { it.boardId in projectBoardIds }
    }
    val projectColumnIds = remember(projectColumns) { projectColumns.map { it.id }.toSet() }
    val projectTasks = remember(state.tasks, projectColumnIds) {
        state.tasks.filter {
            !it.isHidden &&
                (it.columnId in projectColumnIds || it.linkedColumnIds.any { id -> id in projectColumnIds })
        }
    }
    val projectLinks = remember(state.taskLinks, projectTasks) {
        val ids = projectTasks.map { it.id }.toSet()
        state.taskLinks.filter { it.parentId in ids || it.childId in ids }
    }
    val templates = remember(state.recurringTemplates, projectId) {
        state.recurringTemplates.filter { it.projectId == projectId }
    }
    val goals = remember(projectTasks, projectLinks) {
        GoalProgressCalculator.resolveGoals(projectTasks, projectLinks)
    }

    var selectedGoalId by remember(goals) {
        mutableStateOf(goals.firstOrNull()?.id)
    }
    var goalMenuOpen by remember { mutableStateOf(false) }
    val selectedGoal = goals.find { it.id == selectedGoalId } ?: goals.firstOrNull()

    var commandText by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf<KairosPlan?>(null) }
    var applying by remember { mutableStateOf(false) }
    var rollingBack by remember { mutableStateOf(false) }
    val undoRevision by viewModel.kairosUndoRevision.collectAsState()
    val undoCount = remember(undoRevision, projectId) { viewModel.kairosUndoCount(projectId) }

    fun copyPrompt(kind: GoalAiPromptKind) {
        val goal = selectedGoal ?: run {
            Toast.makeText(context, s.aiPromptsNeedGoal, Toast.LENGTH_SHORT).show()
            return
        }
        val tasksById = projectTasks.associateBy { it.id }
        val progress = GoalProgressCalculator.progressOf(goal.id, tasksById, projectLinks)
        val childCount = TaskLinkGraph.childrenOf(projectLinks)[goal.id].orEmpty().size
        val prompt = GoalAiPromptBuilder.build(
            kind = kind,
            projectName = project?.let { s.localized(it.name) }.orEmpty(),
            goal = goal,
            projectTasks = projectTasks,
            links = projectLinks,
            columns = projectColumns,
            templates = templates,
            childCount = childCount,
            progressPercent = ProjectStatsCalculator.percent(progress),
            defaultReminderMinutes = prefs.reminderMinutesOfDay
        )
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(s.aiPromptKindTitle(kind), prompt))
        Toast.makeText(context, s.aiPromptsCopied, Toast.LENGTH_LONG).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.aiPromptsTitle, fontWeight = FontWeight.Bold)
                        if (project != null) {
                            Text(
                                s.localized(project.name),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = s.aiPromptsIntro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (goals.isEmpty()) {
                Text(
                    text = s.aiPromptsNoGoals,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ExposedDropdownMenuBox(
                    expanded = goalMenuOpen,
                    onExpandedChange = { goalMenuOpen = it }
                ) {
                    MarqueeDropdownField(
                        value = selectedGoal?.title.orEmpty(),
                        label = s.aiPromptsGoalLabel,
                        expanded = goalMenuOpen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = goalMenuOpen,
                        onDismissRequest = { goalMenuOpen = false }
                    ) {
                        goals.forEach { goal ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        goal.title,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    selectedGoalId = goal.id
                                    goalMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                GoalAiPromptKind.entries.forEach { kind ->
                    PromptKindCard(
                        title = s.aiPromptKindTitle(kind),
                        description = s.aiPromptKindHint(kind),
                        enabled = selectedGoal != null,
                        dark = kind == GoalAiPromptKind.COMMANDS,
                        onCopy = { copyPrompt(kind) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CommandsPanel,
                    contentColor = CommandsOnPanel
                ),
                border = BorderStroke(1.dp, Color(0xFFE5E5E5))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = s.aiCommandsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CommandsOnPanel
                    )
                    Text(
                        text = s.aiCommandsHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = CommandsOnPanelMuted
                    )
                    OutlinedTextField(
                        value = commandText,
                        onValueChange = {
                            commandText = it
                            plan = null
                        },
                        label = { Text(s.aiCommandsFieldLabel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp),
                        minLines = 6,
                        maxLines = 16,
                        colors = commandFieldColors()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CommandActionButton(
                            text = s.aiCommandsParse,
                            onClick = {
                                plan = viewModel.planKairosCommands(
                                    raw = commandText,
                                    projectId = projectId,
                                    selectedGoalId = selectedGoalId
                                )
                            },
                            enabled = commandText.isNotBlank(),
                            filled = false,
                            modifier = Modifier.weight(1f)
                        )
                        CommandActionButton(
                            text = s.aiCommandsApply,
                            onClick = {
                                val p = plan ?: return@CommandActionButton
                                if (!p.canApply || applying) return@CommandActionButton
                                applying = true
                                viewModel.applyKairosPlan(p, projectId) { result ->
                                    applying = false
                                    val msg = buildString {
                                        append(
                                            s.aiCommandsApplied(
                                                result.createdGoals,
                                                result.createdTasks,
                                                result.createdHabits,
                                                result.createdLinks
                                            )
                                        )
                                        if (result.errors.isNotEmpty()) {
                                            append(" · ")
                                            append(result.errors.take(2).joinToString("; "))
                                        }
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (result.createdGoals + result.createdTasks +
                                        result.createdHabits + result.createdLinks > 0
                                    ) {
                                        commandText = ""
                                        plan = null
                                    }
                                }
                            },
                            enabled = plan?.canApply == true && !applying,
                            filled = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CommandActionButton(
                            text = s.aiCommandsClear,
                            onClick = {
                                commandText = ""
                                plan = null
                            },
                            enabled = commandText.isNotBlank() || plan != null,
                            filled = false,
                            modifier = Modifier.weight(1f)
                        )
                        CommandActionButton(
                            text = s.aiCommandsUndo,
                            onClick = {
                                if (rollingBack) return@CommandActionButton
                                rollingBack = true
                                viewModel.undoLastKairosApply(projectId) { undone ->
                                    rollingBack = false
                                    Toast.makeText(
                                        context,
                                        if (undone) s.aiCommandsUndone else s.aiCommandsUndoEmpty,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            enabled = undoCount > 0 && !rollingBack,
                            filled = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (undoCount > 0) {
                        Text(
                            text = s.aiCommandsUndoLeft(undoCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = CommandsOnPanelMuted
                        )
                    }
                }
            }

            val currentPlan = plan
            if (currentPlan != null) {
                if (currentPlan.errors.isNotEmpty()) {
                    Text(
                        text = s.aiCommandsErrors,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    currentPlan.errors.forEach { err ->
                        Text(
                            text = "• $err",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (currentPlan.warnings.isNotEmpty()) {
                    Text(
                        text = s.aiCommandsWarnings,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    currentPlan.warnings.forEach { w ->
                        Text(
                            text = "• $w",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (currentPlan.previewLines.isEmpty() && currentPlan.errors.isEmpty()) {
                    Text(
                        text = s.aiCommandsPreviewEmpty,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    currentPlan.previewLines.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private val CommandsPanel = Color(0xFFF5F5F5)
private val CommandsOnPanel = Color(0xFF111111)
private val CommandsOnPanelMuted = Color(0xFF525252)
private val CommandsField = Color(0xFFFFFFFF)
private val CommandsAccent = Color(0xFF097969)
private val CommandsAccentSoft = Color(0xFFD7F2F3)

@Composable
private fun commandFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = CommandsOnPanel,
    unfocusedTextColor = CommandsOnPanel,
    disabledTextColor = CommandsOnPanelMuted,
    focusedContainerColor = CommandsField,
    unfocusedContainerColor = CommandsField,
    disabledContainerColor = CommandsField,
    cursorColor = CommandsAccent,
    focusedBorderColor = CommandsAccent,
    unfocusedBorderColor = Color(0xFFE5E5E5),
    focusedLabelColor = CommandsAccent,
    unfocusedLabelColor = CommandsOnPanelMuted,
    focusedPlaceholderColor = CommandsOnPanelMuted,
    unfocusedPlaceholderColor = CommandsOnPanelMuted
)

@Composable
private fun CommandActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    filled: Boolean,
    modifier: Modifier = Modifier
) {
    val label = @Composable {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 2,
            softWrap = true,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
    val padding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
    if (filled) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 48.dp),
            contentPadding = padding,
            colors = ButtonDefaults.buttonColors(
                containerColor = CommandsAccent,
                contentColor = Color.White,
                disabledContainerColor = CommandsAccent.copy(alpha = 0.35f),
                disabledContentColor = Color.White.copy(alpha = 0.7f)
            )
        ) { label() }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.heightIn(min = 48.dp),
            contentPadding = padding,
            border = BorderStroke(
                1.5.dp,
                if (enabled) CommandsAccent else CommandsAccent.copy(alpha = 0.35f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = CommandsAccent,
                disabledContentColor = CommandsAccent.copy(alpha = 0.4f),
                containerColor = Color.White
            )
        ) { label() }
    }
}

@Composable
private fun PromptKindCard(
    title: String,
    description: String,
    enabled: Boolean,
    dark: Boolean = false,
    onCopy: () -> Unit
) {
    val s = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dark) {
                CommandsAccentSoft
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            },
            contentColor = CommandsOnPanel
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = CommandsOnPanel
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = CommandsOnPanelMuted
                )
            }
            IconButton(
                onClick = onCopy,
                enabled = enabled,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = s.aiPromptsCopyAction,
                    tint = if (enabled) CommandsAccent else CommandsOnPanelMuted
                )
            }
        }
    }
}
