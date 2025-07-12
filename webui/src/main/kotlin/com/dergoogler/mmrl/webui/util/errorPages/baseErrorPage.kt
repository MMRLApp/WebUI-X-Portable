package com.dergoogler.mmrl.webui.util.errorPages

import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.webui.R
import com.dergoogler.mmrl.webui.util.WebUIOptions
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

internal fun WebUIOptions.baseErrorPage(
    title: String,
    description: (DIV.(WebUIOptions) -> Unit)? = null,
    tryFollowing: List<String> = emptyList(),
    errorCode: String = "UNDEFINED",
    errorSvgIcon: (DIV.(WebUIOptions) -> Unit)? = null,
    extraButtons: (DIV.(WebUIOptions) -> Unit)? = null,
) = buildString {
    with(context) {
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
                    href = "${domain}/internal/insets.css"
                }
                link {
                    rel = "stylesheet"
                    href = "${domain}/internal/colors.css"
                }
                link {
                    rel = "stylesheet"
                    href = "${domain}/internal/assets/webui/requireNewVersion.css"
                }
                title { +title }
            }
            body {
                div(classes = "container") {
                    div(classes = "content") {
                        errorSvgIcon.nullable {
                            div(classes = "error-icon") {
                                it(this@baseErrorPage)
                            }
                        }

                        div(classes = "title") { +title }

                        description.nullable {
                            div(classes = "description") {
                                it(this@baseErrorPage)
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
                                it(this@baseErrorPage)
                            }
                        }
                    }
                }
            }
        }
    }
}