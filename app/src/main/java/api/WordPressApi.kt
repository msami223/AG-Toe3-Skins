package com.devstormtech.toe3skins.api

import com.devstormtech.toe3skins.model.Skin
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface WordPressApi {
    // Get all skins
    @GET("wp-json/wp/v2/skins?per_page=100")
    fun getSkins(): Call<List<Skin>>

    // Add +1 to Download
    @POST("wp-json/toe3/v1/increment-download/{id}")
    fun incrementDownload(@Path("id") id: Int): Call<Void>

    // Add +1 to View
    @POST("wp-json/toe3/v1/increment-view/{id}")
    fun incrementView(@Path("id") id: Int): Call<Void>
}