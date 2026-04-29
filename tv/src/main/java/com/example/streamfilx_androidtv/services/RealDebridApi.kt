package com.example.streamfilx_androidtv.services

import com.example.streamfilx_androidtv.core.models.RDDownload
import com.example.streamfilx_androidtv.core.models.RDTorrent
import com.example.streamfilx_androidtv.core.models.RDTorrentAdded
import com.example.streamfilx_androidtv.core.models.RDUser
import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RealDebridApi {

    @GET("user")
    suspend fun getUser(): RDUser

    @FormUrlEncoded
    @POST("unrestrict/link")
    suspend fun unrestrictLink(
        @Field("link") link: String,
        @Field("password") password: String? = null,
    ): RDDownload

    // hashes = "hash1/hash2/hash3" — encoded=true so Retrofit doesn't double-encode the slashes
    @GET("torrents/instantAvailability/{hashes}")
    suspend fun checkInstantAvailability(
        @Path("hashes", encoded = true) hashes: String,
    ): JsonObject

    @FormUrlEncoded
    @POST("torrents/addMagnet")
    suspend fun addMagnet(@Field("magnet") magnet: String): RDTorrentAdded

    @FormUrlEncoded
    @POST("torrents/selectFiles/{id}")
    suspend fun selectFiles(
        @Path("id") id: String,
        @Field("files") files: String,
    ): ResponseBody

    @GET("torrents/info/{id}")
    suspend fun getTorrentInfo(@Path("id") id: String): RDTorrent

    @DELETE("torrents/delete/{id}")
    suspend fun deleteTorrent(@Path("id") id: String): ResponseBody

    @GET("torrents")
    suspend fun getTorrents(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
    ): List<RDTorrent>

    @GET("downloads")
    suspend fun getDownloads(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
    ): List<RDDownload>
}
