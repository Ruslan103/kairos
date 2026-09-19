package com.example.kaizenkanban.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.kaizenkanban.ui.i18n.LocalAppStrings

@Composable
fun PlanningGuideDialog(
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val sections = listOf(
        null to s.planningGuideIntro,
        s.planningGuideHubsTitle to s.planningGuideHubsBody,
        s.planningGuideDayTitle to s.planningGuideDayBody,
        s.planningGuideRhythmTitle to s.planningGuideRhythmBody,
        s.planningGuideOwnTitle to s.planningGuideOwnBody,
        s.planningGuideStartTitle to s.planningGuideStartBody
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = s.planningGuideTitle,
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
                    if (index > 0) Spacer(modifier = Modifier.height(14.dp))
                    if (heading != null) {
                        Text(
                            text = heading,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
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
