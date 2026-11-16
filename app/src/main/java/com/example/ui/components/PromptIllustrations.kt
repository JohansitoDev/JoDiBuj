package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BeeYellow
import com.example.ui.theme.BorderGray
import com.example.ui.theme.CardinalRed
import com.example.ui.theme.FeatherGreen
import com.example.ui.theme.FoxOrange
import com.example.ui.theme.LogoPink
import com.example.ui.theme.LogoPurple
import com.example.ui.theme.MacawBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PromptIllustrationBadge(
    promptKey: String,
    size: Dp = 76.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .background(PureWhite)
            .border(2.dp, BorderGray, RoundedCornerShape(18.dp))
            .padding(8.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (promptKey) {
                "fish" -> drawFish()
                "dog" -> drawDog()
                "cat" -> drawCat()
                "horse" -> drawHorse()
                "pencil" -> drawPencil()
                "house" -> drawHouse()
                "tree" -> drawTree()
                "sun" -> drawSun()
                "star" -> drawStar()
                "rocket" -> drawRocket()
                "butterfly" -> drawButterfly()
                "apple" -> drawApple()
                else -> drawFish()
            }
        }
    }
}

private fun DrawScope.drawFish() {
    val w = size.width
    val h = size.height
    // Tail fin
    val tail = Path().apply {
        moveTo(w * 0.72f, h * 0.5f)
        lineTo(w * 0.94f, h * 0.28f)
        lineTo(w * 0.94f, h * 0.72f)
        close()
    }
    drawPath(tail, color = FoxOrange)
    // Body
    drawOval(
        color = MacawBlue,
        topLeft = Offset(w * 0.1f, h * 0.25f),
        size = Size(w * 0.66f, h * 0.5f),
    )
    // Eye
    drawCircle(PureWhite, radius = w * 0.08f, center = Offset(w * 0.28f, h * 0.44f))
    drawCircle(TextPrimary, radius = w * 0.04f, center = Offset(w * 0.27f, h * 0.44f))
    // Bubbles
    drawCircle(MacawBlue.copy(alpha = 0.4f), radius = w * 0.04f, center = Offset(w * 0.08f, h * 0.26f))
    drawCircle(MacawBlue.copy(alpha = 0.4f), radius = w * 0.03f, center = Offset(w * 0.14f, h * 0.14f))
}

private fun DrawScope.drawDog() {
    val w = size.width
    val h = size.height
    // Ears
    drawOval(FoxOrange.copy(alpha = 0.85f), Offset(w * 0.12f, h * 0.22f), Size(w * 0.2f, h * 0.4f))
    drawOval(FoxOrange.copy(alpha = 0.85f), Offset(w * 0.68f, h * 0.22f), Size(w * 0.2f, h * 0.4f))
    // Head
    drawCircle(BeeYellow, radius = w * 0.3f, center = Offset(w * 0.5f, h * 0.52f))
    // Snout
    drawOval(PureWhite, Offset(w * 0.34f, h * 0.54f), Size(w * 0.32f, h * 0.24f))
    // Eyes & Nose
    drawCircle(TextPrimary, radius = w * 0.05f, center = Offset(w * 0.38f, h * 0.44f))
    drawCircle(TextPrimary, radius = w * 0.05f, center = Offset(w * 0.62f, h * 0.44f))
    drawOval(TextPrimary, Offset(w * 0.44f, h * 0.56f), Size(w * 0.12f, h * 0.09f))
}

