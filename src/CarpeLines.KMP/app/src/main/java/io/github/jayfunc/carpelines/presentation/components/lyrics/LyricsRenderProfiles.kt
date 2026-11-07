package io.github.jayfunc.carpelines.presentation.components.lyrics

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.ui.preparation.MeasuredLyricsLine
import com.mocharealm.accompanist.lyrics.ui.profile.DefaultLyricsProfiles
import com.mocharealm.accompanist.lyrics.ui.profile.LyricsProfile
import com.mocharealm.accompanist.lyrics.ui.profile.ProfileGroupEffects
import com.mocharealm.accompanist.lyrics.ui.profile.ProfileTextUnit

/**
 * The `renderProfiles` list handed to `KaraokeLyricsView`.
 *
 * The library's script-aware profiles are always used: CJK animates by grapheme and Latin by
 * syllable, which is what the original "auto" option did. The three focus-effect switches override
 * the per-script `ProfileGroupEffects` policy: the library decides *when* a long syllable should
 * glow, swell or float, and these switches only let the user turn that decision off again.
 *
 * When nothing is overridden the untouched default list is returned, so the renderer keeps its
 * identity-based fast path.
 */
fun lyricsRenderProfiles(
    scaleEffectEnabled: Boolean,
    glowEffectEnabled: Boolean,
    floatEffectEnabled: Boolean,
): List<LyricsProfile> {
    if (scaleEffectEnabled && glowEffectEnabled && floatEffectEnabled) {
        return DefaultLyricsProfiles
    }
    return DefaultLyricsProfiles.map { delegate ->
        EffectTunedLyricsProfile(
            delegate = delegate,
            scale = scaleEffectEnabled,
            glow = glowEffectEnabled,
            lift = floatEffectEnabled,
        )
    }
}

/**
 * Delegates every preparation and drawing step to the library profile and only rewrites the
 * effect policy, so script-specific shaping, wrapping and glyph protection stay untouched.
 */
private class EffectTunedLyricsProfile(
    private val delegate: LyricsProfile,
    private val scale: Boolean,
    private val glow: Boolean,
    private val lift: Boolean,
) : LyricsProfile {

    override fun matches(syllable: KaraokeSyllable): Boolean = delegate.matches(syllable)

    override fun groups(syllables: List<KaraokeSyllable>): List<List<KaraokeSyllable>> =
        delegate.groups(syllables)

    override fun prepare(
        group: List<KaraokeSyllable>,
        measurer: TextMeasurer,
        style: TextStyle,
    ): List<ProfileTextUnit> = delegate.prepare(group, measurer, style)

    override fun prepare(
        line: MeasuredLyricsLine,
        group: List<KaraokeSyllable>,
        measurer: TextMeasurer,
        style: TextStyle,
    ): List<ProfileTextUnit> = delegate.prepare(line, group, measurer, style)

    override fun effects(
        units: List<ProfileTextUnit>,
        accompaniment: Boolean,
    ): ProfileGroupEffects {
        val base = delegate.effects(units, accompaniment)
        return base.copy(
            scale = base.scale && scale,
            glow = base.glow && glow,
            lift = base.lift && lift,
        )
    }

    override fun wrap(
        units: List<ProfileTextUnit>,
        measurer: TextMeasurer,
        maxWidth: Float,
    ): List<List<ProfileTextUnit>> = delegate.wrap(units, measurer, maxWidth)

    override fun combine(units: List<ProfileTextUnit>): ProfileTextUnit? = delegate.combine(units)

    override fun DrawScope.draw(unit: ProfileTextUnit, color: Color, shadow: Shadow) {
        with(delegate) { draw(unit, color, shadow) }
    }
}
