package com.dergoogler.mmrl.webui.view

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dergoogler.mmrl.ext.exception.BrickException
import com.dergoogler.mmrl.ext.nullply
import com.dergoogler.mmrl.webui.R
import com.dergoogler.mmrl.webui.model.WXEvent
import com.dergoogler.mmrl.webui.model.WXRefreshEventData
import com.dergoogler.mmrl.webui.util.WebUIOptions

/**
 * A custom view that extends [FrameLayout] and implements [SwipeRefreshLayout.OnRefreshListener].
 * This view is designed to display web content using a [WXView] and optionally provide pull-to-refresh functionality
 * via a [WXSwipeRefresh] layout.
 *
 * The appearance and behavior of this view can be customized through [WebUIOptions].
 *
 * @constructor Creates a [WebUIXView] with the specified context. This constructor is typically used
 *              when inflating the view from XML.
 * @param context The context in which the view is running.
 *
 * @constructor Creates a [WebUIXView] with the provided [WebUIOptions].
 * @param options The configuration options for the WebUI.
 *
 * @property wx The underlying [WXView] instance responsible for rendering web content.
 *              Accessing this property before it's initialized (which happens in the constructor taking [WebUIOptions])
 *              will result in an [IllegalStateException].
 * @property swipeView The [WXSwipeRefresh] layout that enables pull-to-refresh functionality.
 *                     This property is only initialized if `options.config.pullToRefresh` is true.
 *                     Accessing this property before it's initialized (or if pull-to-refresh is disabled)
 *                     will result in an [IllegalStateException].
 * @property options The [WebUIOptions] used to configure this view.
 *                 Accessing this property before it's initialized (which happens in the constructor taking [WebUIOptions])
 *                 will result in an [IllegalStateException].
 */
open class WebUIXView : FrameLayout, SwipeRefreshLayout.OnRefreshListener {
    constructor(context: Context) : super(context)

    private var mView: WXView? = null
    private var mOptions: WebUIOptions? = null
    private var mSwipeView: WXSwipeRefresh? = null
    private val mLayoutParams: LayoutParams? = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT
    )

    /**
     * Getter for the [WXView] instance.
     * @return The [WXView] instance.
     * @throws IllegalStateException if [mView] is null.
     */
    var wx
        get() = checkNotNull(mView) {
            "mView cannot be null"
        }
        set(value) {
            mView = value
        }

    /**
     * The [WXSwipeRefresh] view for this [WebUIXView].
     *
     * @throws IllegalStateException if accessed before being initialized.
     */
    var swipeView
        get() = checkNotNull(mSwipeView) {
            "mSwipeView cannot be null"
        }
        set(value) {
            mSwipeView = value
        }

    /**
     * Options for the WebUI.
     *
     * @see WebUIOptions
     */
    var options
        get() = checkNotNull(mOptions) {
            "mOptions cannot be null"
        }
        set(value) {
            mOptions = value
        }


    constructor(options: WebUIOptions) : this(options.context) {
        val wxView = WXView(options)

        id = R.id.webuixview

        this.mView = wxView
        this.mOptions = options
        this.mSwipeView = WXSwipeRefresh(options.context, wxView)

        mSwipeView?.nullply {
            isEnabled = options.config.pullToRefresh
        }

        val mainView =
            createMainView() ?: throw BrickException("Failed to create main WebUI X View")

        layoutParams = mLayoutParams

        addView(mainView, mLayoutParams)
    }


    private fun createMainView(): View? {
        if (options.config.pullToRefresh) {
            return swipeView.nullply {
                with(options.colorScheme) {
                    setProgressBackgroundColorSchemeColor(surfaceColorAtElevation(1.dp).toArgb())
                    setColorSchemeColors(
                        primary.toArgb(),
                        secondary.toArgb(),
                    )
                }

                var initialOffsetSet = false

                // Observe insets changes
                wx.doOnAttach { attachedView ->
                    ViewCompat.setOnApplyWindowInsetsListener(attachedView) { v, insets ->
                        val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

                        if (!initialOffsetSet && topInset > 0) {
                            setProgressViewOffset(false, 0, topInset + 32)
                            initialOffsetSet = true
                            Log.d("INSETS", "Applied top inset: $topInset")
                        }

                        insets
                    }
                }

                setOnRefreshListener(this@WebUIXView)

                addView(wx, mLayoutParams)
            }
        }

        return wx
    }

    override fun onRefresh() {
        // Disabled pull-to-refresh
        if (!options.config.pullToRefresh) return

        if (options.config.refreshInterceptor == "javascript") {
            wx.postWXEvent(
                type = WXEvent.WX_ON_REFRESH,
                data = WXRefreshEventData(
                    isRefreshing = swipeView.isRefreshing,
                    isShown = swipeView.isShown,
                    isEnabled = swipeView.isEnabled,
                )
            )
            return
        }

        // view.reload() somehow doesn't work
        wx.loadDomain()
    }
}