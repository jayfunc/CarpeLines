package io.github.jayfunc.carpelines.lyrics.parse

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.core.parser.AutoParser
import com.mocharealm.accompanist.lyrics.core.parser.EnhancedLrcParser
import com.mocharealm.accompanist.lyrics.core.parser.ILyricsParser
import com.mocharealm.accompanist.lyrics.core.parser.KugouKrcParser
import com.mocharealm.accompanist.lyrics.core.parser.LyricifySyllableParser
import com.mocharealm.accompanist.lyrics.core.parser.NeteaseYrcParser
import com.mocharealm.accompanist.lyrics.core.parser.TTMLParser

/**
 * Facade over accompanist-lyrics-core. The [AutoParser] is extended with adapters for payloads the
 * core parsers recognise only in their strict spelling: LRC timestamps with one-digit fields and
 * QQ Music QRC (not shipped by the core library).
 */
object CoreLyricsParser {

    private val parser = AutoParser(
        parsers = listOf(
            LenientTtmlParser,
            QrcCoreParser,
            LenientLrcParser,
            LyricifySyllableParser,
            KugouKrcParser,
            NeteaseYrcParser,
        ),
    )

    fun canParse(raw: String): Boolean = parser.canParse(raw)

    fun parse(raw: String): SyncedLyrics {
        if (raw.isBlank()) return SyncedLyrics(emptyList())
        // Malformed payloads must not crash playback; callers fall back to plain text.
        return runCatching { parser.parse(raw) }.getOrElse { SyncedLyrics(emptyList()) }
    }
}

/** TTML files in the wild often omit the namespace the core detector requires. */
private object LenientTtmlParser : ILyricsParser {

