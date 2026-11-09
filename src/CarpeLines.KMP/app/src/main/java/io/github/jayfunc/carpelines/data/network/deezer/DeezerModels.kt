package io.github.jayfunc.carpelines.data.network.deezer

import com.google.gson.annotations.SerializedName

/** The artist credited on a Deezer track result. */
data class DeezerArtist(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
)

/** Response from Deezer track search. */
data class DeezerTrackSearchResponse(
    @SerializedName("data") val data: List<DeezerTrack> = emptyList()
)

/** One track result; its album carries the cover sizes the cover-source chain needs. */
data class DeezerTrack(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: DeezerArtist? = null,
    @SerializedName("album") val album: DeezerAlbum? = null
)

/** The album a [DeezerTrack] belongs to. */
data class DeezerAlbum(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String? = null,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("cover_small") val coverSmall: String? = null,
    @SerializedName("cover_medium") val coverMedium: String? = null,
    @SerializedName("cover_big") val coverBig: String? = null,
    @SerializedName("cover_xl") val coverXl: String? = null
)

