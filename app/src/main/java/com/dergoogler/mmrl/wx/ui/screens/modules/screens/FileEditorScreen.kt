package com.dergoogler.mmrl.wx.ui.screens.modules.screens

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.dergoogler.mmrl.ext.none
import com.dergoogler.mmrl.ui.component.scaffold.Scaffold
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor

data class CodeEditorState(
    private val context: Context,
    private val colorScheme: ColorScheme,
    private val initialContent: CharSequence?,
    private val threadSafe: Boolean,
) {
    var content by mutableStateOf(Content(initialContent, threadSafe))
    val editor = CodeEditor(context)

    val scheme get() = colorScheme

    init {
        editor.apply {
            setText(content)
            // setColorScheme(SoraColorScheme(scheme))
        }
    }
}

@Composable
fun rememberCodeEditorState(
    initialContent: CharSequence? = null,
    threadSafe: Boolean = true,
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
): CodeEditorState {
    val context = LocalContext.current
    return remember {
        CodeEditorState(
            context = context,
            colorScheme = colorScheme,
            initialContent = initialContent,
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
        onRelease = {
            it.release()
        }
    )
}

@Destination<RootGraph>
@Composable
fun FileEditorScreen() {
    val state = rememberCodeEditorState()

    Scaffold(
        contentWindowInsets = WindowInsets.none
    ) { innerPadding ->
        Column {
            CodeEditor(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                state = state
            )
        }
    }
}