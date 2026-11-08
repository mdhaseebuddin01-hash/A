package com.auris.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.auris.app.AurisViewModel
import com.auris.app.CaseRecord
import com.auris.app.Catalogue
import com.auris.app.ModelState
import com.auris.app.Stats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun AccuracyScreen(vm: AurisViewModel) {
    val context = LocalContext.current
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            vm.exportCsv(uri) { ok ->
                Toast.makeText(context, if (ok) "CSV exported" else "CSV not exported", Toast.LENGTH_SHORT).show()
            }
        }
    }
    var confirmClear by remember { mutableStateOf(false) }
    val cases = vm.cases
    val summary = remember(cases) { Stats.summarise(cases) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Header()
        Text(
            "Accuracy",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        ModelCard(vm.model)

        Text(
            "In your clinic",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 28.dp)
        )
        if (summary.n == 0) {
            Text(
                "No verdicts yet. After each analysis, tap whether Auris was right. This score counts only cases a doctor has confirmed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    "${pct(summary.accuracy)}%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "matched the doctor in ${summary.k} of ${summary.n} ${if (summary.n == 1) "case" else "cases"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            RangeBar(summary)
            Text(
                "Likely true range: ${pct(summary.ciLow)}–${pct(summary.ciHigh)}%. " +
                    "Right diagnosis in the top 3: ${pct(summary.top3Rate)}%.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                statusText(summary),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp)
            )
            ByDiagnosis(cases)
            Recent(cases)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = { exportCsv.launch("auris-accuracy-log.csv") },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) { Text("Export CSV") }
                OutlinedButton(
                    onClick = { confirmClear = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) { Text("Clear log") }
            }
        }
        Text(
            "Verdicts are stored only on this phone. No images or patient details are saved.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 18.dp)
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear the accuracy log?") },
            text = { Text("This deletes all ${summary.n} saved verdicts from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearLog()
                    confirmClear = false
                }) { Text("Clear log") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }
}

private fun statusText(s: Stats.Summary): String {
    val remaining = 100 - s.n
    return when {
        remaining > 0 ->
            "$remaining more confirmed ${if (remaining == 1) "case" else "cases"} before this figure is dependable."
        pct(s.ciLow) >= 95 -> "The 95% target is met: the whole likely range is at or above 95%."
        pct(s.ciHigh) < 95 -> "Below the 95% target so far."
        else -> "Close to the target, but not yet proven at 95%."
    }
}

@Composable
private fun ModelCard(state: ModelState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Installed model",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            when (state) {
                ModelState.Loading -> Text("Loading…", modifier = Modifier.padding(top = 6.dp))
                is ModelState.Missing -> Text(
                    state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
                is ModelState.Ready -> {
                    val info = state.classifier.info
                    Text(
                        info.name + if (info.trainedOn.isNotBlank()) ", trained ${info.trainedOn}" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    if (info.testAccuracy != null) {
                        Text(
                            "Held-out test: ${pct(info.testAccuracy)}% correct on ${info.testImages} images " +
                                "(likely range ${pct(info.testCiLow ?: 0.0)}–${pct(info.testCiHigh ?: 0.0)}%).",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    if (info.confidentAccuracy != null && info.confidentCoverage != null) {
                        Text(
                            "When confident (probability ${pct(info.threshold.toDouble())}% or higher): " +
                                "${pct(info.confidentAccuracy)}% correct, on ${pct(info.confidentCoverage)}% of test images.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    Text(
                        if (info.targetMet) {
                            "Meets the 95% target in confident mode on its test set."
                        } else {
                            "Has not yet shown 95% on its test set."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (info.targetMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Covers: " + state.classifier.labels.joinToString { Catalogue.short(it) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/** Point estimate, likely range and the 95% target line on one 0–100% scale. */
@Composable
private fun RangeBar(s: Stats.Summary) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val band = MaterialTheme.colorScheme.primaryContainer
    val point = MaterialTheme.colorScheme.primary
    val targetLine = MaterialTheme.colorScheme.onSurface
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Box(Modifier.fillMaxWidth(0.95f)) {
            Text(
                "95% target",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            val w = size.width
            val cy = size.height / 2
            drawRoundRect(
                color = track,
                topLeft = Offset(0f, cy - 2.dp.toPx()),
                size = Size(w, 4.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
            val x0 = (s.ciLow * w).toFloat()
            val x1 = (s.ciHigh * w).toFloat()
            drawRoundRect(
                color = band,
                topLeft = Offset(x0, cy - 6.dp.toPx()),
                size = Size(max(x1 - x0, 6.dp.toPx()), 12.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
            val tx = 0.95f * w
            drawLine(targetLine, Offset(tx, 0f), Offset(tx, size.height), strokeWidth = 2.dp.toPx())
            drawCircle(point, radius = 8.dp.toPx(), center = Offset((s.accuracy * w).toFloat(), cy))
        }
    }
}

@Composable
private fun ByDiagnosis(cases: List<CaseRecord>) {
    val groups = remember(cases) {
        cases.groupBy { it.actual }
            .map { (id, list) -> Triple(id, list.size, list.count { it.ok }) }
            .sortedByDescending { it.second }
    }
    Column(Modifier.padding(top = 24.dp)) {
        Text(
            "By confirmed diagnosis",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        groups.forEach { (id, n, k) ->
            Column(Modifier.padding(top = 10.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        Catalogue.name(id),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "$k of $n",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { if (n == 0) 0f else k.toFloat() / n },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Recent(cases: List<CaseRecord>) {
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    Column(Modifier.padding(top = 24.dp)) {
        Text(
            "Recent verdicts",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        cases.take(10).forEach { c ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    dateFormat.format(Date(c.ts)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(56.dp)
                )
                Text(
                    if (c.ok) {
                        "Auris said ${Catalogue.short(c.ai)}"
                    } else {
                        "Auris said ${Catalogue.short(c.ai)}, doctor said ${Catalogue.short(c.actual)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (c.ok) "Match" else "Miss",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (c.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
