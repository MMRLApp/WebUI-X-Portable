package com.dergoogler.mmrl.webui.util.errorPages

import kotlinx.html.DIV
import kotlinx.html.FlowOrPhrasingContent
import kotlinx.html.HTMLTag
import kotlinx.html.HtmlBlockInlineTag
import kotlinx.html.HtmlTagMarker
import kotlinx.html.TagConsumer
import kotlinx.html.emptyMap
import kotlinx.html.svg
import kotlinx.html.visit

fun DIV.zoomExclamation() {
    svg {
        attributes["viewBox"] = "0 0 24 24"
        attributes["fill"] = "currentColor"
        path {
            attributes["stroke"] = "none"
            attributes["d"] = "M0 0h24v24H0z"
            attributes["fill"] = "none"
        }
        path {
            attributes["d"] =
                "M14 3.072a8 8 0 0 1 2.32 11.834l5.387 5.387a1 1 0 0 1 -1.414 1.414l-5.388 -5.387a8 8 0 0 1 -12.905 -6.32l.005 -.285a8 8 0 0 1 11.995 -6.643m-4 8.928a1 1 0 0 0 -1 1l.007 .127a1 1 0 0 0 1.993 -.117l-.007 -.127a1 1 0 0 0 -.993 -.883m0 -6a1 1 0 0 0 -1 1v3a1 1 0 0 0 2 0v-3a1 1 0 0 0 -1 -1"
        }
    }
}

fun DIV.infoCircle() {
    svg {
        attributes["width"] = "20"
        attributes["height"] = "20"
        attributes["viewBox"] = "0 0 24 24"
        attributes["fill"] = "currentColor"
        path {
            attributes["stroke"] = "none"
            attributes["d"] = "M0 0h24v24H0z"
            attributes["fill"] = "none"
        }
        path {
            attributes["d"] =
                "M12 2c5.523 0 10 4.477 10 10a10 10 0 0 1 -19.995 .324l-.005 -.324l.004 -.28c.148 -5.393 4.566 -9.72 9.996 -9.72zm0 9h-1l-.117 .007a1 1 0 0 0 0 1.986l.117 .007v3l.007 .117a1 1 0 0 0 .876 .876l.117 .007h1l.117 -.007a1 1 0 0 0 .876 -.876l.007 -.117l-.007 -.117a1 1 0 0 0 -.764 -.857l-.112 -.02l-.117 -.006v-3l-.007 -.117a1 1 0 0 0 -.876 -.876l-.117 -.007zm.01 -3l-.127 .007a1 1 0 0 0 0 1.986l.117 .007l.127 -.007a1 1 0 0 0 0 -1.986l-.117 -.007z"
        }
    }
}

fun DIV.refresh() {
    svg {
        attributes["width"] = "20"
        attributes["height"] = "20"
        attributes["viewBox"] = "0 0 24 24"
        attributes["fill"] = "currentColor"
        attributes["stroke"] = "currentColor"
        attributes["stroke-width"] = "2"
        attributes["stroke-linecap"] = "round"
        attributes["stroke-linejoin"] = "round"
        path {
            attributes["stroke"] = "none"
            attributes["d"] = "M0 0h24v24H0z"
            attributes["fill"] = "none"
        }
        path {
            attributes["d"] = "M20 11a8.1 8.1 0 0 0 -15.5 -2m-.5 -4v4h4"
        }
        path {
            attributes["d"] = "M4 13a8.1 8.1 0 0 0 15.5 2m.5 4v-4h-4"
        }
    }
}

fun DIV.alertTriangle() {
    svg {
        attributes["viewBox"] = "0 0 24 24"
        attributes["fill"] = "currentColor"
        path {
            attributes["stroke"] = "none"
            attributes["d"] = "M0 0h24v24H0z"
            attributes["fill"] = "none"
        }
        path {
            attributes["d"] =
                "M12 1.67c.955 0 1.845 .467 2.39 1.247l.105 .16l8.114 13.548a2.914 2.914 0 0 1 -2.307 4.363l-.195 .008h-16.225a2.914 2.914 0 0 1 -2.582 -4.2l.099 -.185l8.11 -13.538a2.914 2.914 0 0 1 2.491 -1.403zm.01 13.33l-.127 .007a1 1 0 0 0 0 1.986l.117 .007l.127 -.007a1 1 0 0 0 0 -1.986l-.117 -.007zm-.01 -7a1 1 0 0 0 -.993 .883l-.007 .117v4l.007 .117a1 1 0 0 0 1.986 0l.007 -.117v-4l-.007 -.117a1 1 0 0 0 -.993 -.883z"
        }
    }
}

fun DIV.masksTheater() {
    svg {
        attributes["viewBox"] = "0 0 24 24"
        attributes["fill"] = "none"
        attributes["stroke"] = "currentColor"
        attributes["stroke-width"] = "2"
        attributes["stroke-linecap"] = "round"
        attributes["stroke-linejoin"] = "round"
        path {
            attributes["stroke"] = "none"
            attributes["d"] = "M0 0h24v24H0z"
            attributes["fill"] = "none"
        }

        path {
            attributes["d"] =
                "M13.192 9h6.616a2 2 0 0 1 1.992 2.183l-.567 6.182a4 4 0 0 1 -3.983 3.635h-1.5a4 4 0 0 1 -3.983 -3.635l-.567 -6.182a2 2 0 0 1 1.992 -2.183z"
        }
        path {
            attributes["d"] = "M15 13h.01"
        }
        path {
            attributes["d"] = "M18 13h.01"
        }
        path {
            attributes["d"] = "M15 16.5c1 .667 2 .667 3 0"
        }
        path {
            attributes["d"] =
                "M8.632 15.982a4.037 4.037 0 0 1 -.382 .018h-1.5a4 4 0 0 1 -3.983 -3.635l-.567 -6.182a2 2 0 0 1 1.992 -2.183h6.616a2 2 0 0 1 2 2"
        }
        path {
            attributes["d"] = "M6 8h.01"
        }
        path {
            attributes["d"] = "M9 8h.01"
        }
        path {
            attributes["d"] = "M6 12c.764 -.51 1.528 -.63 2.291 -.36"
        }
    }
}

@HtmlTagMarker
inline fun FlowOrPhrasingContent.path(
    crossinline block: PATH.() -> Unit = {},
): Unit = PATH(emptyMap, consumer).visit(block)

class PATH(
    initialAttributes: Map<String, String>,
    override val consumer: TagConsumer<*>,
) :
    HTMLTag("path", consumer, initialAttributes, null, true, false), HtmlBlockInlineTag
