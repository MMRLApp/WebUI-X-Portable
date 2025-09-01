package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.dergoogler.mmrl.wx.ui.providable.LocalDestinationsNavigator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.html.InputType

data class CodeEditorState(
    private val scope: CoroutineScope,
    private val context: Context,
    private val colorScheme: ColorScheme,
    private val initialFile: SuFile?,
    private val threadSafe: Boolean = true,
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
            file ?: return Toast.makeText(context, "Cannot save", Toast.LENGTH_SHORT).show()

        scope.launch {
            myFile.writeText(editor.text.toString())
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
        }
    }

    init {
        editor.apply {
            setText(content)
            setColorScheme(colorScheme)
            setHighlightCurrentLine(true)
            setEditable(true)
        }

        content.addContentListener(contentListener)
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
}

@Composable
fun rememberCodeEditorState(
    file: SuFile? = null,
    threadSafe: Boolean = true,
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
): CodeEditorState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember {
        CodeEditorState(
            scope = scope,
            context = context,
            colorScheme = colorScheme,
            initialFile = file,
            threadSafe = threadSafe
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

@Destination<RootGraph>
@Composable
fun FileEditorScreen(module: LocalModule, path: String) {
    val navigator = LocalDestinationsNavigator.current

    val file = remember(path) { path.toSuFile() }

    val state = rememberCodeEditorState(
        file = file
    )

    val confirmExit = rememberVisibleState {
        ConfirmDialog(
            title = "Unsaved",
            description = "There're unsaved changes in your file. Do you want exit?",
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
                        title = InputType.file.name,
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