    private val PREFIX = Regex("""<\s*(?:\w+:)?(?:tt|ttml|smil)\b""", RegexOption.IGNORE_CASE)
    private val PARAGRAPH_TAG = Regex("""<(?:[\w]+:)?p\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val PARAGRAPH_CLOSE = Regex("""</(?:[\w]+:)?p>""", RegexOption.IGNORE_CASE)
    private val END_ATTR = Regex("""\bend\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val BEGIN_ATTR = Regex("""\bbegin\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    override fun canParse(content: String): Boolean =
        content.contains("http://www.w3.org/ns/ttml", ignoreCase = true) ||
            PREFIX.containsMatchIn(content)

    override fun parse(content: String): SyncedLyrics =
        TTMLParser().parse(addMissingParagraphEnds(stripElementNamespacePrefixes(content)))

    /**
     * The core TTML parser matches element names literally (`p`, `metadata`), so a document using
     * `<tt:p>` would lose every paragraph. Strip prefixes from element names only; attributes such
     * as `ttm:agent` keep theirs.
     */
    private val PREFIXED_ELEMENT = Regex("""<(/?)([\w.-]+):([\w.-]+)(?=[\s/>])""")

    private fun stripElementNamespacePrefixes(content: String): String =
        PREFIXED_ELEMENT.replace(content) { match ->
            "<" + match.groupValues[1] + match.groupValues[3]
        }

    /**
     * The core TTML parser ignores a paragraph without an explicit `end`. Apple/AMLL exports
     * sometimes put the end only on the child spans (or omit it entirely), so fill it in from the
     * last child end or the next paragraph's begin.
     */
    private fun addMissingParagraphEnds(raw: String): String {
        val paragraphs = PARAGRAPH_TAG.findAll(raw).toList()
        if (paragraphs.isEmpty()) return raw

        val output = StringBuilder()
        var cursor = 0
        paragraphs.forEachIndexed { index, match ->
            output.append(raw, cursor, match.range.first)
            val tag = match.value
            val selfClosing = tag.trimEnd().endsWith("/>")
            if (END_ATTR.containsMatchIn(tag) || selfClosing) {
                output.append(tag)
            } else {
                val bodyStart = match.range.last + 1
                val bodyEnd = PARAGRAPH_CLOSE.find(raw, bodyStart)?.range?.first ?: raw.length
                val body = raw.substring(bodyStart, bodyEnd)
                val endValue = END_ATTR.findAll(body).lastOrNull()?.groupValues?.get(1)
                    ?: paragraphs.getOrNull(index + 1)
                        ?.let { BEGIN_ATTR.find(it.value)?.groupValues?.get(1) }
                    ?: defaultEndFor(tag)
                if (endValue != null) {
                    output.append(tag.dropLast(1))
                        .append(" end=\"")
                        .append(endValue)
                        .append("\">")
                } else {
                    output.append(tag)
                }
            }
            cursor = match.range.last + 1
        }
        output.append(raw, cursor, raw.length)
        return output.toString()
    }

    /** Last resort for a final paragraph without an end: four seconds after its begin. */
    private fun defaultEndFor(tag: String): String? {
        val begin = BEGIN_ATTR.find(tag)?.groupValues?.get(1) ?: return null
        val parts = begin.split(':')
        val seconds = when (parts.size) {
            1 -> parts[0].toDoubleOrNull()
            2 -> parts[0].toDoubleOrNull()?.times(60)?.plus(parts[1].toDoubleOrNull() ?: return null)
            3 -> parts[0].toDoubleOrNull()?.times(3600)
                ?.plus((parts[1].toDoubleOrNull() ?: return null) * 60)
                ?.plus(parts[2].toDoubleOrNull() ?: return null)
            else -> null
        } ?: return null
        val totalMs = (seconds * 1_000).toInt().coerceAtLeast(0) + 4_000
        val minutes = totalMs / 60_000
        val secs = (totalMs % 60_000) / 1_000
        val millis = totalMs % 1_000
        return minutes.toString().padStart(2, '0') + ":" +
            secs.toString().padStart(2, '0') + "." +
            millis.toString().padStart(3, '0')
    }
}

/**
 * QQ Music QRC. Each line is `[lineStartMs,lineDurationMs]text(wordStartMs,wordDurationMs)…`;
 * a syllable's text precedes its tuple and both timestamps are absolute milliseconds. KRC/YRC
 * share the line header but use three-value word tuples, so only that shape is claimed here.
 */
private object QrcCoreParser : ILyricsParser {

    private val LINE = Regex("""^\[(\d{1,9}),(\d{1,9})](.*)$""")
    private val WORD = Regex("""\((\d{1,9}),(\d{1,9})\)""")
    private val VOICE = Regex("""^\s*v(\d{1,4})\s*:\s*""", RegexOption.IGNORE_CASE)

    override fun canParse(content: String): Boolean =
        content.lineSequence().any { rawLine ->
            val match = LINE.matchEntire(rawLine.trim()) ?: return@any false
            WORD.containsMatchIn(match.groupValues[3])
        }

    override fun parse(content: String): SyncedLyrics {
        val lines = mutableListOf<com.mocharealm.accompanist.lyrics.core.model.ISyncedLine>()

        content.removePrefix("\uFEFF").lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            val match = LINE.matchEntire(line) ?: return@forEach
            val lineStart = match.groupValues[1].toIntOrNull() ?: return@forEach
            val lineDuration = match.groupValues[2].toIntOrNull() ?: 0
            val lineEnd = (lineStart + lineDuration).coerceAtLeast(lineStart)
            val body = match.groupValues[3]

            val voiceMatch = VOICE.find(body)
            val voice = voiceMatch?.groupValues?.get(1)?.toIntOrNull()
            val contentBody = if (voiceMatch != null) {
                body.substring(voiceMatch.range.last + 1)
            } else {
                body
            }

            val tuples = WORD.findAll(contentBody).toList()
            if (tuples.isEmpty()) {
                val text = contentBody.trim()
                if (text.isNotEmpty()) {
                    lines += SyncedLine(
                        content = text,
                        translation = null,
                        start = lineStart,
                        end = lineEnd,
                    )
                }
                return@forEach
            }

            val syllables = mutableListOf<KaraokeSyllable>()
            tuples.forEachIndexed { index, tuple ->
                val start = tuple.groupValues[1].toIntOrNull() ?: return@forEachIndexed
                val duration = tuple.groupValues[2].toIntOrNull() ?: 0
                val textStart = tuples.getOrNull(index - 1)?.range?.last?.plus(1) ?: 0
                val text = contentBody.substring(textStart, tuple.range.first).replace("\uFEFF", "")
                if (text.isEmpty()) return@forEachIndexed

                val end = (start + duration).takeIf { it > start }
                    ?: tuples.getOrNull(index + 1)
                        ?.groupValues?.get(1)?.toIntOrNull()
                    ?: lineEnd.coerceAtLeast(start + 1)
                syllables += KaraokeSyllable(
                    content = text,
                    start = start,
                    end = end,
                )
            }

            if (syllables.isEmpty()) return@forEach
            lines += KaraokeLine.MainKaraokeLine(
                syllables = syllables,
                translation = null,
                alignment = alignmentOf(voice),
                start = lineStart,
                end = maxOf(syllables.last().end, lineEnd),
            )
        }

        return SyncedLyrics(lines)
    }

    private fun alignmentOf(voice: Int?): KaraokeAlignment = when (voice) {
        1 -> KaraokeAlignment.Start
        2 -> KaraokeAlignment.End
        else -> KaraokeAlignment.Unspecified
    }
}

/**
 * Normalises timestamp spellings the core Enhanced-LRC detector rejects, then defers to the core
 * parser: `[0:5.029]`, `[00:03]`, `[00:10:50]` and `<00:01.0>` all become `mm:ss.SSS`.
 */
private object LenientLrcParser : ILyricsParser {

    private val TIMESTAMP = Regex("""[<\[](\d{1,4}):(\d{1,2})(?:[.:](\d{1,6}))?[>\]]""")
    private val DETECT = Regex("""[<\[]\d{1,4}:\d{1,2}(?:[.:]\d{1,6})?[>\]]""")
    private val NORMALIZED_LINE_TAGS = Regex("""^((?:\[\d{1,4}:\d{1,2}\.\d{1,3}])+)(.*)$""")
    private val WORD_TAG = Regex("""[<\[]\d{1,4}:\d{1,2}\.\d{1,3}[>\]]""")
    private val INLINE_BRACKET_TAG = Regex("""\[(\d{1,4}:\d{1,2}\.\d{1,3})]""")
    private val VOICE_PREFIX = Regex("""^v\d{1,4}\s*:\s*""", RegexOption.IGNORE_CASE)

    override fun canParse(content: String): Boolean = DETECT.containsMatchIn(content)

    private data class TimedBlock(
        val start: Int,
        val lines: List<String>,
    )

    override fun parse(content: String): SyncedLyrics {
        val normalized = normalizeTimestamps(content)
        val blocks = sortTimedBlocks(mergeContinuationLines(normalized))
            .map { block ->
                TimedBlock(
                    start = block.start,
                    lines = block.lines
                        .map { rewriteLegacyTag(it, block.start) }
                        .map(::ensureLeadingSyllableTag),
                )
            }

        // Parse each group separately: the core parser folds any line at the same time into a
        // translation, which would consume a sung line that happens to share a backing-only
        // line's timestamp. Groups keep only real translations together.
        val parsedLines = groupBlocks(blocks).flatMap { group ->
            EnhancedLrcParser.parse(group.flatMap { it.lines }.joinToString("\n")).lines
        }
        return SyncedLyrics(lines = closeLineEnds(parsedLines.sortedBy { it.start }))
    }

    private fun normalizeTimestamps(raw: String): String {
        val sanitized = raw.removePrefix("\uFEFF")
            .lineSequence()
            .map { rawLine ->
                val line = rawLine.trim()
                    .filterNot { it.category == CharCategory.FORMAT }
                val firstBracket = line.indexOf('[')
                if (firstBracket > 0) line.substring(firstBracket) else line
            }
            .joinToString("\n")

        return TIMESTAMP.replace(sanitized) { match ->
            val minutes = match.groupValues[1].toIntOrNull() ?: 0
            val seconds = match.groupValues[2].toIntOrNull() ?: 0
            val fraction = match.groupValues[3]
            val millis = when {
                fraction.isEmpty() -> 0
                fraction.length >= 3 -> fraction.take(3).toIntOrNull() ?: 0
                fraction.length == 2 -> (fraction.toIntOrNull() ?: 0) * 10
                else -> (fraction.toIntOrNull() ?: 0) * 100
            }
            val timestamp = minutes.toString().padStart(2, '0') + ":" +
                seconds.toString().padStart(2, '0') + "." +
                millis.toString().padStart(3, '0')
            match.groupValues[0].first() + timestamp + match.groupValues[0].last()
        }
    }

    /**
     * The core parser only understands timed background spans and has no `[tr:]` tag. The app
     * writes both forms when saving enhanced LRC, so translate them back into timed lines the
     * core parser can consume:
     * - `[bg:plain text]` becomes `[bg:<time>plain text]`;
     * - `[tr:text]` becomes a line at the block's time (core pairs it as a translation).
     */
    private fun rewriteLegacyTag(line: String, blockStart: Int): String = when {
        line.startsWith("[bg:", ignoreCase = true) -> {
            val inner = line.substring(4, line.length - 1)
            if (WORD_TAG.containsMatchIn(inner)) {
                line
            } else {
                "[bg:<${blockStart.toLrcTimestamp()}>$inner]"
            }
        }
        line.startsWith("[tr:", ignoreCase = true) -> {
            val text = line.substring(4, line.length - 1)
            "[${blockStart.toLrcTimestamp()}]$text"
        }
        else -> line
    }

    /**
     * The core parser assigns a line's end from the next line in file order and never sorts, so
     * an out-of-order file would make it construct a line ending before it starts. Sort timed
     * blocks (a line plus its `[bg:]`/`[tr:]` followers) and expand repeated line timestamps.
     */
    private fun sortTimedBlocks(raw: String): List<TimedBlock> {
        val blocks = mutableListOf<TimedBlock>()
        raw.lineSequence().forEach { line ->
            if (line.isBlank()) return@forEach
            val timed = NORMALIZED_LINE_TAGS.matchEntire(line)
            when {
                timed != null -> {
                    val tags = INLINE_BRACKET_TAG.findAll(timed.groupValues[1]).toList()
                    val body = timed.groupValues[2]
                    tags.forEach { tag ->
                        val time = tag.groupValues[1].toLrcMillis()
                        if (time != null) {
                            blocks += TimedBlock(time, mutableListOf("[${tag.groupValues[1]}]$body"))
                        }
                    }
                }
                line.startsWith("[bg:", ignoreCase = true) ||
                    line.startsWith("[tr:", ignoreCase = true) ->
                    if (blocks.isNotEmpty()) {
                        val last = blocks.last()
                        blocks[blocks.lastIndex] = last.copy(lines = last.lines + line)
                    } else if (line.startsWith("[bg:", ignoreCase = true)) {
                        blocks += TimedBlock(
                            start = INLINE_BRACKET_TAG.find(line)
                                ?.groupValues?.get(1)?.toLrcMillis() ?: 0,
                            lines = mutableListOf(line),
                        )
                    }
                line.startsWith("[") -> Unit
                blocks.isNotEmpty() -> {
                    val last = blocks.last()
                    blocks[blocks.lastIndex] = last.copy(lines = last.lines + line)
                }
            }
        }
        return blocks.sortedBy { it.start }
    }

    private fun groupBlocks(blocks: List<TimedBlock>): List<List<TimedBlock>> {
        val groups = mutableListOf<MutableList<TimedBlock>>()
        for (block in blocks) {
            val current = groups.lastOrNull()
            val canJoin = current != null &&
                current.last().start == block.start &&
                !isBackgroundOnly(current.last()) &&
                !isBackgroundOnly(block)
            if (canJoin) {
                current += block
            } else {
                groups += mutableListOf(block)
            }
        }
        return groups
    }

    private fun isBackgroundOnly(block: TimedBlock): Boolean {
        val timed = block.lines.firstOrNull { NORMALIZED_LINE_TAGS.matchEntire(it) != null }
        val hasBackground = block.lines.any { it.startsWith("[bg:", ignoreCase = true) }
        if (timed == null) return hasBackground
        val rawBody = NORMALIZED_LINE_TAGS.matchEntire(timed)
            ?.groupValues
            ?.get(2)
            .orEmpty()
        val voice = VOICE_PREFIX.find(rawBody)?.value.orEmpty()
        return rawBody.removePrefix(voice).isBlank() && hasBackground
    }

    private fun closeLineEnds(lines: List<ISyncedLine>): List<ISyncedLine> =
        lines.mapIndexed { index, line ->
            if (line is SyncedLine && (line.end == Int.MAX_VALUE || line.end <= line.start)) {
                val nextStart = lines.getOrNull(index + 1)?.start
                if (nextStart != null && nextStart > line.start) line.copy(end = nextStart) else line
            } else {
                line
            }
        }

    private fun Int.toLrcTimestamp(): String {
        val minutes = this / 60_000
        val seconds = (this % 60_000) / 1_000
        val millis = this % 1_000
        return minutes.toString().padStart(2, '0') + ":" +
            seconds.toString().padStart(2, '0') + "." +
            millis.toString().padStart(3, '0')
    }

    private fun String.toLrcMillis(): Int? {
        val parts = split(':')
        if (parts.size != 2) return null
        val minutes = parts[0].toIntOrNull() ?: return null
        val secondsAndFraction = parts[1].split('.')
        val seconds = secondsAndFraction.getOrNull(0)?.toIntOrNull() ?: return null
        val millis = secondsAndFraction.getOrNull(1)
            ?.padEnd(3, '0')
            ?.take(3)
            ?.toIntOrNull()
            ?: 0
        return minutes * 60_000 + seconds * 1_000 + millis
    }

    /**
     * Untagged lines after the first timed line are continuations of it in many LRC exports.
     * U+2028 keeps them in one parser line while still rendering as a line break.
     */
    private fun mergeContinuationLines(raw: String): String {
        val output = mutableListOf<String>()
        var hasTimedLine = false
        raw.lineSequence().forEach { line ->
            if (line.isBlank()) return@forEach
            when {
                NORMALIZED_LINE_TAGS.matchEntire(line) != null -> {
                    hasTimedLine = true
                    output += line
                }
                line.startsWith("[") -> output += line
                hasTimedLine && output.isNotEmpty() ->
                    output[output.lastIndex] = output.last() + "\u0001" + line
                else -> output += line
            }
        }
        return output.joinToString("\n")
    }

    /**
     * The core syllable parser only captures text that follows a word tag, so text before the
     * first tag (`[00:10.00]Hello <00:10.50>world`) would disappear. Mirror the line timestamp as
     * a leading syllable tag to keep that text and its own timing.
     */
    private fun ensureLeadingSyllableTag(raw: String): String =
        raw.lineSequence().joinToString("\n") { line ->
            val match = NORMALIZED_LINE_TAGS.matchEntire(line) ?: return@joinToString line
            val leadingTags = match.groupValues[1]
            if (match.groupValues[2].isBlank()) return@joinToString line

            // Inline `[mm:ss.xxx]` tags are word timings, not line timings; the core parser would
            // otherwise swallow them as extra line tags.
            val body = INLINE_BRACKET_TAG.replace(
                match.groupValues[2]
                    .substringBefore("\\n")
                    .substringBefore("\\r"),
            ) { tag ->
                "<${tag.groupValues[1]}>"
            }
            val voice = VOICE_PREFIX.find(body)?.value.orEmpty()
            val content = body.removePrefix(voice)
            val firstTag = WORD_TAG.find(content) ?: return@joinToString line
            if (content.substring(0, firstTag.range.first).isBlank()) {
                return@joinToString leadingTags + body
            }

            val firstLineTag = INLINE_BRACKET_TAG.find(leadingTags)
                ?.groupValues
                ?.get(1)
                ?: return@joinToString line
            leadingTags + voice + "<$firstLineTag>" + content
        }
}

/** Converts the core model into the plain timed lines the rest of the shared pipeline uses. */
object CoreLyricsMapper {

    fun toLines(
        lyrics: SyncedLyrics,
        insertMissingLatinSpaces: Boolean = false,
    ): List<io.github.jayfunc.carpelines.lyrics.model.LyricsLine> =
        lyrics.lines.sortedBy { it.start }.mapNotNull { line ->
            when (line) {
                is KaraokeLine.MainKaraokeLine -> {
                    val syllables = line.syllables
                        .toLyricsSyllables(insertMissingLatinSpaces)
                    val text = syllables.joinToString(separator = "") { it.text }
                        .replace('\u0001', '\n')
                        .replace('\u2028', '\n')
                        .trim()
                    val accompaniment = line.accompanimentLines.orEmpty()
                    val backgroundSyllables = accompaniment
                        .flatMap { it.syllables.toLyricsSyllables(insertMissingLatinSpaces) }
                    val backgroundText = backgroundSyllables
                        .joinToString(separator = "") { it.text }
                        .replace('\u0001', '\n')
                        .replace('\u2028', '\n')
                        .trim()
                        .takeIf { it.isNotEmpty() }
                    io.github.jayfunc.carpelines.lyrics.model.LyricsLine(
                        startMs = line.start.toLong(),
                        endMs = line.end.toLong().takeIf { it > line.start },
                        text = text,
                        syllables = syllables,
                        hasRealSyllableTiming = syllables.isNotEmpty(),
                        voice = voiceOf(line.alignment),
                        backgroundText = backgroundText,
                        backgroundSyllables = backgroundSyllables,
                        translation = line.translation,
                        romanization = line.phonetic,
                    )
                }

                is KaraokeLine.AccompanimentKaraokeLine -> {
                    val syllables = line.syllables.toLyricsSyllables(insertMissingLatinSpaces)
                    val text = syllables.joinToString(separator = "") { it.text }.trim()
                    io.github.jayfunc.carpelines.lyrics.model.LyricsLine(
                        startMs = line.start.toLong(),
                        endMs = line.end.toLong().takeIf { it > line.start },
                        text = "",
                        backgroundText = text,
                        backgroundSyllables = syllables,
                        translation = line.translation,
                        romanization = line.phonetic,
                    )
                }

                is SyncedLine -> {
                    io.github.jayfunc.carpelines.lyrics.model.LyricsLine(
                        startMs = line.start.toLong(),
                        endMs = line.end.toLong().takeIf {
                            it > line.start && it != Int.MAX_VALUE.toLong()
                        },
                        text = line.content.replace('\u0001', '\n').replace('\u2028', '\n'),
                        translation = line.translation,
                        romanization = line.phonetic,
                    )
                }

                else -> null
            }
        }

    private fun List<KaraokeSyllable>.toLyricsSyllables(
        insertMissingLatinSpaces: Boolean,
    ): List<io.github.jayfunc.carpelines.lyrics.model.LyricsSyllable> {
        // A tone mark split from its letter cannot be drawn on its own, so fold it back into the
        // syllable it belongs to.
        val merged = mutableListOf<Triple<String, Int, Int>>()
        forEach { syllable ->
            val content = syllable.content
            if (content.isNotEmpty() && content.first().isCombiningMark() && merged.isNotEmpty()) {
                val previous = merged.last()
                merged[merged.lastIndex] = Triple(
                    previous.first + content,
                    previous.second,
                    maxOf(previous.third, syllable.end),
                )
            } else {
                merged += Triple(content, syllable.start, syllable.end)
            }
        }

        var index = 0
        var previousContent: String? = null
        return merged.map { (rawContent, start, end) ->
            val content = if (
                insertMissingLatinSpaces &&
                previousContent != null &&
                needsVisibleSpace(previousContent, rawContent)
            ) {
                " " + rawContent
            } else {
                rawContent
            }
            val result = io.github.jayfunc.carpelines.lyrics.model.LyricsSyllable(
                text = content,
                startMs = start.toLong(),
                endMs = end.toLong().takeIf { it > start },
                startIndex = index,
            )
            index += content.length
            previousContent = content
            result
        }
    }

    private fun Char.isCombiningMark(): Boolean = when (category) {
        CharCategory.NON_SPACING_MARK,
        CharCategory.ENCLOSING_MARK,
        CharCategory.COMBINING_SPACING_MARK -> true
        else -> false
    }

    /**
     * Kugou/KRC producers sometimes omit the space between timed Latin tokens. Only visible-space
     * scripts get one back; CJK runs stay gapless.
     */
    private fun needsVisibleSpace(previous: String, current: String): Boolean {
        val previousChar = previous.lastOrNull { !it.isWhitespace() } ?: return false
        val currentChar = current.firstOrNull { !it.isWhitespace() } ?: return false
        if (current.first().isWhitespace() || previous.last().isWhitespace()) return false
        if (!previousChar.isLetterOrDigit() || !currentChar.isLetterOrDigit()) return false
        return usesVisibleInterWordSpacing(previousChar) && usesVisibleInterWordSpacing(currentChar)
    }

    private fun usesVisibleInterWordSpacing(char: Char): Boolean {
        val code = char.code
        return when {
            code in 0x2E80..0x9FFF -> false      // CJK radicals and unified ideographs
            code in 0xAC00..0xD7AF -> false      // Hangul syllables
            code in 0xF900..0xFAFF -> false      // CJK compatibility ideographs
            code in 0xFE30..0xFE4F -> false      // CJK compatibility forms
            code in 0xFF00..0xFFEF -> false      // halfwidth and fullwidth forms
            code in 0x3000..0x303F -> false      // CJK symbols and punctuation
            code in 0x0E00..0x0E7F -> false      // Thai
            code in 0x0E80..0x0EFF -> false      // Lao
            code in 0x1780..0x17FF -> false      // Khmer
            code in 0x1000..0x109F -> false      // Myanmar
            else -> true
        }
    }

    private fun voiceOf(alignment: KaraokeAlignment): Int? = when (alignment) {
        KaraokeAlignment.Start -> 1
        KaraokeAlignment.End -> 2
        KaraokeAlignment.Unspecified -> null
    }
}
