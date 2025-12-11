package com.devstormtech.toe3skins.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Sticker(
    val id: Int,
    val title: Title,
    @SerializedName("featured_media") val featuredMedia: Int,
    @SerializedName("_embedded") val embedded: EmbeddedMedia?
) : Serializable

data class EmbeddedMedia(
    @SerializedName("wp:featuredmedia") val featuredMediaList: List<FeaturedMedia>?
) : Serializable

data class FeaturedMedia(
    @SerializedName("source_url") val sourceUrl: String?
) : Serializable {
    // Helper to get the image URL
    fun getImageUrl(): String? = sourceUrl
}

// Extension function to easily get the sticker image URL
fun Sticker.getImageUrl(): String? {
    return embedded?.featuredMediaList?.firstOrNull()?.sourceUrl
}
