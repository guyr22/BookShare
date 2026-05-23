package com.example.bookstore.network.dto

import com.google.gson.annotations.SerializedName

data class GoogleBooksResponse(
    @SerializedName("totalItems") val totalItems: Int = 0,
    @SerializedName("items") val items: List<VolumeItem> = emptyList()
)

data class VolumeItem(
    @SerializedName("id") val id: String,
    @SerializedName("volumeInfo") val volumeInfo: VolumeInfo
)

data class VolumeInfo(
    @SerializedName("title") val title: String = "",
    @SerializedName("authors") val authors: List<String> = emptyList(),
    @SerializedName("description") val description: String = "",
    @SerializedName("imageLinks") val imageLinks: ImageLinks? = null
)

data class ImageLinks(
    @SerializedName("thumbnail") val thumbnail: String = "",
    @SerializedName("smallThumbnail") val smallThumbnail: String = ""
)
