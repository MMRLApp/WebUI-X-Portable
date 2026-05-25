@file:Suppress("CanBeParameter", "ClassName")

package com.dergoogler.mmrl.wx.ui.screens.modules.screens.editor

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.model.module.AdbPath
import com.dergoogler.mmrl.wx.ui.component.LocalModule
import com.dergoogler.mmrl.wx.ui.component.ModuleScope
import com.dergoogler.mmrl.wx.ui.component.NavigateUpToolbar
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import dev.mmrlx.compose.ui.LocalTextStyle
import dev.mmrlx.compose.ui.Text
import dev.mmrlx.compose.ui.button.Button
import dev.mmrlx.compose.ui.button.ButtonVariant
import dev.mmrlx.compose.ui.dialog.Content
import dev.mmrlx.compose.ui.dialog.Footer
import dev.mmrlx.compose.ui.dialog.Title
import dev.mmrlx.compose.ui.dialog.rememberDialog
import dev.mmrlx.compose.ui.icon.Icon
import dev.mmrlx.compose.ui.icon.IconButton
import dev.mmrlx.compose.ui.scaffold.Scaffold
import dev.mmrlx.compose.ui.theme.Colors
import dev.mmrlx.compose.ui.theme.MMRLXTheme
import dev.mmrlx.compose.ui.toolbar.ToolbarTitle
import dev.mmrlx.nio.SuFile
import dev.mmrlx.nio.SuFile.Companion.toSuFile
import dev.mmrlx.nio.readText
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.eclipse.tm4e.core.registry.IThemeSource

object TextMateManager {

    private var initialized = false

    fun initialize(debug: Boolean, adbPath: AdbPath) {
        if (initialized) return

        initialized = true

        FileProviderRegistry
            .getInstance()
            .addFileProvider(
                AdbPathFileResolver(debug, adbPath)
            )

        GrammarRegistry
            .getInstance()
            .loadGrammars("textmate/languages.json")
    }
}

