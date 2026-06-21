package com.dergoogler.mmrl.wx.ui.webui.util

import android.webkit.WebResourceResponse
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.webui.R
import dev.mmrlx.webui.WebUI
import dev.mmrlx.webui.PathHandler
import kotlinx.html.DIV
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.lang
import kotlinx.html.li
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.onClick
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.title
import kotlinx.html.ul

internal fun PathHandler.errorResponse(
    title: String,
    description: (DIV.(WebUI) -> Unit)? = null,
    tryFollowing: List<String> = emptyList(),
    errorCode: String = "UNDEFINED",
    errorSvgIcon: (DIV.(WebUI) -> Unit)? = null,
    extraButtons: (DIV.(WebUI) -> Unit)? = null,
): WebResourceResponse {
    val str = buildString {
        with(kontext) {
            appendHTML().html {
                lang = "en"
                head {
                    meta {
                        name = "viewport"
                        content =
                            "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0"
                    }
                    link {
                        rel = "stylesheet"
                        href = "${baseUri}/internal/insets.css"
                    }
                    link {
                        rel = "stylesheet"
                        href = "${baseUri}/internal/colors.css"
                    }
                    link {
                        rel = "stylesheet"
                        href = "${baseUri}/internal/assets/error-page.css"
                    }
                    title { +title }
                }
                body {
                    div(classes = "container") {
                        div(classes = "content") {
                            errorSvgIcon.nullable {
                                div(classes = "error-icon") {
                                    it(this@errorResponse)
                                }
                            }

                            div(classes = "title") { +title }

                            description.nullable {
                                div(classes = "description") {
                                    it(this@errorResponse)
                                }
                            }

                            if (tryFollowing.isNotEmpty()) {
                                div(classes = "list") {
                                    span { +getString(R.string.requireNewVersion_try_the_following) }
                                    ul {
                                        tryFollowing.forEach { item ->
                                            li {
                                                +item
                                            }
                                        }
                                    }
                                }
                            }

                            div(classes = "code") { +errorCode }
                            div(classes = "buttons") {
                                button(classes = "refresh") {
                                    onClick = "location.reload();"
                                    +getString(R.string.requireNewVersion_refresh)
                                }

                                extraButtons.nullable {
                                    it(this@errorResponse)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    return htmlResponse(str)
}