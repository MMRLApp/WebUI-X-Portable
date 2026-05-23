@file:Suppress("CanBeParameter", "ClassName")

package com.dergoogler.mmrl.wx.ui.screens.modules.screens.editor

import android.content.Context
import android.graphics.Typeface
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
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.ANNOTATION
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.BLOCK_LINE
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.BLOCK_LINE_CURRENT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMMENT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.CURRENT_LINE
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.FUNCTION_NAME
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.IDENTIFIER_NAME
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.IDENTIFIER_VAR
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.KEYWORD
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_DIVIDER
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER_CURRENT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LITERAL
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.MATCHED_TEXT_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.NON_PRINTABLE_CHAR
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.OPERATOR
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SCROLL_BAR_THUMB
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SCROLL_BAR_THUMB_PRESSED
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SELECTED_TEXT_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SELECTION_HANDLE
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SELECTION_INSERT
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.TEXT_NORMAL
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.TEXT_SELECTED
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.WHOLE_BACKGROUND
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class CodeEditorState(
    private val scope: CoroutineScope,
    private val context: Context,
    private val colors: Colors,
    private val darkMode: Boolean,
    private val initialFile: SuFile?,
    private val threadSafe: Boolean = true,
    private val textStyle: TextStyle,
    private val typeface: Typeface,
) {
    val file: SuFile? by lazy {
        if (initialFile == null) return@lazy null

        val f = SuFile(initialFile.path)

        if (!f.exists()) {
            isSaveAllowed = false
            return@lazy null
        }

        if (f.isDirectory) {
            isSaveAllowed = false
            return@lazy null
        }

        return@lazy f
    }

    var isSaveAllowed by mutableStateOf(true)
    var content by mutableStateOf(Content(file?.readText(), threadSafe))
    var isModified by mutableStateOf(false)
    val editor = CodeEditor(context)

    fun saveFile() {
        if (!isModified) return

        if (!isSaveAllowed) {
            Toast.makeText(context, "Cannot save", Toast.LENGTH_SHORT).show()
            return
        }

        val myFile =
            file ?: run {
                Toast.makeText(context, "Cannot save", Toast.LENGTH_SHORT).show()
                return
            }

        scope.launch {
            myFile.writeText(editor.text.toString())
            isModified = false
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun init() {
        val scheme = FUCK_THIS_SHIT_EDITOR_COLOR_SCHEME(darkMode).apply {
            setColor(ANNOTATION, colors.background.lighten(0.1f))
            setColor(FUNCTION_NAME, colors.primary.darken(0.2f))
            setColor(IDENTIFIER_NAME, colors.primary.darken(0.1f))
            setColor(IDENTIFIER_VAR, colors.secondary.darken(0.15f))
            setColor(LITERAL, colors.accent.lighten(0.2f))
            setColor(OPERATOR, colors.primary.darken(0.3f))
            setColor(COMMENT, colors.border.darken(0.1f))
            setColor(KEYWORD, colors.secondary.lighten(0.2f))
            setColor(WHOLE_BACKGROUND, colors.background)
            setColor(TEXT_NORMAL, colors.foreground)
            setColor(LINE_NUMBER_BACKGROUND, colors.card.darken(0.05f))
            setColor(LINE_NUMBER, colors.mutedForeground.lighten(0.0465f))
            setColor(LINE_DIVIDER, colors.border)
            setColor(SCROLL_BAR_THUMB, colors.primary.copy(alpha = 0.4535f))
            setColor(
                SCROLL_BAR_THUMB_PRESSED,
                colors.primary.darken(0.1f).copy(alpha = 0.4535f)
            )
            setColor(
                SELECTED_TEXT_BACKGROUND,
                colors.primary.lighten(0.15f).copy(alpha = 0.25f)
            )
            setColor(
                MATCHED_TEXT_BACKGROUND,
                colors.secondary.lighten(0.2f).copy(alpha = 0.25f)
            )
            setColor(LINE_NUMBER_CURRENT, colors.primary.darken(0.1f))
            setColor(CURRENT_LINE, colors.muted.darken(0.05f))
            setColor(SELECTION_INSERT, colors.primary.lighten(0.1f))
            setColor(SELECTION_HANDLE, colors.primary.darken(0.1f))
            setColor(BLOCK_LINE, colors.border.darken(0.05f))
            setColor(BLOCK_LINE_CURRENT, colors.mutedForeground.darken(0.2f))
            setColor(NON_PRINTABLE_CHAR, colors.popoverForeground.darken(0.3f))
            setColor(TEXT_SELECTED, colors.primaryForeground.darken(0.1f))
        }

        editor.apply {
            setText(content)
            setTextSize(textStyle.fontSize.value)
            setColorScheme(scheme)
            setTypefaceText(typeface)
            setHighlightCurrentLine(true)
            setEditable(true)
        }

        content.addContentListener(contentListener)
    }

    init {
        init()
    }

    private val contentListener
        get() = object : ContentListener {
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


    private inner class FUCK_THIS_SHIT_EDITOR_COLOR_SCHEME(
        private val darkMode: Boolean,
    ) : EditorColorScheme(darkMode) {
        fun Color.darken(fraction: Float) = lerp(this, Color.Black, fraction)
        fun Color.lighten(fraction: Float) = lerp(this, Color.White, fraction)
        fun setColor(type: Int, color: Color) = super.setColor(type, color.toArgb())
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
    val context = LocalContext.current
    val prefs = LocalUserPreferences.current

    val typeface by rememberTypefaceFrom(textStyle)
    val scope = rememberCoroutineScope()

    return remember(prefs) {
        CodeEditorState(
            scope = scope,
            context = context,
            colors = colors,
            initialFile = file,
            threadSafe = threadSafe,
            textStyle = textStyle,
            darkMode = prefs.isDarkMode(),
            typeface = typeface,
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


interface VisibleState {
    val isVisible: Boolean
    fun show()
    fun hide()
}

@Composable
fun rememberVisibleState(
    initialState: Boolean = false,
    content: @Composable (VisibleState) -> Unit,
): VisibleState {
    var visible by remember { mutableStateOf(initialState) }

    val obj = remember(visible) {
        object : VisibleState {
            override val isVisible = visible
            override fun show() {
                visible = true
            }

            override fun hide() {
                visible = false
            }
        }
    }

    if (visible) content(obj)

    return obj
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