package com.example.kaizenkanban.ui.stats

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.model.Column as HubColumn
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.stats.DailyActivityPoint
import com.example.kaizenkanban.domain.stats.ProgressPoint
import com.example.kaizenkanban.domain.stats.ProjectStatsCalculator
import com.example.kaizenkanban.domain.stats.StatsPeriod
import com.example.kaizenkanban.ui.board.TaskLinksDialog
import com.example.kaizenkanban.ui.calendar.localMillisToUtcPicker
import com.example.kaizenkanban.ui.calendar.startOfLocalDayMillis
import com.example.kaizenkanban.ui.calendar.utcPickerMillisToLocalNoon
import com.example.kaizenkanban.ui.components.DialogSectionDivider
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.taskmeta.TaskCriteriaDialog
import com.example.kaizenkanban.ui.theme.chartForecast
import com.example.kaizenkanban.ui.theme.chartForecastBrush
import com.example.kaizenkanban.ui.theme.chartProgress
import com.example.kaizenkanban.ui.theme.chartProgressBrush
import com.example.kaizenkanban.ui.theme.chartToward
import com.example.kaizenkanban.ui.theme.chartTowardBrush
import com.example.kaizenkanban.ui.theme.chartTrivia
import com.example.kaizenkanban.ui.theme.chartTriviaBrush
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    projectId: String,
    viewModel: SharedViewModel,
    onBack: () -> Unit
) {
    val s = LocalAppStrings.current
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val prefs = remember { KairosPreferences(context) }
    var epochTick by remember { mutableIntStateOf(0) }
    val epochMillis = remember(epochTick) { prefs.statsEpochMillis }
    val project = state.projects.find { it.id == projectId }
    var period by rememberSaveable { mutableStateOf(StatsPeriod.NOW.name) }
    val selectedPeriod = remember(period) {
        runCatching { StatsPeriod.valueOf(period) }.getOrDefault(StatsPeriod.WEEK)
    }
    var selectedGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var moreOpen by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var showEpochPicker by remember { mutableStateOf(false) }
    var legacyPinsMigrated by rememberSaveable(projectId) { mutableStateOf(false) }

    val projectBoardIds = remember(state.boards, projectId) {
        state.boards.filter { it.projectId == projectId }.map { it.id }.toSet()
    }
    val projectColumns = remember(state.columns, projectBoardIds) {
        state.columns.filter { it.boardId in projectBoardIds }.sortedBy { it.position }
    }
    val projectColumnIds = remember(projectColumns) { projectColumns.map { it.id }.toSet() }
    val projectTasks = remember(state.tasks, projectColumnIds) {
        state.tasks.filter { it.columnId in projectColumnIds }
    }
    val journal = remember(state.statsJournal, projectId) {
        state.statsJournal.filter { it.projectId == projectId }
    }
    val templates = remember(state.recurringTemplates, projectId) {
        state.recurringTemplates.filter { it.projectId == projectId }
    }

    LaunchedEffect(projectId, projectTasks, legacyPinsMigrated) {
        if (legacyPinsMigrated) return@LaunchedEffect
        val pinned = prefs.pinnedStatsGoalIds(projectId)
        if (pinned.isNotEmpty()) {
            pinned.forEach { id ->
                val task = projectTasks.find { it.id == id }
                if (task != null && !task.isGoal) {
                    viewModel.updateTask(task.copy(isGoal = true))
                }
            }
            prefs.setPinnedStatsGoalIds(projectId, emptySet())
            prefs.setHiddenStatsGoalIds(projectId, emptySet())
        }
        legacyPinsMigrated = true
    }

    val snapshot = remember(
        projectTasks, state.taskLinks, templates, selectedPeriod, selectedGoalId,
        epochMillis, journal
    ) {
        ProjectStatsCalculator.compute(
            projectTasks = projectTasks,
            links = state.taskLinks,
            templates = templates,
            period = selectedPeriod,
            epochMillis = epochMillis,
            journal = journal,
            selectedGoalId = selectedGoalId
        )
    }
    LaunchedEffect(snapshot.selectedGoal?.goalId) {
        val id = snapshot.selectedGoal?.goalId
        if (id != null && selectedGoalId == null) selectedGoalId = id
    }

    val dateFmt = remember {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.statsTitle, fontWeight = FontWeight.Bold)
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
                },
                actions = {
                    IconButton(onClick = { moreOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = s.statsMore)
                    }
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(s.statsGuideMenu) },
                            onClick = {
                                moreOpen = false
                                showGuide = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(s.statsEpochTitle)
                                    Text(
                                        dateFmt.format(Date(epochMillis)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            onClick = {
                                moreOpen = false
                                showEpochPicker = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(s.statsResetPeriod) },
                            onClick = {
                                prefs.advanceStatsEpochTo(System.currentTimeMillis())
                                epochTick++
                                moreOpen = false
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (showGuide) {
            StatsGuideDialog(onDismiss = { showGuide = false })
        }
        if (showEpochPicker) {
            val epochPickerState = rememberDatePickerState(
                initialSelectedDateMillis = localMillisToUtcPicker(epochMillis)
            )
            DatePickerDialog(
                onDismissRequest = { showEpochPicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selected = epochPickerState.selectedDateMillis
                            if (selected != null) {
                                val noon = utcPickerMillisToLocalNoon(selected)
                                prefs.statsEpochMillis = startOfLocalDayMillis(noon)
                                epochTick++
                            }
                            showEpochPicker = false
                        }
                    ) { Text(s.ok) }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                prefs.resetStatsEpochToDefault()
                                epochTick++
                                showEpochPicker = false
                            }
                        ) { Text(s.statsEpochResetYear) }
                        TextButton(onClick = { showEpochPicker = false }) { Text(s.cancel) }
                    }
                }
            ) {
                DatePicker(state = epochPickerState)
            }
        }
        val density = LocalDensity.current
        var headerHeightPx by remember { mutableFloatStateOf(0f) }
        var headerCollapsePx by remember { mutableFloatStateOf(0f) }
        val headerConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val dy = available.y
                    if (dy < 0f && headerHeightPx > 0f && headerCollapsePx < headerHeightPx) {
                        val room = headerHeightPx - headerCollapsePx
                        val consume = (-dy).coerceAtMost(room)
                        headerCollapsePx += consume
                        return Offset(0f, -consume)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    val dy = available.y
                    if (dy > 0f && headerCollapsePx > 0f) {
                        val consume = dy.coerceAtMost(headerCollapsePx)
                        headerCollapsePx -= consume
                        return Offset(0f, consume)
                    }
                    return Offset.Zero
                }
            }
        }
        val visibleHeaderPx = (headerHeightPx - headerCollapsePx).coerceAtLeast(0f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(headerConnection)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (headerHeightPx > 0f) {
                            Modifier
                                .height(with(density) { visibleHeaderPx.toDp() })
                                .clipToBounds()
                        } else {
                            Modifier
                        }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (headerHeightPx > 0f) {
                                Modifier.requiredHeight(with(density) { headerHeightPx.toDp() })
                            } else {
                                Modifier
                            }
                        )
                        .onGloballyPositioned { coords ->
                            val h = coords.size.height.toFloat()
                            if (h > headerHeightPx) headerHeightPx = h
                        }
                        .offset {
                            IntOffset(
                                0,
                                if (headerHeightPx > 0f) -headerCollapsePx.roundToInt() else 0
                            )
                        }
                ) {
                    ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
                        listOf(
                            s.statsTabGoals,
                            s.statsTabPeriod,
                            s.statsTabArchive
                        ).forEachIndexed { i, title ->
                            Tab(
                                selected = tab == i,
                                onClick = { tab = i },
                                text = { Text(title) }
                            )
                        }
                    }
                }
            }

            when (tab) {
                0 -> GoalsTab(
                    snapshot = snapshot,
                    selectedPeriod = selectedPeriod,
                    onPeriodChange = { period = it.name },
                    selectedGoalId = selectedGoalId,
                    onSelectGoal = { selectedGoalId = it },
                    onResetGoalEpoch = { viewModel.resetGoalStatsEpoch(it) }
                )
                1 -> PeriodTab(
                    snapshot = snapshot,
                    selectedPeriod = selectedPeriod,
                    onPeriodChange = { period = it.name }
                )
                2 -> ArchiveTab(
                    projectId = projectId,
                    projectTasks = projectTasks,
                    projectColumns = projectColumns,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun StatsGuideDialog(onDismiss: () -> Unit) {
    val s = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.statsGuideTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GuideSection(s.statsGuideGoalsTitle, s.statsGuideGoalsBody)
                DialogSectionDivider()
                GuideSection(s.statsGuideGoalProgressTitle, s.statsGuideGoalProgressBody)
                DialogSectionDivider()
                GuideSection(s.statsGuideRhythmTitle, s.statsGuideRhythmBody)
                DialogSectionDivider()
                GuideSection(s.statsGuideTriviaTitle, s.statsGuideTriviaBody)
                DialogSectionDivider()
                GuideSection(s.statsGuideAssessmentTitle, s.statsGuideAssessmentBody)
                DialogSectionDivider()
                GuideSection(s.statsGuideChanceTitle, s.statsGuideChanceBody)
                DialogSectionDivider()
                GuideSection(s.statsGuidePeriodAssessmentTitle, s.statsGuidePeriodAssessmentBody)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s.done) }
        }
    )
}

