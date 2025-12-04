package com.devstormtech.toe3skins.api

import com.devstormtech.toe3skins.model.AcfData
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type

object RetrofitClient {
    // Your URL
    private const val BASE_URL = "https://wordpress-1436552-6036659.cloudwaysapps.com/"

    // This "Translator" fixes the WordPress [] vs {} bug
    private val gson = GsonBuilder()
        .registerTypeAdapter(AcfData::class.java, object : JsonDeserializer<AcfData> {
            override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): AcfData {
                // If WordPress sends an Array [] (which means empty), return a default empty object
                if (json.isJsonArray) {
                    return AcfData(
                        truckModel = "Unknown",
                        skinFileUrl = "",       // Matches skin_file_download
                        previewImage1 = "",     // Matches skin_image_main
                        previewImage2 = null,
                        previewImage3 = null,
                        creatorName = "Unknown",
                        downloadCount = 0,
                        viewCount = 0,
                        instructions = "",
                        fileSize = "",
                        tags = emptyList()
                    )
                }
                // Otherwise, parse it normally
                return GsonBuilder().create().fromJson(json, typeOfT)
            }
        })
        .create()

    val instance: WordPressApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson)) // Use our smart translator
            .build()
            .create(WordPressApi::class.java)
    }
}