package com.dergoogler.mmrl.wx.ui.component.events

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.pow
import kotlin.random.Random

private data class Streak(
    val cx: Float,
    val strokeWidth: Float,
    val maxLen: Float,
    val wobble: Float,
    val hasHair: Boolean,
    val hairOffset: Float,
    val hairLenFrac: Float,
    val durationMillis: Int,
    val delayMillis: Int
)

private data class Droplet(
    val cx: Float,
    val startYFrac: Float,
    val radius: Float,
    val fallSpeed: Float,
    val durationMillis: Int,
    val delayMillis: Int
)

@Composable
fun BloodDripOverlay(
    modifier: Modifier = Modifier,
    streakCount: Int = 120,
    maxTotalHeight: Float = 160f,
    seed: Long = 1337L,
    color: Color = Color(0xFFB5121B),
    darkColor: Color = Color(0xFF3A0000)
) {
    val streaks = remember(seed, streakCount) {
        val random = Random(seed)
        val xs = (0 until streakCount).map { random.nextFloat() }.sorted()

        xs.map { x ->
            val lenRoll = random.nextFloat().pow(3.4f)
            val maxLen = 10f + lenRoll * 150f

            Streak(
                cx = x,
                strokeWidth = 2f + random.nextFloat().pow(1.5f) * 4.5f,
                maxLen = maxLen,
                wobble = (random.nextFloat() - 0.5f) * 5f,
                hasHair = random.nextFloat() < 0.3f,
                hairOffset = (2f + random.nextFloat() * 5f) * (if (random.nextBoolean()) 1f else -1f),
                hairLenFrac = 0.4f + random.nextFloat() * 0.7f,
                // much longer, more spread cycles
                durationMillis = 11000 + random.nextInt(9000),
                delayMillis = random.nextInt(10000)
            )
        }
    }

    val droplets = remember(seed, streakCount) {
        val random = Random(seed + 99)
        List((streakCount * 0.9f).toInt()) {
            Droplet(
                cx = random.nextFloat(),
                startYFrac = 0.2f + random.nextFloat() * 0.7f,
                radius = 1.2f + random.nextFloat().pow(1.8f) * 3f,
                fallSpeed = 14f + random.nextFloat() * 28f,
                durationMillis = 6000 + random.nextInt(6000),
                delayMillis = random.nextInt(9000)
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "blood_drip")

    val streakProgress = streaks.map { cfg ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(cfg.durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(cfg.delayMillis, StartOffsetType.FastForward)
            ),
            label = "streak"
        )
    }

    val dropletProgress = droplets.map { cfg ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(cfg.durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(cfg.delayMillis, StartOffsetType.FastForward)
            ),
            label = "droplet"
        )
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth()) {
            val width = size.width

            drawRect(color = darkColor, size = Size(width, 3f))

            streaks.forEachIndexed { i, cfg ->
                val p = streakProgress[i].value

                // Phase layout across the full cycle (fractions of p):
                //   0.00–0.18  grow from 0 to maxLen
                //   0.18–0.75  hold at full length, fully opaque
                //   0.75–1.00  fade alpha 1 -> 0 while staying at full length
                // Because alpha is already 0 right before p wraps to 0,
                // and the next cycle's alpha at p=0 starts at 1 with len=0,
                // there's no visible pop — it's invisible on both sides of the seam.
                val growFraction = (p / 0.18f).coerceIn(0f, 1f)
                val len = cfg.maxLen * growFraction

                val alpha = when {
                    p < 0.75f -> 1f
                    else -> (1f - (p - 0.75f) / 0.25f).coerceIn(0f, 1f)
                }

                if (len < 1f || alpha < 0.01f) return@forEachIndexed

                val cx = cfg.cx * width

                fun drawDrip(x: Float, dripLen: Float, strokeW: Float) {
                    val path = Path().apply {
                        moveTo(x, 0f)
                        quadraticBezierTo(x + cfg.wobble, dripLen * 0.5f, x, dripLen)
                    }
                    drawPath(
                        path,
                        color = color.copy(alpha = color.alpha * alpha),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    drawCircle(
                        color = color.copy(alpha = color.alpha * alpha),
                        radius = strokeW * 0.55f,
                        center = Offset(x, dripLen)
                    )
                }

                drawDrip(cx, len, cfg.strokeWidth)

                if (cfg.hasHair) {
                    drawDrip(cx + cfg.hairOffset, len * cfg.hairLenFrac, cfg.strokeWidth * 0.45f)
                }
            }

            droplets.forEachIndexed { i, cfg ->
                val p = dropletProgress[i].value
                val startY = cfg.startYFrac * maxTotalHeight * 0.6f
                val fallY = startY + p * cfg.fallSpeed
                // fade the droplet out over the back half of its own life too, same reasoning
                val alpha = when {
                    p < 0.5f -> 1f
                    else -> (1f - (p - 0.5f) / 0.5f).coerceIn(0f, 1f)
                }
                if (fallY < maxTotalHeight && alpha > 0.01f) {
                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = cfg.radius,
                        center = Offset(cfg.cx * width, fallY)
                    )
                }
            }
        }
    }
}