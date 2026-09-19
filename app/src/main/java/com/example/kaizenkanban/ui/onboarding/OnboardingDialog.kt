package com.example.kaizenkanban.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kaizenkanban.ui.i18n.LocalAppStrings

@Composable
fun OnboardingDialog(
    onFinished: () -> Unit
) {
    val s = LocalAppStrings.current
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        s.onboardingTitle1 to s.onboardingText1,
        s.onboardingTitle2 to s.onboardingText2,
        s.onboardingTitle3 to s.onboardingText3,
        s.onboardingTitle4 to s.onboardingText4,
        s.onboardingTitle5 to s.onboardingText5
    )
    val (title, body) = pages[page]
    val isLast = page == pages.lastIndex

    AlertDialog(
        onDismissRequest = onFinished,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pages.indices.forEach { index ->
                        Text(
                            text = if (index == page) "●" else "○",
                            color = if (index == page) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isLast) onFinished()
                    else page += 1
                }
            ) {
                Text(if (isLast) s.onboardingDone else s.onboardingNext)
            }
        },
        dismissButton = {
            if (!isLast) {
                TextButton(onClick = onFinished) {
                    Text(s.onboardingSkip)
                }
            }
        }
    )
}
