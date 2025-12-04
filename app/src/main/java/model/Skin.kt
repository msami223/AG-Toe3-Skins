package com.devstormtech.toe3skins.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Skin(
    val id: Int,
    val date: String,
    val title: Title,
    val acf: AcfData
) : Serializable

data class Title(
    val rendered: String
) : Serializable

data class AcfData(
    @SerializedName("truck_model") val truckModel: String,

    // MATCHES WORDPRESS FIELD NAME
    @SerializedName("skin_file_download") val skinFileUrl: String,

    // MATCHES WORDPRESS FIELD NAME
    @SerializedName("skin_image_main") val previewImage1: String,

    @SerializedName("preview_image_2") val previewImage2: String?,
    @SerializedName("preview_image_3") val previewImage3: String?,
    @SerializedName("creator_name") val creatorName: String,
    @SerializedName("download_count") val downloadCount: Int,
    @SerializedName("view_count") val viewCount: Int,
    @SerializedName("installation_instructions") val instructions: String,
    @SerializedName("file_size") val fileSize: String,
    val tags: List<String>?
) : Serializable