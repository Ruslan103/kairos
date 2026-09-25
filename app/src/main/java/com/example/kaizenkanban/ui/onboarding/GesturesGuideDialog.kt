package com.example.kaizenkanban.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.ui.components.DialogSectionDivider
import com.example.kaizenkanban.ui.i18n.LocalAppStrings

@Composable
fun GesturesGuideDialog(
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val sections = listOf(
        s.gesturesGuideOpenProjectsTitle to s.gesturesGuideOpenProjectsBody,
        s.gesturesGuideSwitchListsTitle to s.gesturesGuideSwitchListsBody,
        s.gesturesGuideShowMenuTitle to s.gesturesGuideShowMenuBody,
        s.gesturesGuideMoveTaskTitle to s.gesturesGuideMoveTaskBody,
        s.gesturesGuideEditTaskTitle to s.gesturesGuideEditTaskBody,
        s.gesturesGuideLongTitleTitle to s.gesturesGuideLongTitleBody,
        s.gesturesGuideMoreActionsTitle to s.gesturesGuideMoreActionsBody,
        s.gesturesGuideInProgressTitle to s.gesturesGuideInProgressBody,
        s.gesturesGuideDueDateTitle to s.gesturesGuideDueDateBody
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = s.gesturesGuideTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                sections.forEachIndexed { index, (heading, body) ->
                    if (index > 0) DialogSectionDivider()
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(s.planningGuideGotIt)
            }
        }
    )
}
