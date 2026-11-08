package io.github.jayfunc.carpelines.presentation.floatinglyrics

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.android.material.card.MaterialCardView
import io.github.jayfunc.carpelines.R
import kotlin.math.abs
import timber.log.Timber

/**
 * The M3E roles the floating window draws with. Floating lyrics live outside the app's Compose
 * tree, so the album-art scheme resolved by the service is handed over as plain ARGB values.
 */
internal data class FloatingLyricsColors(
    val container: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val accent: Int,
)

/**
 * Owns the floating lyric window: inflation, placement, dragging and content updates.
 *
 * The window is a plain overlay view rather than a Compose window so it can live in a service
 * without a lifecycle owner, and it uses the app's Material 3 theme so the tonal container and
 * rounded corners match the in-app M3E surfaces. Its colours are tinted per track from the album
 * artwork palette; without artwork the app-theme defaults in colors.xml stay in place.
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
    private val themedContext =
        androidx.appcompat.view.ContextThemeWrapper(context, R.style.Theme_CarpeLines)

    private var root: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var cardView: MaterialCardView? = null
    private var currentLineView: TextView? = null
    private var secondaryLineView: TextView? = null
    private var previousButton: ImageButton? = null
    private var playPauseButton: ImageButton? = null
    private var nextButton: ImageButton? = null
    private var lockButton: ImageButton? = null
    private var closeButton: ImageButton? = null
    private var controlsView: View? = null
    private var lockBadge: View? = null
    private var lockBadgeCard: MaterialCardView? = null
    private var lockBadgeButton: ImageButton? = null

    /** Album-art palette in use; null means the app-theme defaults from colors.xml. */
    private var colors: FloatingLyricsColors? = null

    private val defaultColors: FloatingLyricsColors by lazy {
        FloatingLyricsColors(
            container = ContextCompat.getColor(context, R.color.floating_lyrics_background),
            textPrimary = ContextCompat.getColor(context, R.color.floating_lyrics_text_primary),
            textSecondary = ContextCompat.getColor(context, R.color.floating_lyrics_text_secondary),
            accent = ContextCompat.getColor(context, R.color.floating_lyrics_accent),
        )
    }

    /** Locked windows ignore touches so they cannot be moved or closed by accident. */
    var isLocked: Boolean = false
        private set

    val isAttached: Boolean get() = root != null

    /** Whether the window is actually drawn; a hidden-by-policy window reports false. */
    val isShown: Boolean get() = root?.isShown == true

    /** Adds the window when it is not attached yet. */
    fun show() {
        if (root == null) attachWindow()
    }

    fun hide() {
        hideLockBadge()
        val view = root ?: return
        runCatching { windowManager?.removeView(view) }
            .onFailure { Timber.w(it, "Could not remove the floating lyrics window") }
        root = null
        layoutParams = null
        cardView = null
        currentLineView = null
        secondaryLineView = null
        previousButton = null
        playPauseButton = null
        nextButton = null
        lockButton = null
        closeButton = null
        controlsView = null
    }

    /**
     * Paints the window with the album-art palette; null restores the app-theme defaults. The
     * colours are kept while the window is detached, so a re-attach comes back already tinted.
     */
    fun applyColors(value: FloatingLyricsColors?) {
        colors = value
        applyColorsToViews()
    }

    private fun applyColorsToViews() {
        val active = colors ?: defaultColors
        cardView?.setCardBackgroundColor(active.container)
        currentLineView?.setTextColor(active.textPrimary)
        secondaryLineView?.setTextColor(active.textSecondary)
        playPauseButton?.imageTintList = ColorStateList.valueOf(active.accent)
        previousButton?.imageTintList = ColorStateList.valueOf(active.textSecondary)
        nextButton?.imageTintList = ColorStateList.valueOf(active.textSecondary)
        lockButton?.imageTintList = ColorStateList.valueOf(active.textSecondary)
        closeButton?.imageTintList = ColorStateList.valueOf(active.textSecondary)
        lockBadgeCard?.setCardBackgroundColor(active.container)
        lockBadgeButton?.imageTintList = ColorStateList.valueOf(active.textSecondary)
    }

    /**
     * Toggles the locked state. While locked the lyric window becomes touch-transparent and
     * swaps its controls for a small lock badge, which is the only tappable surface and unlocks
     * the window again.
     */
    fun setLocked(locked: Boolean) {
        if (isLocked == locked) return
        isLocked = locked
        applyLockState()
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
        val view = LayoutInflater.from(themedContext).inflate(R.layout.floating_lyrics_window, null)
        // MIUI sometimes removes the overlay when the keyguard takes over during an unlock
        // animation. Forget the stale root so the next sync can attach a fresh window.
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit

            override fun onViewDetachedFromWindow(v: View) {
                Timber.d("FloatingLyricsOverlay: detached")
                if (root === v) {
                    hideLockBadge()
                    root = null
                    layoutParams = null
                    cardView = null
                    currentLineView = null
                    secondaryLineView = null
                    previousButton = null
                    playPauseButton = null
                    nextButton = null
                    lockButton = null
                    closeButton = null
                    controlsView = null
                }
            }
        })
        cardView = view.findViewById(R.id.floating_lyrics_card)
        currentLineView = view.findViewById(R.id.floating_lyrics_current_line)
        secondaryLineView = view.findViewById(R.id.floating_lyrics_secondary_line)
        previousButton = view.findViewById(R.id.floating_lyrics_previous)
        playPauseButton = view.findViewById(R.id.floating_lyrics_play_pause)
        nextButton = view.findViewById(R.id.floating_lyrics_next)
        lockButton = view.findViewById(R.id.floating_lyrics_lock)
        closeButton = view.findViewById(R.id.floating_lyrics_close)
        controlsView = view.findViewById(R.id.floating_lyrics_controls)

        previousButton?.setOnClickListener { onPrevious() }
        playPauseButton?.setOnClickListener { onPlayPause() }
        nextButton?.setOnClickListener { onNext() }
        lockButton?.setOnClickListener { setLocked(true) }
        closeButton?.setOnClickListener { onClose() }
        attachDragAndTap(view.findViewById(R.id.floating_lyrics_text_area))

        val widthPx = FloatingLyricsPlacement.widthPx(context)

        val params = WindowManager.LayoutParams(
            widthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            windowFlags(),
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
            applyColorsToViews()
            controlsView?.visibility = if (isLocked) View.GONE else View.VISIBLE
            if (isLocked) showLockBadge()
            Timber.d("FloatingLyricsOverlay: attached")
        }
    }

    private fun windowFlags(): Int =
        if (isLocked) {
            BASE_FLAGS or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            BASE_FLAGS
        }

    private fun applyLockState() {
        controlsView?.visibility = if (isLocked) View.GONE else View.VISIBLE
        val view = root
        val params = layoutParams
        val updated = if (view != null && params != null) {
            params.flags = windowFlags()
            runCatching { windowManager?.updateViewLayout(view, params) }
                .onFailure { Timber.w(it, "Could not update the floating lyrics lock state") }
                .isSuccess
        } else {
            true
        }
        // Keep the badge around if the flag update failed; otherwise the window would stay
        // touch-transparent with no way to unlock it.
        if (isLocked) showLockBadge() else if (updated) hideLockBadge()
    }

    /**
     * The lock badge is a separate overlay window so it stays tappable after the lyric window
     * itself turns touch-transparent. It sits just above the top-right corner of the lyric card.
     */
    private fun showLockBadge() {
        if (lockBadge != null) return
        val manager = windowManager ?: return
        val params = layoutParams ?: return
        val badge = LayoutInflater.from(themedContext)
            .inflate(R.layout.floating_lyrics_lock_badge, null)
        lockBadgeButton = badge.findViewById<ImageButton>(R.id.floating_lyrics_lock_badge)
            .also { it.setOnClickListener { setLocked(false) } }
        lockBadgeCard = badge.findViewById(R.id.floating_lyrics_lock_badge_card)
        val size = context.dp(BADGE_SIZE_DP)
        val badgeParams = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            BASE_FLAGS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = params.x + params.width - size
            y = (params.y - size - context.dp(BADGE_GAP_DP)).coerceAtLeast(0)
        }
        val added = runCatching { manager.addView(badge, badgeParams) }
            .onFailure { Timber.w(it, "Could not show the floating lyrics lock badge") }
            .isSuccess
        if (added) {
            lockBadge = badge
            applyColorsToViews()
            Timber.d("FloatingLyricsOverlay: lock badge attached")
        } else {
            lockBadgeCard = null
            lockBadgeButton = null
        }
    }

    private fun hideLockBadge() {
        val badge = lockBadge ?: return
        runCatching { windowManager?.removeView(badge) }
            .onFailure { Timber.w(it, "Could not remove the floating lyrics lock badge") }
        lockBadge = null
        lockBadgeCard = null
        lockBadgeButton = null
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

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val BASE_FLAGS =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        const val BADGE_SIZE_DP = 36
        const val BADGE_GAP_DP = 6
    }
}
