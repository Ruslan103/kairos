package com.example.kaizenkanban.ui.aiprompts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val project = state.projects.find { it.id == projectId }

    val projectBoardIds = remember(state.boards, projectId) {
        state.boards.filter { it.projectId == projectId }.map { it.id }.toSet()
    }
    val projectColumns = remember(state.columns, projectBoardIds) {
        state.columns.filter { it.boardId in projectBoardIds }
    }
    val projectColumnIds = remember(projectColumns) { projectColumns.map { it.id }.toSet() }
    val projectTasks = remember(state.tasks, projectColumnIds) {
        state.tasks.filter { it.columnId in projectColumnIds || it.linkedColumnIds.any { id -> id in projectColumnIds } }
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
            progressPercent = ProjectStatsCalculator.percent(progress)
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
                        onCopy = { copyPrompt(kind) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PromptKindCard(
    title: String,
    description: String,
    enabled: Boolean,
    onCopy: () -> Unit
) {
    val s = LocalAppStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
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
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onCopy,
                enabled = enabled,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = s.aiPromptsCopyAction
                )
            }
        }
    }
}
