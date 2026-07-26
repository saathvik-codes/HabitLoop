package com.habitloop.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.habitloop.app.R

@Composable
fun LaunchScreen() {
    val motion = rememberInfiniteTransition(label = "launch")
    val logoScale by motion.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo-breath"
    )
    val loopProgress by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1450, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "draw-loop"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF8F4)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "HabitLoop",
            modifier = Modifier
                .size(208.dp)
                .scale(logoScale)
        )

        Text(
            text = "HabitLoop",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF2F2F2F),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = "Small steps, kept gently.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF767676),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        HandDrawnLoop(
            progress = loopProgress,
            modifier = Modifier.size(width = 112.dp, height = 28.dp)
        )
    }
}

@Composable
private fun HandDrawnLoop(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 3.dp.toPx()
        val inset = stroke
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val start = -12f

        // A faint pencil-like guide keeps the loader tactile rather than mechanical.
        drawArc(
            color = Color(0xFFE7DFD3),
            startAngle = start,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFFFF6B3D),
            startAngle = start,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0x55FF9B69),
            startAngle = start + 3f,
            sweepAngle = 355f * progress,
            useCenter = false,
            topLeft = Offset(inset + 1.2f, inset - 0.8f),
            size = arcSize,
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
