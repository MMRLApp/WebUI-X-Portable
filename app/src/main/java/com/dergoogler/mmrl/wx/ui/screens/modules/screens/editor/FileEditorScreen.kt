@file:Suppress("CanBeParameter", "ClassName")

package com.dergoogler.mmrl.wx.ui.screens.modules.screens.editor

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.platform.content.LocalModule
import com.dergoogler.mmrl.platform.file.SuFile
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toSuFile
import com.dergoogler.mmrl.ui.component.NavigateUpTopBar
import com.dergoogler.mmrl.ui.component.dialog.ConfirmDialog
import com.dergoogler.mmrl.ui.component.scaffold.Scaffold
import com.dergoogler.mmrl.ui.component.toolbar.ToolbarTitle
import com.dergoogler.mmrl.wx.R
import com.dergoogler.mmrl.wx.datastore.providable.LocalUserPreferences
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
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
    private val colorScheme: ColorScheme,
    private val darkMode: Boolean,
    private val initialFile: SuFile?,
    private val threadSafe: Boolean = true,
    private val textStyle: TextStyle,
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
            setColor(ANNOTATION, colorScheme.background.lighten(0.1f))
            setColor(FUNCTION_NAME, colorScheme.primary.darken(0.2f))
            setColor(IDENTIFIER_NAME, colorScheme.primary.darken(0.1f))
            setColor(IDENTIFIER_VAR, colorScheme.secondary.darken(0.15f))
            setColor(LITERAL, colorScheme.tertiary.lighten(0.2f))
            setColor(OPERATOR, colorScheme.primary.darken(0.3f))
            setColor(COMMENT, colorScheme.outline.darken(0.1f))
            setColor(KEYWORD, colorScheme.secondary.lighten(0.2f))
            setColor(WHOLE_BACKGROUND, colorScheme.background)
            setColor(TEXT_NORMAL, colorScheme.onBackground)
            setColor(LINE_NUMBER_BACKGROUND, colorScheme.surface.darken(0.05f))
            setColor(LINE_NUMBER, colorScheme.outlineVariant.lighten(0.0465f))
            setColor(LINE_DIVIDER, colorScheme.outlineVariant)
            setColor(SCROLL_BAR_THUMB, colorScheme.primary.copy(alpha = 0.4535f))
            setColor(
                SCROLL_BAR_THUMB_PRESSED,
                colorScheme.primary.darken(0.1f).copy(alpha = 0.4535f)
            )
            setColor(SELECTED_TEXT_BACKGROUND, colorScheme.primaryContainer.lighten(0.15f))
            setColor(MATCHED_TEXT_BACKGROUND, colorScheme.secondaryContainer.lighten(0.2f))
            setColor(LINE_NUMBER_CURRENT, colorScheme.primary.darken(0.1f))
            setColor(CURRENT_LINE, colorScheme.surfaceVariant.darken(0.05f))
            setColor(SELECTION_INSERT, colorScheme.primary.lighten(0.1f))
            setColor(SELECTION_HANDLE, colorScheme.primary.darken(0.1f))
            setColor(BLOCK_LINE, colorScheme.outlineVariant.darken(0.05f))
            setColor(BLOCK_LINE_CURRENT, colorScheme.onSurfaceVariant.darken(0.2f))
            setColor(NON_PRINTABLE_CHAR, colorScheme.inverseOnSurface.darken(0.3f))
            setColor(TEXT_SELECTED, colorScheme.onPrimary.darken(0.1f))
        }

        editor.apply {
            setText(content)
            setTextSize(textStyle.fontSize.value)
            setColorScheme(scheme)
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
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    textStyle: TextStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
): CodeEditorState {
    val context = LocalContext.current
    val prefs = LocalUserPreferences.current
    val scope = rememberCoroutineScope()
    return remember(prefs) {
        CodeEditorState(
            scope = scope,
            context = context,
            colorScheme = colorScheme,
            initialFile = file,
            threadSafe = threadSafe,
            textStyle = textStyle,
            darkMode = prefs.isDarkMode(),
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
fun FileEditorScreen(module: LocalModule, path: String) {
    val navigator = LocalDestinationsNavigator.current
    val file = remember(path) { path.toSuFile() }
    val state = rememberCodeEditorState(
        file = file,
    )

    val confirmExit = rememberVisibleState {
        ConfirmDialog(
            title = "Unsaved",
            description = "There'll unsaved changes in your file. Do you want exit?",
            onConfirm = {
                it.hide()
                navigator.popBackStack()
            },
            onClose = {
                it.hide()
            }
        )
    }

    val backClick: () -> Unit = remember {
        {
            if (state.isModified) {
                confirmExit.show()
            } else {
                navigator.popBackStack()
            }
        }
    }

    BackHandler(onBack = backClick)

    Scaffold(
        topBar = {
            NavigateUpTopBar(
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
    ) { innerPadding ->
        CodeEditor(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            state = state
        )
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