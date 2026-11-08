package io.github.jayfunc.carpelines.presentation.floatinglyrics

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.getSystemService
import io.github.jayfunc.carpelines.R
import kotlin.math.abs
import timber.log.Timber

/**
 * Owns the floating lyric window: inflation, placement, dragging and content updates.
 *
 * The window is a plain overlay view rather than a Compose window so it can live in a service
 * without a lifecycle owner, and it uses the app's Material 3 theme so the tonal container and
 * rounded corners match the in-app M3E surfaces.
 */
internal class FloatingLyricsOverlay(
    private val context: Context,
    private val onOpenLyrics: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onPlayPause: () -> Unit,
    private val onNext: () -> Unit,
    private val onClose: () -> Unit,
) {

    private val windowManager: WindowManager? = context.getSystemService()

    private var root: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var currentLineView: TextView? = null
    private var secondaryLineView: TextView? = null
    private var playPauseButton: ImageButton? = null

    val isAttached: Boolean get() = root != null

    /** Whether the window is actually drawn; a hidden-by-policy window reports false. */
    val isShown: Boolean get() = root?.isShown == true

    /** Adds the window when it is not attached yet. */
    fun show() {
        if (root == null) attachWindow()
    }

    fun hide() {
        val view = root ?: return
        runCatching { windowManager?.removeView(view) }
            .onFailure { Timber.w(it, "Could not remove the floating lyrics window") }
        root = null
        layoutParams = null
        currentLineView = null
        secondaryLineView = null
        playPauseButton = null
    }

    fun update(currentLine: String, secondaryLine: String, isPlaying: Boolean) {
        currentLineView?.text = currentLine
        secondaryLineView?.text = secondaryLine
        secondaryLineView?.visibility = if (secondaryLine.isBlank()) View.GONE else View.VISIBLE
        playPauseButton?.apply {
            setImageResource(
                if (isPlaying) R.drawable.rounded_pause_filled_24
                else R.drawable.rounded_play_arrow_filled_24
            )
            contentDescription = context.getString(
                if (isPlaying) R.string.cd_pause else R.string.cd_play
            )
        }
    }

    private fun attachWindow() {
        val manager = windowManager ?: return
        val themedContext = androidx.appcompat.view.ContextThemeWrapper(context, R.style.Theme_CarpeLines)
        val view = LayoutInflater.from(themedContext).inflate(R.layout.floating_lyrics_window, null)
        // MIUI sometimes removes the overlay when the keyguard takes over during an unlock
        // animation. Forget the stale root so the next sync can attach a fresh window.
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit

            override fun onViewDetachedFromWindow(v: View) {
                Timber.d("FloatingLyricsOverlay: detached")
                if (root === v) {
                    root = null
                    layoutParams = null
                    currentLineView = null
                    secondaryLineView = null
                    playPauseButton = null
                }
            }
        })
        currentLineView = view.findViewById(R.id.floating_lyrics_current_line)
        secondaryLineView = view.findViewById(R.id.floating_lyrics_secondary_line)
        playPauseButton = view.findViewById(R.id.floating_lyrics_play_pause)

        view.findViewById<ImageButton>(R.id.floating_lyrics_previous).setOnClickListener { onPrevious() }
        view.findViewById<ImageButton>(R.id.floating_lyrics_play_pause).setOnClickListener { onPlayPause() }
        view.findViewById<ImageButton>(R.id.floating_lyrics_next).setOnClickListener { onNext() }
        view.findViewById<ImageButton>(R.id.floating_lyrics_close).setOnClickListener { onClose() }
        attachDragAndTap(view.findViewById(R.id.floating_lyrics_text_area))

        val widthPx = FloatingLyricsPlacement.widthPx(context)

        val params = WindowManager.LayoutParams(
            widthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            BASE_FLAGS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = FloatingLyricsPlacement.restoreX(context, widthPx)
            y = FloatingLyricsPlacement.restoreY(context)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val added = runCatching { manager.addView(view, params) }
            .onFailure { Timber.w(it, "Could not show the floating lyrics window") }
            .isSuccess
        if (added) {
            root = view
            layoutParams = params
            Timber.d("FloatingLyricsOverlay: attached")
        }
    }

    /**
     * The lyric block is the drag handle: a quick tap opens the lyrics page, a move repositions
     * the window and remembers where the user left it.
     */
    private fun attachDragAndTap(target: View) {
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragging = false

        target.setOnTouchListener { _, event ->
            val params = layoutParams
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params?.x ?: 0
                    startY = params?.y ?: 0
                    dragging = false
                }

                MotionEvent.ACTION_MOVE -> {
                    if (params == null) return@setOnTouchListener false
                    val deltaX = event.rawX - downRawX
                    val deltaY = event.rawY - downRawY
                    if (!dragging &&
                        (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop)
                    ) {
                        dragging = true
                    }
                    if (dragging) {
                        params.x = startX + deltaX.toInt()
                        params.y = maxOf(startY + deltaY.toInt(), 0)
                        runCatching { windowManager?.updateViewLayout(target.rootView, params) }
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        FloatingLyricsPlacement.remember(context, params?.x ?: 0, params?.y ?: 0)
                    } else {
                        target.performClick()
                        onOpenLyrics()
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        FloatingLyricsPlacement.remember(context, params?.x ?: 0, params?.y ?: 0)
                    }
                }
            }
            true
        }
    }

    private companion object {
        const val BASE_FLAGS =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
    }
}