private fun DrawScope.drawCat() {
    val w = size.width
    val h = size.height
    // Ears
    val leftEar = Path().apply {
        moveTo(w * 0.2f, h * 0.45f)
        lineTo(w * 0.25f, h * 0.14f)
        lineTo(w * 0.42f, h * 0.34f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(w * 0.8f, h * 0.45f)
        lineTo(w * 0.75f, h * 0.14f)
        lineTo(w * 0.58f, h * 0.34f)
        close()
    }
    drawPath(leftEar, color = LogoPurple)
    drawPath(rightEar, color = LogoPurple)
    drawCircle(LogoPurple.copy(alpha = 0.85f), radius = w * 0.3f, center = Offset(w * 0.5f, h * 0.56f))
    drawCircle(PureWhite, radius = w * 0.055f, center = Offset(w * 0.38f, h * 0.5f))
    drawCircle(PureWhite, radius = w * 0.055f, center = Offset(w * 0.62f, h * 0.5f))
    drawCircle(LogoPink, radius = w * 0.045f, center = Offset(w * 0.5f, h * 0.6f))
    // Whiskers
    drawLine(PureWhite, Offset(w * 0.12f, h * 0.58f), Offset(w * 0.34f, h * 0.6f), strokeWidth = 3f)
    drawLine(PureWhite, Offset(w * 0.88f, h * 0.58f), Offset(w * 0.66f, h * 0.6f), strokeWidth = 3f)
}

private fun DrawScope.drawHorse() {
    val w = size.width
    val h = size.height
    val neck = Path().apply {
        moveTo(w * 0.24f, h * 0.88f)
        lineTo(w * 0.32f, h * 0.32f)
        lineTo(w * 0.52f, h * 0.18f)
        lineTo(w * 0.84f, h * 0.42f)
        lineTo(w * 0.72f, h * 0.56f)
        lineTo(w * 0.54f, h * 0.52f)
        lineTo(w * 0.58f, h * 0.88f)
        close()
    }
    drawPath(neck, color = FoxOrange)
    // Ear & Mane
    val ear = Path().apply {
        moveTo(w * 0.42f, h * 0.24f)
        lineTo(w * 0.48f, h * 0.08f)
        lineTo(w * 0.56f, h * 0.22f)
        close()
    }
    drawPath(ear, color = FoxOrange)
    drawCircle(TextPrimary, radius = w * 0.045f, center = Offset(w * 0.58f, h * 0.34f))
}

private fun DrawScope.drawPencil() {
    val w = size.width
    val h = size.height
    // Yellow body
    drawRoundRect(
        color = BeeYellow,
        topLeft = Offset(w * 0.28f, h * 0.22f),
        size = Size(w * 0.44f, h * 0.56f),
        cornerRadius = CornerRadius(6f, 6f),
    )
    // Eraser
    drawRoundRect(
        color = LogoPink,
        topLeft = Offset(w * 0.28f, h * 0.1f),
        size = Size(w * 0.44f, h * 0.14f),
        cornerRadius = CornerRadius(8f, 8f),
    )
    // Tip
    val tip = Path().apply {
        moveTo(w * 0.28f, h * 0.78f)
        lineTo(w * 0.72f, h * 0.78f)
        lineTo(w * 0.5f, h * 0.94f)
        close()
    }
    drawPath(tip, color = FoxOrange.copy(alpha = 0.5f))
    val lead = Path().apply {
        moveTo(w * 0.43f, h * 0.88f)
        lineTo(w * 0.57f, h * 0.88f)
        lineTo(w * 0.5f, h * 0.94f)
        close()
    }
    drawPath(lead, color = TextPrimary)
}

private fun DrawScope.drawHouse() {
    val w = size.width
    val h = size.height
    val roof = Path().apply {
        moveTo(w * 0.1f, h * 0.46f)
        lineTo(w * 0.5f, h * 0.14f)
        lineTo(w * 0.9f, h * 0.46f)
        close()
    }
    drawPath(roof, color = CardinalRed)
    drawRect(MacawBlue.copy(alpha = 0.2f), Offset(w * 0.2f, h * 0.46f), Size(w * 0.6f, h * 0.42f))
    drawRoundRect(FoxOrange, Offset(w * 0.42f, h * 0.6f), Size(w * 0.16f, h * 0.28f), CornerRadius(4f, 4f))
}

private fun DrawScope.drawTree() {
    val w = size.width
    val h = size.height
    drawRoundRect(FoxOrange, Offset(w * 0.42f, h * 0.58f), Size(w * 0.16f, h * 0.34f), CornerRadius(4f, 4f))
    drawCircle(FeatherGreen, radius = w * 0.28f, center = Offset(w * 0.5f, h * 0.38f))
    drawCircle(FeatherGreen, radius = w * 0.18f, center = Offset(w * 0.34f, h * 0.46f))
    drawCircle(FeatherGreen, radius = w * 0.18f, center = Offset(w * 0.66f, h * 0.46f))
}

private fun DrawScope.drawSun() {
    val w = size.width
    val h = size.height
    val c = Offset(w * 0.5f, h * 0.5f)
    drawCircle(BeeYellow, radius = w * 0.24f, center = c)
    for (i in 0 until 8) {
        val angle = i * (PI / 4.0)
        val start = Offset((c.x + cos(angle) * w * 0.3f).toFloat(), (c.y + sin(angle) * w * 0.3f).toFloat())
        val end = Offset((c.x + cos(angle) * w * 0.42f).toFloat(), (c.y + sin(angle) * w * 0.42f).toFloat())
        drawLine(FoxOrange, start, end, strokeWidth = 6f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawStar() {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.52f
    val outer = w * 0.42f
    val inner = w * 0.18f
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val angle = -PI / 2.0 + i * (PI / 5.0)
        val x = (cx + cos(angle) * r).toFloat()
        val y = (cy + sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = BeeYellow)
    drawPath(path, color = FoxOrange, style = Stroke(width = 3f, join = StrokeJoin.Round))
}

private fun DrawScope.drawRocket() {
    val w = size.width
    val h = size.height
    val body = Path().apply {
        moveTo(w * 0.5f, h * 0.1f)
        quadraticTo(w * 0.72f, h * 0.38f, w * 0.65f, h * 0.72f)
        lineTo(w * 0.35f, h * 0.72f)
        quadraticTo(w * 0.28f, h * 0.38f, w * 0.5f, h * 0.1f)
        close()
    }
    drawPath(body, color = MacawBlue)
    drawCircle(PureWhite, radius = w * 0.1f, center = Offset(w * 0.5f, h * 0.44f))
    val flame = Path().apply {
        moveTo(w * 0.4f, h * 0.74f)
        lineTo(w * 0.5f, h * 0.94f)
        lineTo(w * 0.6f, h * 0.74f)
        close()
    }
    drawPath(flame, color = FoxOrange)
}

private fun DrawScope.drawButterfly() {
    val w = size.width
    val h = size.height
    drawCircle(LogoPink, radius = w * 0.19f, center = Offset(w * 0.3f, h * 0.38f))
    drawCircle(LogoPink, radius = w * 0.19f, center = Offset(w * 0.7f, h * 0.38f))
    drawCircle(LogoPurple, radius = w * 0.15f, center = Offset(w * 0.33f, h * 0.64f))
    drawCircle(LogoPurple, radius = w * 0.15f, center = Offset(w * 0.67f, h * 0.64f))
    drawRoundRect(TextPrimary, Offset(w * 0.46f, h * 0.28f), Size(w * 0.08f, h * 0.48f), CornerRadius(8f, 8f))
}

private fun DrawScope.drawApple() {
    val w = size.width
    val h = size.height
    drawCircle(CardinalRed, radius = w * 0.28f, center = Offset(w * 0.42f, h * 0.56f))
    drawCircle(CardinalRed, radius = w * 0.28f, center = Offset(w * 0.58f, h * 0.56f))
    drawLine(FoxOrange, Offset(w * 0.5f, h * 0.28f), Offset(w * 0.53f, h * 0.14f), strokeWidth = 5f, cap = StrokeCap.Round)
    drawOval(FeatherGreen, Offset(w * 0.53f, h * 0.14f), Size(w * 0.2f, h * 0.12f))
}
