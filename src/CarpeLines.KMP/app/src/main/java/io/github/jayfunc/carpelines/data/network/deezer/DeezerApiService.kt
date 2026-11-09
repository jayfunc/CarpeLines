package io.github.jayfunc.carpelines.data.network.deezer

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Deezer API.
 * Used by the cover-source chain to find the album cover of a playing track.
 */
interface DeezerApiService {

    /**
     * Search for a track by title and artist. Used by the cover-source chain to find the album
     * cover Deezer has for this recording.
     */
    @GET("search/track")
    suspend fun searchTrack(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5
    ): DeezerTrackSearchResponse
}
