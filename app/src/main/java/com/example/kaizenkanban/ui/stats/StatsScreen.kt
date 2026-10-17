package com.example.kaizenkanban.ui.stats

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.domain.model.Column as HubColumn
import com.example.kaizenkanban.domain.model.RecurringTemplate
import com.example.kaizenkanban.domain.model.Task
import com.example.kaizenkanban.domain.model.TaskLink
import com.example.kaizenkanban.domain.stats.CompletionOutlook
import com.example.kaizenkanban.domain.stats.DailyActivityPoint
import com.example.kaizenkanban.domain.stats.ProgressPoint
import com.example.kaizenkanban.domain.stats.ProjectStatsCalculator
import com.example.kaizenkanban.domain.stats.ProjectStatsSnapshot
import com.example.kaizenkanban.domain.stats.StatsPeriod
import com.example.kaizenkanban.ui.board.TaskLinksDialog
import com.example.kaizenkanban.ui.components.DialogMarqueeText
import com.example.kaizenkanban.ui.components.MarqueeDropdownField
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.taskmeta.TaskCriteriaDialog
import com.example.kaizenkanban.ui.theme.buttonFill
import com.example.kaizenkanban.ui.theme.buttonLabel
import com.example.kaizenkanban.ui.theme.buttonLine
import com.example.kaizenkanban.ui.theme.chartForecast
import com.example.kaizenkanban.ui.theme.chartProgress
import com.example.kaizenkanban.ui.theme.chartToward
import com.example.kaizenkanban.ui.theme.chartTrivia
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) epochTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val epochMillis = remember(epochTick) { prefs.statsEpochMillis }
    val project = state.projects.find { it.id == projectId }
    var period by rememberSaveable { mutableStateOf(StatsPeriod.NOW.name) }
    val selectedPeriod = remember(period) {
        runCatching {
            when (period) {
                "DAY" -> StatsPeriod.TODAY
                else -> StatsPeriod.valueOf(period)
            }
        }.getOrDefault(StatsPeriod.WEEK)
    }
    var selectedGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var legacyPinsMigrated by rememberSaveable(projectId) { mutableStateOf(false) }

    val projectBoardIds = remember(state.boards, projectId) {
        state.boards.filter { it.projectId == projectId }.map { it.id }.toSet()
    }
    val projectColumns = remember(state.columns, projectBoardIds) {
        state.columns.filter { it.boardId in projectBoardIds }.sortedBy { it.position }
    }
    val projectColumnIds = remember(projectColumns) { projectColumns.map { it.id }.toSet() }
    val projectTasks = remember(state.tasks, projectColumnIds) {
        state.tasks.filter { it.columnId in projectColumnIds && !it.isHidden }
    }
    val journal = remember(state.statsJournal, projectId) {
        state.statsJournal.filter { it.projectId == projectId }
    }
    val templates = remember(state.recurringTemplates, projectId) {
        state.recurringTemplates.filter { it.projectId == projectId }
    }
    val projectTaskIds = remember(projectTasks) { projectTasks.map { it.id }.toSet() }
    val projectLinks = remember(state.taskLinks, projectTaskIds) {
        state.taskLinks.filter { it.parentId in projectTaskIds && it.childId in projectTaskIds }
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

    val snapshot by produceState<ProjectStatsSnapshot?>(
        initialValue = null,
        projectTasks,
        projectLinks,
        templates,
        selectedPeriod,
        selectedGoalId,
        epochMillis,
        journal
    ) {
        value = null
        value = withContext(Dispatchers.Default) {
            ProjectStatsCalculator.compute(
                projectTasks = projectTasks,
                links = projectLinks,
                templates = templates,
                period = selectedPeriod,
                epochMillis = epochMillis,
                journal = journal,
                selectedGoalId = selectedGoalId
            )
        }
    }
    LaunchedEffect(snapshot?.selectedGoal?.goalId) {
        val id = snapshot?.selectedGoal?.goalId
        if (id != null && selectedGoalId == null) selectedGoalId = id
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
                }
            )
        }
    ) { padding ->
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
                0 -> {
                    val snap = snapshot
                    if (snap == null) {
                        StatsComputingPlaceholder()
                    } else {
                        GoalsTab(
                            snapshot = snap,
                            selectedPeriod = selectedPeriod,
                            onPeriodChange = { period = it.name },
                            selectedGoalId = selectedGoalId,
                            onSelectGoal = { selectedGoalId = it },
                            onResetGoalEpoch = { viewModel.resetGoalStatsEpoch(it) }
                        )
                    }
                }
                1 -> {
                    val snap = snapshot
                    if (snap == null) {
                        StatsComputingPlaceholder()
                    } else {
                        PeriodTab(
                            snapshot = snap,
                            selectedPeriod = selectedPeriod,
                            onPeriodChange = { period = it.name }
                        )
                    }
                }
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
private fun StatsComputingPlaceholder() {
    val s = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = s.statsLoading,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsViewModeControl(
    selectedPeriod: StatsPeriod,
    onPeriodChange: (StatsPeriod) -> Unit
) {
    val s = LocalAppStrings.current
    val overall = selectedPeriod == StatsPeriod.NOW
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatsModeSegment(
                label = s.statsViewModeOverall,
                selected = overall,
                modifier = Modifier.weight(1f),
                onClick = { onPeriodChange(StatsPeriod.NOW) }
            )
            StatsModeSegment(
                label = s.statsViewModeDynamics,
                selected = !overall,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (selectedPeriod == StatsPeriod.NOW) {
                        onPeriodChange(StatsPeriod.WEEK)
                    }
                }
            )
        }
        if (!overall) {
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
}

@Composable
private fun StatsModeSegment(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(buttonFill()).border(1.dp, buttonLine(), shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                buttonLabel()
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1
        )
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
    var showMetricDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (snapshot.goals.isNotEmpty()) {
            val selectedLabel = goal?.let { g ->
                val archive = if (g.isArchived) " (${s.statsGoalArchivedBadge})" else ""
                "${g.goalTitle}$archive"
            } ?: s.statsGoalLabel

            ExposedDropdownMenuBox(
                expanded = goalMenuExpanded,
                onExpandedChange = { goalMenuExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                MarqueeDropdownField(
                    value = selectedLabel,
                    label = s.statsGoalLabel,
                    expanded = goalMenuExpanded,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = goalMenuExpanded,
                    onDismissRequest = { goalMenuExpanded = false }
                ) {
                    snapshot.goals.forEach { g ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (g.isArchived) {
                                        "${g.goalTitle} (${s.statsGoalArchivedBadge})"
                                    } else {
                                        g.goalTitle
                                    },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
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

        StatsViewModeControl(
            selectedPeriod = selectedPeriod,
            onPeriodChange = onPeriodChange
        )

        if (!fromStart || goal == null) ReviewSectionCard(
            title = if (fromStart) s.statsFromStartSection else s.statsHowPeriodWent(snapshot.period)
        ) {
            if (goal == null) {
                Text(s.statsNoGoals, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val closurePct = ProjectStatsCalculator.percent(goal.periodDelta)
                Text(
                    s.statsGoalClosureLine(s.statsPeriodDelta(closurePct, snapshot.period)),
                    color = deltaColor(closurePct)
                )
                snapshot.chancePeriodDelta?.let { chanceDelta ->
                    val chancePct = ProjectStatsCalculator.percent(chanceDelta)
                    Text(
                        s.statsChanceLine(s.statsPeriodDelta(chancePct, snapshot.period)),
                        color = deltaColor(chancePct)
                    )
                }
                Text(s.statsDoneTotal(snapshot.selectedGoalDoneCount))
                Text(s.statsNotDoneTotal(snapshot.selectedGoalNotDoneCount))
                Text(
                    s.statsRecurringOfDone(snapshot.selectedGoalRecurringDoneCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    s.statsRecurringOfNotDone(snapshot.selectedGoalRecurringNotDoneCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (fromStart && goal != null && snapshot.hatP != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EfficiencyRing(
                    title = s.statsForecastTitle,
                    progress = snapshot.hatP,
                    accent = chartProgress()
                )
                val chanceTitle = snapshot.selectedGoalDeadlineMillis?.let { at ->
                    val formatted = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                        .format(Date(at))
                    s.statsChanceTitleUntil(formatted)
                } ?: s.statsChanceTitle
                MetricCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = chanceTitle,
                    percent = ProjectStatsCalculator.percent(snapshot.chanceToComplete ?: 0f),
                    progress = snapshot.chanceToComplete ?: 0f,
                    accent = chartForecast(),
                    caption = likelyFinishCaption(
                        progress = goal.progress,
                        series = snapshot.goalProgressSeries,
                        chance = snapshot.chanceToComplete,
                        deadlineMillis = snapshot.selectedGoalDeadlineMillis
                    )
                )
            }
            TextButton(
                onClick = { showMetricDetails = !showMetricDetails },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (showMetricDetails) s.hide else s.statsDetails,
                    maxLines = 1
                )
            }
            if (showMetricDetails) {
                Text(s.statsDoneTotal(snapshot.selectedGoalDoneCount))
                Text(s.statsNotDoneTotal(snapshot.selectedGoalNotDoneCount))
                Text(
                    s.statsRecurringOfDone(snapshot.selectedGoalRecurringDoneCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    s.statsRecurringOfNotDone(snapshot.selectedGoalRecurringNotDoneCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    s.statsTaskTotal(
                        snapshot.selectedGoalDoneCount + snapshot.selectedGoalNotDoneCount
                    )
                )
            }
        }

        if (fromStart && snapshot.goalProgressSeries.isNotEmpty()) {
            Text(s.statsForecastTitle, fontWeight = FontWeight.SemiBold)
            EfficiencyLineChart(snapshot.goalProgressSeries)
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
            title = { DialogMarqueeText(text = s.statsResetGoalEpoch, fontWeight = FontWeight.Bold, maxLines = 2) },
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
private fun deltaColor(pp: Int) = when {
    pp > 0 -> MaterialTheme.colorScheme.primary
    pp < 0 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun ReviewSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
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
        shape = RoundedCornerShape(10.dp),
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
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
private fun likelyFinishCaption(
    progress: Float,
    series: List<com.example.kaizenkanban.domain.stats.ProgressPoint>,
    chance: Float?,
    deadlineMillis: Long?
): String {
    val s = LocalAppStrings.current
    val outlook = ProjectStatsCalculator.completionOutlook(
        progress = progress,
        series = series,
        now = System.currentTimeMillis(),
        chance = chance,
        deadlineMillis = deadlineMillis
    )
    return when (outlook) {
        CompletionOutlook.Reached -> s.statsLikelyDateReached
        CompletionOutlook.NoPace -> s.statsLikelyDateNoPace
        is CompletionOutlook.OnDate -> {
            val formatted = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                .format(Date(outlook.dayMillis))
            s.statsLikelyDate(formatted)
        }
    }
}

@Composable
private fun EfficiencyRing(
    title: String,
    progress: Float,
    accent: androidx.compose.ui.graphics.Color
) {
    val s = LocalAppStrings.current
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val shown = progress.coerceIn(-1f, 1f)
    val negative = shown < 0f
    val ink = if (negative) chartForecast() else accent
    val score = String.format(Locale.getDefault(), "%.1f", shown * 10f)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier.size(168.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                val stroke = 12.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                drawArc(
                    color = track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                val sweep = 360f * shown.absoluteValue
                if (sweep > 0f) {
                    drawArc(
                        color = ink,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    score,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ink
                )
                Text(
                    s.statsScoreOutOfTen,
                    style = MaterialTheme.typography.labelMedium,
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
        StatsViewModeControl(
            selectedPeriod = selectedPeriod,
            onPeriodChange = onPeriodChange
        )

        ReviewSectionCard(
            title = if (fromStart) s.statsFromStartSection else s.statsHowPeriodWent(snapshot.period)
        ) {
            if (fromStart) {
                Text(s.statsDoneTotal(snapshot.doneInPeriod))
                Text(s.statsNotDoneTotal(snapshot.notDoneCount))
                Text(s.statsTrivia(snapshot.triviaCount))
                Text(
                    s.statsRecurringOfDone(snapshot.recurringFact),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    s.statsRecurringOfNotDone(snapshot.recurringNotDoneCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    s.statsTaskTotal(
                        snapshot.doneInPeriod + snapshot.notDoneCount + snapshot.openTaskCount
                    )
                )
            } else {
                Text(s.statsDoneTotal(snapshot.doneInPeriod))
                Text(s.statsNotDoneTotal(snapshot.notDoneCount))
                Text(s.statsTrivia(snapshot.triviaCount))
                Text(
                    s.statsRecurringOfDone(snapshot.recurringFact),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    s.statsRecurringOfNotDone(snapshot.recurringNotDoneCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(s.statsTaskTotal(snapshot.doneInPeriod + snapshot.notDoneCount))
            }
        }

        if (fromStart && snapshot.dailyActivity.isNotEmpty()) {
            Text(s.statsChartActivity, fontWeight = FontWeight.SemiBold)
            ActivityBarChart(snapshot.dailyActivity)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(chartToward(), s.statsChartToward)
                LegendDot(chartTrivia(), s.statsChartTrivia)
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

private const val ORPHAN_HUB_KEY = "archive-orphan-hub"

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
    var archiveSearch by rememberSaveable { mutableStateOf("") }
    val archived = remember(projectTasks) { projectTasks.filter { it.isBoardArchived } }
    val done = remember(archived) { archived.filter { it.isCompleted } }
    val notDone = remember(archived) { archived.filter { it.isNotDone } }
    val listBase = if (segment == 0) done else notDone
    val list = remember(listBase, archiveSearch) {
        val q = archiveSearch.trim()
        if (q.isEmpty()) listBase
        else listBase.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.description.contains(q, ignoreCase = true)
        }
    }
    var linksTask by remember { mutableStateOf<Task?>(null) }
    var settingsTask by remember { mutableStateOf<Task?>(null) }
    var expandedHubs by remember { mutableStateOf(emptySet<String>()) }
    var confirmClear by remember { mutableStateOf(false) }
    val clearProgress by viewModel.clearArchiveProgress.collectAsState()
    val isClearing = clearProgress != null

    LaunchedEffect(isClearing) {
        if (!isClearing) confirmClear = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = segment == 0,
                onClick = { segment = 0 },
                label = { Text("${s.statsArchiveDone} (${done.size})") },
                enabled = !isClearing
            )
            FilterChip(
                selected = segment == 1,
                onClick = { segment = 1 },
                label = { Text("${s.statsArchiveNotDone} (${notDone.size})") },
                enabled = !isClearing
            )
        }
        OutlinedTextField(
            value = archiveSearch,
            onValueChange = { archiveSearch = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            placeholder = { Text(s.searchArchive) },
            enabled = !isClearing
        )
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
                val byHub = remember(list) {
                    list.groupBy { it.columnId }.mapValues { (_, tasks) ->
                        tasks.sortedByDescending { it.completedAt ?: Long.MIN_VALUE }
                    }
                }
                projectColumns.asReversed().forEach { col ->
                    val hubTasks = byHub[col.id].orEmpty()
                    if (hubTasks.isEmpty()) return@forEach
                    ArchiveHubSection(
                        title = s.localized(col.title),
                        tasks = hubTasks,
                        expanded = col.id in expandedHubs,
                        onToggle = {
                            expandedHubs = if (col.id in expandedHubs) {
                                expandedHubs - col.id
                            } else {
                                expandedHubs + col.id
                            }
                        },
                        projectId = projectId,
                        viewModel = viewModel,
                        onOpenSettings = { settingsTask = it },
                        onOpenLinks = { linksTask = it }
                    )
                }
                val orphan = byHub
                    .filterKeys { it !in columnsById }
                    .values
                    .flatten()
                    .sortedByDescending { it.completedAt ?: Long.MIN_VALUE }
                if (orphan.isNotEmpty()) {
                    ArchiveHubSection(
                        title = s.unknownHub,
                        tasks = orphan,
                        expanded = ORPHAN_HUB_KEY in expandedHubs,
                        onToggle = {
                            expandedHubs = if (ORPHAN_HUB_KEY in expandedHubs) {
                                expandedHubs - ORPHAN_HUB_KEY
                            } else {
                                expandedHubs + ORPHAN_HUB_KEY
                            }
                        },
                        projectId = projectId,
                        viewModel = viewModel,
                        onOpenSettings = { settingsTask = it },
                        onOpenLinks = { linksTask = it }
                    )
                }
            }
            if (archived.isNotEmpty() || isClearing) {
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = { confirmClear = true },
                    enabled = !isClearing && archived.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isClearing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                s.statsClearingArchive,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Text(s.statsClearArchive, color = MaterialTheme.colorScheme.error)
                    }
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

    if (confirmClear || isClearing) {
        val progress = clearProgress
        AlertDialog(
            onDismissRequest = {
                if (!isClearing) confirmClear = false
            },
            title = {
                DialogMarqueeText(
                    text = if (isClearing) s.statsClearingArchive else s.statsClearArchive,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            },
            text = {
                if (isClearing && progress != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = s.statsClearingArchiveProgress(
                                progress.processed,
                                progress.total
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(
                            progress = {
                                progress.processed.toFloat() /
                                    progress.total.coerceAtLeast(1).toFloat()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(s.statsClearArchiveConfirm(archived.size))
                }
            },
            confirmButton = {
                if (!isClearing) {
                    TextButton(
                        onClick = { viewModel.clearArchiveToJournal(projectId) }
                    ) {
                        Text(s.clear, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                if (!isClearing) {
                    TextButton(onClick = { confirmClear = false }) {
                        Text(s.cancel)
                    }
                }
            }
        )
    }
}

@Composable
private fun ArchiveHubSection(
    title: String,
    tasks: List<Task>,
    expanded: Boolean,
    onToggle: () -> Unit,
    projectId: String,
    viewModel: SharedViewModel,
    onOpenSettings: (Task) -> Unit,
    onOpenLinks: (Task) -> Unit
) {
    val s = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggle)
                .padding(top = 8.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) s.hide else s.show,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                tasks.size.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (expanded) {
            tasks.forEach { task ->
                ArchiveTaskRow(
                    task = task,
                    projectId = projectId,
                    viewModel = viewModel,
                    onOpenSettings = { onOpenSettings(task) },
                    onOpenLinks = { onOpenLinks(task) }
                )
            }
        }
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

    val closedAt = task.completedAt
    val closedLabel = remember(closedAt) {
        closedAt?.let { SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(it)) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
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
                        color = chartProgress(),
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
                        text = { Text(if (task.isGoal) s.goalSteps else s.linkToGoal) },
                        onClick = {
                            menuOpen = false
                            onOpenLinks()
                        }
                    )
                    if (!viewModel.isGoalByHub(task.columnId)) {
                    DropdownMenuItem(
                        text = {
                            Text(if (task.isGoal) s.unmarkAsGoal else s.markAsGoal)
                        },
                        onClick = {
                            menuOpen = false
                            viewModel.updateTask(task.copy(isGoal = !task.isGoal))
                        }
                    )
                    }
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
        if (closedLabel != null) {
            Text(
                closedLabel,
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        val towardInk = chartToward()
        val triviaInk = chartTrivia()
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
                    color = towardInk,
                    topLeft = Offset(x, size.height - towardH),
                    size = Size(barW, towardH)
                )
                drawRect(
                    color = triviaInk,
                    topLeft = Offset(x, size.height - h),
                    size = Size(barW, triviaH)
                )
            }
        }
    }
}

@Composable
private fun EfficiencyLineChart(points: List<ProgressPoint>) {
    val guide = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val aboveInk = chartProgress()
    val belowInk = MaterialTheme.colorScheme.error
    Box {
        listOf(1f to "10", 0.5f to "5", 0f to "0", -0.5f to "-5", -1f to "-10").forEach { (value, label) ->
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                modifier = Modifier.align(BiasAlignment(-1f, -value))
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
        ) {
            val series = points.mapNotNull { it.forecast }
            if (series.isEmpty()) return@Canvas
            fun yAt(value: Float): Float {
                val v = value.coerceIn(-1f, 1f)
                return size.height * (1f - v) / 2f
            }
            val splitY = yAt(0.5f)
            listOf(splitY, yAt(-0.5f)).forEach { y ->
                drawLine(
                    guide,
                    Offset(0f, y),
                    Offset(size.width, y),
                    strokeWidth = 1.5f
                )
            }
            fun xAt(i: Int): Float =
                if (series.size == 1) size.width / 2 else i * size.width / (series.size - 1)

            val stroke = Stroke(width = 5f, cap = StrokeCap.Round)
            if (series.size == 1) {
                val v = series.first()
                drawCircle(
                    color = if (v >= 0.5f) aboveInk else belowInk,
                    radius = 5f,
                    center = Offset(xAt(0), yAt(v))
                )
                return@Canvas
            }
            for (i in 1 until series.size) {
                val prev = series[i - 1]
                val next = series[i]
                val x0 = xAt(i - 1)
                val x1 = xAt(i)
                val y0 = yAt(prev)
                val y1 = yAt(next)
                val aboveStart = prev >= 0.5f
                val aboveEnd = next >= 0.5f
                if (aboveStart == aboveEnd || prev == next) {
                    drawLine(
                        color = if (aboveStart) aboveInk else belowInk,
                        start = Offset(x0, y0),
                        end = Offset(x1, y1),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round
                    )
                } else {
                    val t = ((0.5f - prev) / (next - prev)).coerceIn(0f, 1f)
                    val cross = Offset(x0 + (x1 - x0) * t, splitY)
                    drawLine(
                        color = if (aboveStart) aboveInk else belowInk,
                        start = Offset(x0, y0),
                        end = cross,
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = if (aboveEnd) aboveInk else belowInk,
                        start = cross,
                        end = Offset(x1, y1),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
