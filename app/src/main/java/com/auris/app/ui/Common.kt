package com.auris.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** The warm colour of otoscope light, used for the speculum rim. */
internal val Reflex = Color(0xFFF2CF7A)

internal fun pct(v: Double): Int = (v * 100).roundToInt()

@Composable
internal fun Header() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(38.dp)) {
            drawCircle(Color.Black)
            drawEardrum(outline = false)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "Auris",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Otoscopy assistant for ENT doctors",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A stylised right eardrum: pearly drum, malleus handle and the cone of light at five o’clock. */
internal fun DrawScope.drawEardrum(outline: Boolean = true) {
    val w = size.width
    val h = size.height
    val drumTopLeft = Offset(w * 0.18f, h * 0.15f)
    val drumSize = Size(w * 0.64f, h * 0.72f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF4A504D), Color(0xFF2A2F2D), Color(0xFF141716)),
            center = Offset(w * 0.48f, h * 0.44f),
            radius = w * 0.6f
        ),
        topLeft = drumTopLeft,
        size = drumSize
    )
    if (outline) {
        drawOval(
            color = Color.White.copy(alpha = 0.12f),
            topLeft = drumTopLeft,
            size = drumSize,
            style = Stroke(width = w * 0.0075f)
        )
    }
    drawLine(
        color = Color(0xFFEBEEEC).copy(alpha = 0.42f),
        start = Offset(w * 0.55f, h * 0.275f),
        end = Offset(w * 0.505f, h * 0.52f),
        strokeWidth = w * 0.03f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = Color(0xFFEBEEEC).copy(alpha = 0.5f),
        radius = w * 0.025f,
        center = Offset(w * 0.555f, h * 0.26f)
    )
    val cone = Path().apply {
        moveTo(w * 0.52f, h * 0.555f)
        lineTo(w * 0.71f, h * 0.73f)
        lineTo(w * 0.64f, h * 0.79f)
        close()
    }
    drawPath(
        path = cone,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xF2F7DE9C), Color(0x1FF2CF7A)),
            start = Offset(w * 0.52f, h * 0.555f),
            end = Offset(w * 0.70f, h * 0.78f)
        )
    )
}

internal enum class Tone { Warning, Error }

@Composable
internal fun Banner(text: String, tone: Tone) {
    val container = when (tone) {
        Tone.Warning -> MaterialTheme.colorScheme.tertiaryContainer
        Tone.Error -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (tone) {
        Tone.Warning -> MaterialTheme.colorScheme.onTertiaryContainer
        Tone.Error -> MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}
