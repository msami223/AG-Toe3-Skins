package com.devstormtech.toe3skins.api

import com.devstormtech.toe3skins.model.Skin
import com.devstormtech.toe3skins.model.Sticker
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface WordPressApi {
    // Get all skins
    @GET("wp-json/wp/v2/skins?per_page=100")
    fun getSkins(): Call<List<Skin>>

    // Get all stickers with embedded featured images
    @GET("wp-json/wp/v2/stickers?_embed&per_page=100")
    fun getStickers(): Call<List<Sticker>>

    // Add +1 to Download
    @POST("wp-json/toe3/v1/increment-download/{id}")
    fun incrementDownload(@Path("id") id: Int): Call<Void>

    // Add +1 to View
    @POST("wp-json/toe3/v1/increment-view/{id}")
    fun incrementView(@Path("id") id: Int): Call<Void>
}