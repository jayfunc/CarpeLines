package io.github.jayfunc.carpelines.ui.glancewidget

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.IOException
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Snapshot rendered by every home-screen widget.
 *
 * Widgets follow the system media session, which is the only playback CarpeLines exposes. The
 * current lyric line is resolved off the main thread and stored with the position that produced
 * it, so Glance layouts stay simple and do not need to parse lyrics themselves.
 */
@Serializable
data class WidgetMediaState(
    val hasSession: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val appLabel: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val artworkUri: String? = null,
    val lyricLine: String = "",
    val lyricTranslation: String = "",
)

object WidgetMediaStateDefinition : GlanceStateDefinition<WidgetMediaState> {
    private const val DATASTORE_FILE_NAME = "carpeLinesWidgetState_v1_json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val Context.widgetStateDataStore: DataStore<WidgetMediaState> by dataStore(
        fileName = DATASTORE_FILE_NAME,
        serializer = WidgetMediaStateSerializer(json),
    )

    override suspend fun getDataStore(
        context: Context,
        fileKey: String,
    ): DataStore<WidgetMediaState> = context.widgetStateDataStore

    override fun getLocation(context: Context, fileKey: String): File =
        File(context.filesDir, "datastore/$DATASTORE_FILE_NAME")
}

class WidgetMediaStateSerializer(
    private val json: Json,
) : Serializer<WidgetMediaState> {
    override val defaultValue: WidgetMediaState = WidgetMediaState()

    override suspend fun readFrom(input: InputStream): WidgetMediaState {
        try {
            val encoded = input.bufferedReader().use { it.readText() }
            if (encoded.isBlank()) return defaultValue
            return json.decodeFromString(encoded)
        } catch (exception: SerializationException) {
            throw CorruptionException("Cannot read widget state.", exception)
        } catch (exception: IOException) {
            throw CorruptionException("Cannot read widget state due to IO.", exception)
        }
    }

    override suspend fun writeTo(t: WidgetMediaState, output: OutputStream) {
        output.bufferedWriter().use { it.write(json.encodeToString(t)) }
    }
}
