package com.dergoogler.mmrl.wx.ui.component.events

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Snowflake(
    val xFrac: Float,        // base horizontal position, fraction of width (0..1)
    val yFrac: Float,        // initial vertical offset, fraction of height (0..1), staggers start
    val radius: Float,
    val fallSpeed: Float,    // fraction of height fallen per second
    val swayAmplitude: Float,// horizontal sway in px
    val swayFrequency: Float,// sway cycles per second
    val swayPhase: Float,    // phase offset so flakes don't sway in sync
    val alpha: Float
)

/**
 * Full-screen falling snow overlay. Put it in a Box on top of your
 * screen content (e.g. inside the same Box that hosts a Halloween
 * scene, or its own screen), sized with fillMaxSize().
 *
 * Uses one continuously-increasing time driver (not per-flake reset
 * cycles), so flakes wrap seamlessly from bottom back to top with
 * no visible pop/blink.
 */
@Composable
fun SnowfallOverlay(
    modifier: Modifier = Modifier,
    flakeCount: Int = 120,
    seed: Long = 4242L,
    color: Color = Color.White
) {
    val flakes = remember(seed, flakeCount) {
        val random = Random(seed)
        List(flakeCount) {
            Snowflake(
                xFrac = random.nextFloat(),
                yFrac = random.nextFloat(),
                radius = 1.5f + random.nextFloat().let { it * it } * 4f, // bias toward small flakes
                fallSpeed = 0.03f + random.nextFloat() * 0.09f,          // fraction of height / sec
                swayAmplitude = 8f + random.nextFloat() * 22f,
                swayFrequency = 0.15f + random.nextFloat() * 0.35f,
                swayPhase = random.nextFloat() * (2f * PI.toFloat()),
                alpha = 0.5f + random.nextFloat() * 0.5f
            )
        }
    }

    // one driver that counts up forever, in seconds; nothing about it
    // ever resets to 0 mid-flight, so there's no shared "restart" moment.
    val transition = rememberInfiniteTransition(label = "snow_time")
    val timeSeconds by transition.animateFloat(
        initialValue = 0f,
        targetValue = 100000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100000 * 1000, easing = LinearEasing)
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        flakes.forEach { f ->
            drawFlake(f, timeSeconds, w, h, color)
        }
    }
}

private fun DrawScope.drawFlake(
    f: Snowflake,
    t: Float,
    w: Float,
    h: Float,
    color: Color
) {
    // vertical position wraps via floor-mod, so it loops top<->bottom
    // continuously instead of jumping/resetting.
    val rawY = f.yFrac + f.fallSpeed * t
    val yFracWrapped = rawY - kotlin.math.floor(rawY) // stays in [0,1), wraps smoothly
    val y = yFracWrapped * (h + f.radius * 4f) - f.radius * 2f

    val sway = sin(t * f.swayFrequency * 2f * PI.toFloat() + f.swayPhase) * f.swayAmplitude
    val x = f.xFrac * w + sway

    // fade flakes slightly as they approach the very top/bottom seam
    // so the wrap point is even less noticeable (belt-and-suspenders,
    // the modulo wrap alone already avoids any blink).
    val edgeFade = when {
        yFracWrapped < 0.03f -> yFracWrapped / 0.03f
        yFracWrapped > 0.97f -> (1f - yFracWrapped) / 0.03f
        else -> 1f
    }.coerceIn(0f, 1f)

    drawCircle(
        color = color.copy(alpha = f.alpha * edgeFade),
        radius = f.radius,
        center = Offset(x, y)
    )
}