package com.dergoogler.mmrl.wx.model

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.ext.toCssValue
import com.dergoogler.mmrl.ui.component.button.defaultFilledTonalButtonColors
import com.dergoogler.mmrl.ui.component.card.defaultCardColors
import java.util.Locale
import kotlin.reflect.full.memberProperties

data class WebColors(val colorScheme: ColorScheme) {
    val filledTonalButtonColors = colorScheme.defaultFilledTonalButtonColors
    val cardColors = colorScheme.defaultCardColors

    private inline fun <reified T : Any> T.map(): Map<String, Color> = T::class.memberProperties
        .filter { it.returnType.classifier == Color::class }
        .associateBy({ it.name }, { it.get(this) as Color })

    val colorSchemeMap = colorScheme.map()
    val filledTonalButtonColorsMap = filledTonalButtonColors.map()
    val cardColorsMap = cardColors.map()

    val allCssColors = buildString {
        appendLine(":root {")

        appendTabbedLine("/* App Base Colors */")
        colorSchemeMap.forEach { (name, color) ->
            appendTabbedLine("--$name: ${color.toCssValue()};")

            // Manual inject this because it not a default
            if (name == "surface") {
                appendTabbedLine(
                    "--tonalSurface: ${
                        colorScheme.surfaceColorAtElevation(1.dp).toCssValue()
                    };"
                )
            }
        }

        appendTabbedLine("/* Filled Tonal Button Colors */")
        filledTonalButtonColorsMap.forEach { (name, color) ->
            appendTabbedLine("--filledTonalButton${name.capitalize()}: ${color.toCssValue()};")
        }

        appendTabbedLine("/* Filled Card Colors */")
        cardColorsMap.forEach { (name, color) ->
            appendTabbedLine("--filledCard${name.capitalize()}: ${color.toCssValue()};")
        }

        appendLine("}")
    }

    fun String.capitalize() =
        this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    fun StringBuilder.appendTabbedLine(line: String, count: Int = 1) {
        val tabs = "\t".repeat(count)
        appendLine("$tabs$line")
    }
}