package com.dergoogler.mmrl.wx.ui.webui.devtools.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dergoogler.mmrl.ext.toCssValue
import com.dergoogler.mmrl.wx.ui.webui.WebUIActivity
import com.dergoogler.mmrl.wx.ui.webui.devtools.LocalDevTools
import com.dergoogler.mmrl.wx.ui.webui.devtools.LocalWebUI
import dev.mmrlx.compose.ui.Badge
import dev.mmrlx.compose.ui.BadgeVariant
import dev.mmrlx.compose.ui.list.List
import dev.mmrlx.compose.ui.list.ListScope
import dev.mmrlx.compose.ui.list.component.RawItem
import dev.mmrlx.compose.ui.list.component.item.Description
import dev.mmrlx.compose.ui.list.component.item.Supporting
import dev.mmrlx.compose.ui.list.component.item.Title
import dev.mmrlx.webui.console.iife

@Composable
fun SnippetsTab() {
    val webui = LocalWebUI.current
    val devTools = LocalDevTools.current
    val colorScheme = MaterialTheme.colorScheme

    List(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val placement = 1
        SnippetBox(
            active = devTools.borderAllState != null,
            name = "Border All",
            description = "Add color borders to all elements"
        ) {
            if (devTools.borderAllState == null) {
                devTools.borderAllState =
                    webui.runCss(
                        "wx-devtools-border-all",
                        "* { outline: ${placement}px dashed ${colorScheme.primary.toCssValue()}; outline-offset: -${placement + 1}px; }"
                    )
            } else {
                devTools.borderAllState?.remove()
                devTools.borderAllState = null
                return@SnippetBox
            }
        }

        SnippetBox(
            name = "Refresh Page",
            description = "Refresh the Website"
        ) {
            webui.runJs("location.reload()")
        }

        SnippetBox(
            name = "Reload WebUI",
            description = "Completely reloads the WebUI"
        ) {
            WebUIActivity.recompose()
        }

        SnippetBox(
            active = devTools.editContentState != null && devTools.editContentState == true,
            name = "Edit Page",
            description = "Toggle body contentEditable"
        ) {
            webui.runJs("var state = document.body.contentEditable !== 'true';document.body.contentEditable = state;return state;".iife) {
                devTools.editContentState = it.toBooleanStrictOrNull()
            }
        }
    }
}

@Composable
fun ListScope.SnippetBox(
    active: Boolean = false,
    name: String,
    description: String,
    action: () -> Unit,
) {
    RawItem(
        modifier = Modifier
            .clickable(onClick = action)
            .contentPadding()
    ) {
        Title(name)
        Description(description)
        if (active) {
            Supporting {
                Badge("Active", variant = BadgeVariant.Default)
            }
        }
    }
}