@Composable
private fun GuideSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsPeriodChips(
    selectedPeriod: StatsPeriod,
    onPeriodChange: (StatsPeriod) -> Unit
) {
    val s = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedPeriod == StatsPeriod.NOW,
            onClick = { onPeriodChange(StatsPeriod.NOW) },
            label = { Text(s.statsPeriodLabel(StatsPeriod.NOW)) }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsPeriod.entries.filter { it != StatsPeriod.NOW }.forEach { p ->
                FilterChip(
                    selected = selectedPeriod == p,
                    onClick = { onPeriodChange(p) },
                    label = { Text(s.statsPeriodLabel(p)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalsTab(
    snapshot: com.example.kaizenkanban.domain.stats.ProjectStatsSnapshot,
    selectedPeriod: StatsPeriod,
    onPeriodChange: (StatsPeriod) -> Unit,
    selectedGoalId: String?,
    onSelectGoal: (String) -> Unit,
    onResetGoalEpoch: (String) -> Unit
) {
    val s = LocalAppStrings.current
    val goal = snapshot.selectedGoal
    val fromStart = snapshot.period == StatsPeriod.NOW
    var confirmReset by remember { mutableStateOf(false) }
    var goalMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatsPeriodChips(
            selectedPeriod = selectedPeriod,
            onPeriodChange = onPeriodChange
        )

        if (snapshot.goals.isNotEmpty()) {
            val selectedLabel = goal?.let { g ->
                val pct = ProjectStatsCalculator.percent(
                    if (fromStart) g.progress else g.periodDelta
                )
                val archive = if (g.isArchived) " (${s.statsGoalArchivedBadge})" else ""
                val pctText = if (fromStart) "$pct%" else {
                    val sign = if (pct >= 0) "+" else ""
                    "$sign$pct%"
                }
                "${g.goalTitle}$archive · $pctText"
            } ?: s.statsGoalLabel

            ExposedDropdownMenuBox(
                expanded = goalMenuExpanded,
                onExpandedChange = { goalMenuExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(s.statsGoalLabel) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalMenuExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = goalMenuExpanded,
                    onDismissRequest = { goalMenuExpanded = false }
                ) {
                    snapshot.goals.forEach { g ->
                        val pct = ProjectStatsCalculator.percent(
                            if (fromStart) g.progress else g.periodDelta
                        )
                        val pctText = if (fromStart) {
                            "$pct%"
                        } else {
                            val sign = if (pct >= 0) "+" else ""
                            "$sign$pct%"
                        }
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (g.isArchived) {
                                            "${g.goalTitle} (${s.statsGoalArchivedBadge})"
                                        } else {
                                            g.goalTitle
                                        },
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = pctText,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onSelectGoal(g.goalId)
                                goalMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        ReviewSectionCard(
            title = if (fromStart) s.statsFromStartSection else s.statsHowPeriodWent(snapshot.period)
        ) {
            if (goal == null) {
                Text(s.statsNoGoals, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (fromStart) {
                val pct = ProjectStatsCalculator.percent(goal.progress)
                Text(s.statsGoalProgress, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { goal.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = chartProgress
                )
                Text(
                    s.statsGoalChildren(goal.childCount, goal.childWeightSum),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(s.statsPeriodChangesSection, fontWeight = FontWeight.SemiBold)
                val deltaPct = ProjectStatsCalculator.percent(goal.periodDelta)
                val deltaColor = when {
                    deltaPct > 0 -> MaterialTheme.colorScheme.primary
                    deltaPct < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(s.statsProgressDelta, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = s.statsPeriodDelta(deltaPct, snapshot.period),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = deltaColor
                )
                Spacer(Modifier.height(8.dp))
                Text(s.statsRecurring(snapshot.recurringFact, snapshot.recurringPlan))
                Text(s.statsTowardGoals(snapshot.towardGoalsCount, snapshot.towardGoalsWeight))
                Text(s.statsTrivia(snapshot.triviaCount, snapshot.triviaWeight))
            }
        }

        if (fromStart && goal != null && snapshot.hatP != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp),
                    title = s.statsForecastTitle,
                    percent = ProjectStatsCalculator.percent(snapshot.hatP),
                    progress = snapshot.hatP,
                    accent = chartProgress
                )
                MetricCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp),
                    title = s.statsChanceTitle,
                    percent = ProjectStatsCalculator.percent(snapshot.chanceToComplete ?: 0f),
                    progress = snapshot.chanceToComplete ?: 0f,
                    accent = chartForecast
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    s.statsForecastRhythmBonus(ProjectStatsCalculator.percent(snapshot.rhythmBonus)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    s.statsForecastTriviaPenalty(ProjectStatsCalculator.percent(snapshot.triviaPenalty)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (fromStart && snapshot.goalProgressSeries.isNotEmpty()) {
            Text(s.statsChartProgress, fontWeight = FontWeight.SemiBold)
            ProgressLineChart(snapshot.goalProgressSeries)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(chartProgress, s.statsChartProgressLegend)
                LegendDot(chartForecast, s.statsChartForecastLegend)
            }
        }

        if (fromStart && goal != null) {
            TextButton(
                onClick = { confirmReset = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(s.statsResetGoalEpoch, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmReset && goal != null) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(s.statsResetGoalEpoch, fontWeight = FontWeight.Bold) },
            text = { Text(s.statsResetGoalEpochHint) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        onResetGoalEpoch(goal.goalId)
                    }
                ) { Text(s.ok, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(s.cancel) }
            }
        )
    }
}

@Composable
private fun ReviewSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    percent: Int,
    progress: Float,
    accent: androidx.compose.ui.graphics.Color,
    caption: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "$percent%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = accent
            )
            if (caption != null) {
                Text(
                    caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PeriodTab(
    snapshot: com.example.kaizenkanban.domain.stats.ProjectStatsSnapshot,
    selectedPeriod: StatsPeriod,
    onPeriodChange: (StatsPeriod) -> Unit
) {
    val s = LocalAppStrings.current
    val fromStart = snapshot.period == StatsPeriod.NOW
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatsPeriodChips(
            selectedPeriod = selectedPeriod,
            onPeriodChange = onPeriodChange
        )

        ReviewSectionCard(
            title = if (fromStart) s.statsFromStartSection else s.statsHowPeriodWent(snapshot.period)
        ) {
            if (fromStart) {
                snapshot.periodHatP?.let { overall ->
                    Text(s.statsPeriodOverallTitle, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${ProjectStatsCalculator.percent(overall)}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { overall.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = chartProgress
                    )
                    Text(
                        s.statsPeriodOverallHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            s.statsForecastRhythmBonus(
                                ProjectStatsCalculator.percent(snapshot.rhythmBonus)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            s.statsForecastTriviaPenalty(
                                ProjectStatsCalculator.percent(snapshot.triviaPenalty)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    snapshot.towardShare?.let { share ->
                        Text(
                            "${s.statsPeriodTowardShare}: ${ProjectStatsCalculator.percent(share)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                Text(s.statsPeriodChangesSection, fontWeight = FontWeight.SemiBold)
                val deltaPct = ProjectStatsCalculator.percent(snapshot.periodHatP ?: 0f)
                val deltaColor = when {
                    deltaPct > 0 -> MaterialTheme.colorScheme.primary
                    deltaPct < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(s.statsProgressDelta, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = s.statsPeriodDelta(deltaPct, snapshot.period),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = deltaColor
                )
                snapshot.towardShare?.let { share ->
                    Text(
                        "${s.statsPeriodTowardShare}: ${ProjectStatsCalculator.percent(share)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Text(s.statsPeriodSummary, fontWeight = FontWeight.Bold)
            Text(s.statsDoneTotal(snapshot.doneInPeriod))
            Text(s.statsTowardGoals(snapshot.towardGoalsCount, snapshot.towardGoalsWeight))
            Text(s.statsTrivia(snapshot.triviaCount, snapshot.triviaWeight))
            Text(s.statsRecurring(snapshot.recurringFact, snapshot.recurringPlan))
            Text(s.statsRated(snapshot.ratedDone, snapshot.doneInPeriod))
        }

        if (snapshot.dailyActivity.isNotEmpty()) {
            Text(s.statsChartActivity, fontWeight = FontWeight.SemiBold)
            ActivityBarChart(snapshot.dailyActivity)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(chartToward, s.statsChartToward)
                LegendDot(chartTrivia, s.statsChartTrivia)
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ArchiveTab(
    projectId: String,
    projectTasks: List<Task>,
    projectColumns: List<HubColumn>,
    viewModel: SharedViewModel
) {
    val s = LocalAppStrings.current
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var segment by rememberSaveable { mutableIntStateOf(0) }
    val archived = remember(projectTasks) { projectTasks.filter { it.isBoardArchived } }
    val done = remember(archived) { archived.filter { it.isCompleted } }
    val notDone = remember(archived) { archived.filter { it.isNotDone } }
    val list = if (segment == 0) done else notDone
    var linksTask by remember { mutableStateOf<Task?>(null) }
    var settingsTask by remember { mutableStateOf<Task?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = segment == 0,
                onClick = { segment = 0 },
                label = { Text("${s.statsArchiveDone} (${done.size})") }
            )
            FilterChip(
                selected = segment == 1,
                onClick = { segment = 1 },
                label = { Text("${s.statsArchiveNotDone} (${notDone.size})") }
            )
        }
        if (segment == 1) {
            Text(
                s.statsArchiveNotDoneHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (list.isEmpty()) {
                Text(
                    s.statsArchiveEmpty,
                    modifier = Modifier.padding(top = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val columnsById = remember(projectColumns) { projectColumns.associateBy { it.id } }
                val byHub = remember(list) { list.groupBy { it.columnId } }
                projectColumns.asReversed().forEach { col ->
                    val hubTasks = byHub[col.id].orEmpty()
                    if (hubTasks.isEmpty()) return@forEach
                    Text(
                        s.localized(col.title),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    hubTasks.forEach { task ->
                        ArchiveTaskRow(
                            task = task,
                            projectId = projectId,
                            viewModel = viewModel,
                            onOpenSettings = { settingsTask = task },
                            onOpenLinks = { linksTask = task }
                        )
                    }
                }
                val orphan = list.filter { it.columnId !in columnsById }
                if (orphan.isNotEmpty()) {
                    Text(
                        s.unknownHub,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    orphan.forEach { task ->
                        ArchiveTaskRow(
                            task = task,
                            projectId = projectId,
                            viewModel = viewModel,
                            onOpenSettings = { settingsTask = task },
                            onOpenLinks = { linksTask = task }
                        )
                    }
                }
            }
            if (archived.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = { confirmClear = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(s.statsClearArchive, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    linksTask?.let { linked ->
        val live = state.tasks.find { it.id == linked.id } ?: linked
        TaskLinksDialog(
            task = live,
            allTasks = projectTasks,
            columns = projectColumns,
            links = state.taskLinks,
            onAddParent = { parentId ->
                val ok = viewModel.addTaskLink(parentId, live.id)
                if (!ok) Toast.makeText(context, s.taskLinkCycleRejected, Toast.LENGTH_SHORT).show()
                ok
            },
            onAddChild = { childId ->
                val ok = viewModel.addTaskLink(live.id, childId)
                if (!ok) Toast.makeText(context, s.taskLinkCycleRejected, Toast.LENGTH_SHORT).show()
                ok
            },
            onRemoveLink = { parentId, childId -> viewModel.removeTaskLink(parentId, childId) },
            onDismiss = { linksTask = null },
            onCycleRejected = {
                Toast.makeText(context, s.taskLinkCycleRejected, Toast.LENGTH_SHORT).show()
            }
        )
    }

    settingsTask?.let { target ->
        val live = state.tasks.find { it.id == target.id } ?: target
        TaskCriteriaDialog(
            task = live,
            onDismiss = { settingsTask = null },
            onUpdate = { viewModel.updateTask(it) },
            onQuality = { q -> viewModel.setTaskCompletionQuality(live.id, q) }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(s.statsClearArchive, fontWeight = FontWeight.Bold) },
            text = { Text(s.statsClearArchiveConfirm(archived.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        viewModel.clearArchiveToJournal(projectId)
                    }
                ) {
                    Text(s.clear, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(s.cancel)
                }
            }
        )
    }
}

@Composable
private fun ArchiveTaskRow(
    task: Task,
    projectId: String,
    viewModel: SharedViewModel,
    onOpenSettings: () -> Unit,
    onOpenLinks: () -> Unit
) {
    val s = LocalAppStrings.current
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                task.statsExcluded -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
                task.isNotDone -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (task.statsExcluded) {
            androidx.compose.foundation.BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.tertiary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.statsExcluded) {
                    Text(
                        s.statsExcludeFromStats,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (task.isGoal) {
                    Text(
                        s.statsGoalLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = chartProgress,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = s.statsMore)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (task.isCompleted) {
                        DropdownMenuItem(
                            text = { Text(s.statsEditTask) },
                            onClick = {
                                menuOpen = false
                                onOpenSettings()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(s.manageTaskLinks) },
                        onClick = {
                            menuOpen = false
                            onOpenLinks()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (task.isGoal) s.unmarkAsGoal else s.markAsGoal)
                        },
                        onClick = {
                            menuOpen = false
                            viewModel.updateTask(task.copy(isGoal = !task.isGoal))
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (task.statsExcluded) s.statsIncludeInStats
                                else s.statsExcludeFromStats
                            )
                        },
                        onClick = {
                            menuOpen = false
                            viewModel.setTaskStatsExcluded(task.id, !task.statsExcluded)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(s.statsDeleteToJournal) },
                        onClick = {
                            menuOpen = false
                            viewModel.deleteArchivedTaskToJournal(task.id, projectId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityBarChart(points: List<DailyActivityPoint>) {
    val s = LocalAppStrings.current
    val max = points.maxOfOrNull { it.towardCount + it.triviaCount }?.coerceAtLeast(1) ?: 1
    Column {
        Text(
            "0 … $max ${s.statsChartAxisCount}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(vertical = 8.dp)
        ) {
            val n = points.size.coerceAtLeast(1)
            val barW = size.width / (n * 1.4f)
            val gap = barW * 0.4f
            points.forEachIndexed { i, p ->
                val total = (p.towardCount + p.triviaCount).toFloat()
                val h = if (total <= 0f) 0f else (total / max) * size.height
                val x = i * (barW + gap)
                val towardH = if (total <= 0f) 0f else (p.towardCount / total) * h
                val triviaH = h - towardH
                drawRect(
                    brush = chartTowardBrush,
                    topLeft = Offset(x, size.height - towardH),
                    size = Size(barW, towardH)
                )
                drawRect(
                    brush = chartTriviaBrush,
                    topLeft = Offset(x, size.height - h),
                    size = Size(barW, triviaH)
                )
            }
        }
    }
}

@Composable
private fun ProgressLineChart(points: List<ProgressPoint>) {
    val guide = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box {
        Text(
            "+100%",
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Text(
            "0%",
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Text(
            "−100%",
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(start = 40.dp, top = 8.dp, bottom = 8.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            drawLine(
                guide,
                Offset(0f, size.height / 2),
                Offset(size.width, size.height / 2),
                strokeWidth = 1f
            )
            fun xAt(i: Int): Float =
                if (points.size == 1) size.width / 2
                else i * size.width / (points.size - 1)

            fun yAt(value: Float): Float =
                size.height * (1f - (value.coerceIn(-1f, 1f) + 1f) / 2f)

            val progressPath = Path()
            points.forEachIndexed { i, p ->
                val x = xAt(i)
                val y = yAt(p.progress)
                if (i == 0) progressPath.moveTo(x, y) else progressPath.lineTo(x, y)
            }
            drawPath(
                progressPath,
                brush = chartProgressBrush,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            val forecastPts = points.mapIndexedNotNull { i, p ->
                p.forecast?.let { i to it }
            }
            if (forecastPts.size >= 2) {
                val forecastPath = Path()
                forecastPts.forEachIndexed { idx, (i, v) ->
                    val x = xAt(i)
                    val y = yAt(v)
                    if (idx == 0) forecastPath.moveTo(x, y) else forecastPath.lineTo(x, y)
                }
                drawPath(
                    forecastPath,
                    brush = chartForecastBrush,
                    style = Stroke(
                        width = 3.5f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )
                )
            } else if (forecastPts.size == 1) {
                val (i, v) = forecastPts.first()
                drawCircle(
                    color = chartForecast,
                    radius = 5f,
                    center = Offset(xAt(i), yAt(v))
                )
            }
        }
    }
}