data class CodeEditorState(
    private val scope: CoroutineScope,
    private val adbPath: AdbPath,
    private val context: Context,
    private val colors: Colors,
    private val darkMode: Boolean,
    private val debug: Boolean,
    private val initialFile: SuFile?,
    private val threadSafe: Boolean = true,
    private val textStyle: TextStyle,
    private val typeface: Typeface,
) {

    val editor = CodeEditor(context)

    var isSaveAllowed by mutableStateOf(true)

    val file: SuFile? by lazy {

        if (initialFile == null) {
            isSaveAllowed = false
            return@lazy null
        }

        val f = SuFile(initialFile.path)

        if (!f.exists()) {
            isSaveAllowed = false
            return@lazy null
        }

        if (f.isDirectory) {
            isSaveAllowed = false
            return@lazy null
        }

        f
    }

    var content by mutableStateOf(
        Content(
            file?.readText(),
            threadSafe
        )
    )

    var isModified by mutableStateOf(false)

    init {
        initialize()
    }

    private fun Color.darken(fraction: Float) = lerp(this, Color.Black, fraction)
    private fun Color.lighten(fraction: Float) = lerp(this, Color.White, fraction)

    private fun buildTextMateTheme(): String {
        val comment = if (darkMode) "#8B949E" else "#6B7280"
        val keyword = if (darkMode) "#FF7B72" else "#C0392B"
        val string = if (darkMode) "#A5D6FF" else "#1155A3"
        val function = if (darkMode) "#D2A8FF" else "#7C3AED"
        val variable = if (darkMode) "#FFA657" else "#B45309"
        val type = if (darkMode) "#7EE787" else "#166534"
        val constant = if (darkMode) "#79C0FF" else "#0369A1"
        val tag = if (darkMode) "#7EE787" else "#166534"
        val attribute = if (darkMode) "#FFA657" else "#9A6700"
        val property = if (darkMode) "#79C0FF" else "#0E7490"
        val number = if (darkMode) "#79C0FF" else "#047857"
        val punctuation = if (darkMode) "#C9D1D9" else "#374151"
        val cssValue = if (darkMode) "#A5D6FF" else "#1155A3"

        return """
{
  "name": "Sora GitHub Contrast",
  "type": "${if (darkMode) "dark" else "light"}",
  "tokenColors": [
    {
      "name": "Comments",
      "scope": [
        "comment",
        "comment.line",
        "comment.block",
        "punctuation.definition.comment"
      ],
      "settings": {
        "foreground": "$comment"
      }
    },
    {
      "name": "Keywords",
      "scope": [
        "keyword",
        "keyword.control",
        "keyword.operator.word",
        "storage",
        "storage.type",
        "storage.modifier"
      ],
      "settings": {
        "foreground": "$keyword"
      }
    },
    {
      "name": "Operators / punctuation",
      "scope": [
        "keyword.operator",
        "punctuation",
        "meta.brace",
        "meta.delimiter"
      ],
      "settings": {
        "foreground": "$punctuation"
      }
    },
    {
      "name": "Strings",
      "scope": [
        "string",
        "string.quoted",
        "punctuation.definition.string"
      ],
      "settings": {
        "foreground": "$string"
      }
    },
    {
      "name": "Numbers / constants",
      "scope": [
        "constant.numeric",
        "constant.language",
        "constant.character.escape",
        "constant"
      ],
      "settings": {
        "foreground": "$number"
      }
    },
    {
      "name": "Functions",
      "scope": [
        "entity.name.function",
        "support.function",
        "meta.function-call",
        "variable.function"
      ],
      "settings": {
        "foreground": "$function"
      }
    },
    {
      "name": "Variables",
      "scope": [
        "variable",
        "variable.other",
        "variable.parameter"
      ],
      "settings": {
        "foreground": "$variable"
      }
    },
    {
      "name": "Types / classes",
      "scope": [
        "entity.name.type",
        "entity.name.class",
        "support.type",
        "support.class",
        "storage.type.java"
      ],
      "settings": {
        "foreground": "$type"
      }
    },
    {
      "name": "HTML/XML tag",
      "scope": [
        "entity.name.tag",
        "punctuation.definition.tag",
        "meta.tag"
      ],
      "settings": {
        "foreground": "$tag"
      }
    },
    {
      "name": "HTML/XML attributes",
      "scope": [
        "entity.other.attribute-name",
        "entity.other.attribute-name.html",
        "entity.other.attribute-name.css"
      ],
      "settings": {
        "foreground": "$attribute"
      }
    },
    {
      "name": "CSS property names",
      "scope": [
        "support.type.property-name.css",
        "meta.property-name",
        "variable.property",
        "meta.object-literal.key"
      ],
      "settings": {
        "foreground": "$property"
      }
    },
    {
      "name": "CSS values",
      "scope": [
        "support.constant.property-value",
        "meta.property-value",
        "string.unquoted",
        "entity.other.attribute-name.id.css",
        "entity.other.attribute-name.class.css"
      ],
      "settings": {
        "foreground": "$cssValue"
      }
    },
    {
      "name": "Regex",
      "scope": [
        "string.regexp",
        "source.regexp"
      ],
      "settings": {
        "foreground": "$string"
      }
    },
    {
      "name": "Links / markup",
      "scope": [
        "markup.underline.link",
        "constant.other.reference.link"
      ],
      "settings": {
        "foreground": "$constant",
        "fontStyle": "underline"
      }
    }
  ]
}
""".trimIndent()
    }

    private fun buildColorScheme(colors: Colors): EditorColorScheme {
        val themeRegistry = ThemeRegistry.getInstance()

        // Build and register a dynamic theme from our Colors
        val themeJson = buildTextMateTheme()
        val themeModel = ThemeModel(
            IThemeSource.fromString(IThemeSource.ContentType.JSON, themeJson),
            "dynamic"
        )

        runCatching { themeRegistry.loadTheme(themeModel) }
        themeRegistry.setTheme("dynamic")

        return object : TextMateColorScheme(themeRegistry, themeModel) {
            override fun getColor(type: Int): Int {
                val color: Color? = when (type) {
                    // syntax
                    ANNOTATION -> colors.mutedForeground.lighten(0.1f)
                    FUNCTION_NAME -> colors.primary.darken(0.2f)
                    IDENTIFIER_NAME -> colors.foreground
                    IDENTIFIER_VAR -> colors.secondary.darken(0.15f)
                    LITERAL -> colors.accent
                    OPERATOR -> colors.mutedForeground
                    COMMENT -> colors.mutedForeground.lighten(0.15f)
                    KEYWORD -> colors.accent.darken(0.1f)

                    // editor chrome
                    WHOLE_BACKGROUND -> colors.background
                    TEXT_NORMAL -> colors.foreground
                    TEXT_SELECTED -> colors.primaryForeground

                    // line numbers
                    LINE_NUMBER_BACKGROUND -> colors.card.darken(0.05f)
                    LINE_NUMBER -> colors.mutedForeground
                    LINE_NUMBER_CURRENT -> colors.primary.darken(0.1f)

                    // lines / blocks
                    LINE_DIVIDER -> colors.border
                    CURRENT_LINE -> colors.muted.darken(0.05f)
                    BLOCK_LINE -> colors.border
                    BLOCK_LINE_CURRENT -> colors.border.darken(0.15f)

                    // selection / search
                    SELECTED_TEXT_BACKGROUND -> colors.primary.lighten(0.15f).copy(alpha = 0.25f)
                    MATCHED_TEXT_BACKGROUND -> colors.primary.lighten(0.3f).copy(alpha = 0.2f)
                    SELECTION_INSERT -> colors.primary.lighten(0.1f)
                    SELECTION_HANDLE -> colors.primary.darken(0.1f)

                    // scroll
                    SCROLL_BAR_THUMB -> colors.primary.copy(alpha = 0.45f)
                    SCROLL_BAR_THUMB_PRESSED -> colors.primary.darken(0.1f).copy(alpha = 0.45f)

                    // misc
                    NON_PRINTABLE_CHAR -> colors.mutedForeground.darken(0.1f)

                    else -> null
                }
                return color?.toArgb() ?: super.getColor(type)
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Initialisation
    // ---------------------------------------------------------------------------

    private fun initialize() {
        TextMateManager.initialize(debug, adbPath)

        val scopeName = when (file?.extension) {
            "kt" -> "source.kotlin"
            "kts" -> "source.kotlin"
            "java" -> "source.java"
            "js" -> "source.js"
            "ts" -> "source.ts"
            "json" -> "source.json"
            "xml" -> "text.xml"
            "html" -> "text.html.basic"
            "css" -> "source.css"
            "sh" -> "source.shell"
            "lua" -> "source.lua"
            "py" -> "source.python"
            "cpp" -> "source.cpp"
            "c" -> "source.c"
            "rs" -> "source.rust"
            else -> null
        }

        val scheme = buildColorScheme(colors)

        editor.apply {
            setText(content)
            setTextSize(textStyle.fontSize.value)
            setTypefaceText(typeface)
            scopeName?.runCatching {
                setEditorLanguage(
                    TextMateLanguage.create(this, true)
                )
            }?.onFailure {
                Log.e("CodeEditor", "Failed to set language: $it")
            }
            colorScheme = scheme
            setHighlightCurrentLine(true)
            setEditable(true)
            isWordwrap = false
            setUndoEnabled(true)
        }

        content.addContentListener(contentListener)
    }

    fun saveFile() {

        if (!isModified) return

        val target =
            file ?: run {
                Toast
                    .makeText(
                        context,
                        "Cannot save",
                        Toast.LENGTH_SHORT
                    )
                    .show()

                return
            }

        scope.launch {

            target.writeText(
                editor.text.toString()
            )

            isModified = false

            Toast
                .makeText(
                    context,
                    "Saved",
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    }

    private val contentListener
        get() =
            object : ContentListener {

                override fun beforeReplace(content: Content) {
                    isModified = true
                }

                override fun afterInsert(
                    content: Content,
                    startLine: Int,
                    startColumn: Int,
                    endLine: Int,
                    endColumn: Int,
                    insertedContent: CharSequence,
                ) {
                    isModified = true
                }

                override fun afterDelete(
                    content: Content,
                    startLine: Int,
                    startColumn: Int,
                    endLine: Int,
                    endColumn: Int,
                    deletedContent: CharSequence,
                ) {
                    isModified = true
                }
            }
}

@Composable
fun rememberCodeEditorState(
    file: SuFile? = null,
    threadSafe: Boolean = true,
    colors: Colors = MMRLXTheme.colors,
    textStyle: TextStyle = LocalTextStyle.current.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp
    ),
): CodeEditorState {
    val prefs = LocalUserPreferences.current
    val module = LocalModule.current
    val context = LocalContext.current
    val typeface by rememberTypefaceFrom(textStyle)
    val scope = rememberCoroutineScope()

    return remember {
        CodeEditorState(
            scope = scope,
            context = context,
            colors = colors,
            darkMode = true,
            initialFile = file,
            threadSafe = threadSafe,
            textStyle = textStyle,
            typeface = typeface,
            adbPath = module.adbPath,
            debug = prefs.developerMode
        )
    }
}

@Composable
fun CodeEditor(
    modifier: Modifier = Modifier,
    state: CodeEditorState,
) {
    AndroidView(
        factory = { state.editor },
        modifier = modifier,
        onRelease = { it.release() }
    )
}

@Destination<RootGraph>
@Composable
fun FileEditorScreen(moduleId: String, path: String) {
    ModuleScope(moduleId) {
        FileEditorContent(path)
    }
}

@Composable
fun FileEditorContent(path: String) {
    val module = LocalModule.current
    val navigator = LocalDestinationsNavigator.current
    val file = remember(path) { path.toSuFile() }
    val state = rememberCodeEditorState(
        file = file,
    )

    val confirmExit = rememberDialog()

    val backClick: () -> Unit = remember {
        {
            if (state.isModified) {
                confirmExit.open()
            } else {
                navigator.popBackStack()
            }
        }
    }

    BackHandler(onBack = backClick)

    Scaffold(
        toolbar = {
            NavigateUpToolbar(
                title = {
                    ToolbarTitle(
                        title = file.name,
                        subtitle = module.name
                    )
                },
                onBack = backClick,
                actions = {
                    IconButton(
                        enabled = state.isModified,
                        onClick = {
                            state.saveFile()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.device_floppy),
                            contentDescription = "Save"
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.none
    ) {
        CodeEditor(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            state = state
        )
    }

    confirmExit {
        Title {
            Text("Unsaved")
        }

        Content {
            Text("There'll unsaved changes in your file. Do you want exit?")
        }

        Footer {
            Button(
                onClick = {
                    confirmExit.close()
                },
                variant = ButtonVariant.Outline
            ) {
                Text(stringResource(R.string.cancel))
            }

            Button(
                onClick = {
                    confirmExit.close()
                    navigator.popBackStack()
                },
            ) {
                Text(stringResource(R.string.confirm))
            }
        }
    }
}

@Composable
fun rememberTypefaceFrom(textStyle: TextStyle): State<Typeface> {
    val resolver = LocalFontFamilyResolver.current
    val family = textStyle.fontFamily
    val weight = textStyle.fontWeight ?: FontWeight.Normal
    val style = textStyle.fontStyle ?: FontStyle.Normal
    val synth = textStyle.fontSynthesis ?: FontSynthesis.All
    return remember(family, weight, style, synth) {
        resolver.resolve(family, weight, style, synth)
    } as State<Typeface>
}