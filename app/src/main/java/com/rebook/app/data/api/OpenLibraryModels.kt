package com.rebook.app.data.api

import com.google.gson.annotations.SerializedName

data class OpenLibrarySearchResponse(
    @SerializedName("numFound") val numFound: Int,
    @SerializedName("docs") val docs: List<OpenLibraryBook>
)

data class OpenLibraryBook(
    @SerializedName("key") val key: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("author_name") val authorName: List<String>?,
    @SerializedName("first_publish_year") val firstPublishYear: Int?,
    @SerializedName("cover_i") val coverId: Long?,
    @SerializedName("isbn") val isbn: List<String>?,
    @SerializedName("publisher") val publisher: List<String>?,
    @SerializedName("number_of_pages_median") val numberOfPages: Int?
) {
    fun getCoverUrl(size: CoverSize = CoverSize.MEDIUM): String? {
        return coverId?.let {
            "https://covers.openlibrary.org/b/id/$it-${size.code}.jpg"
        }
    }

    fun getAuthorsString(): String {
        return authorName?.joinToString(", ") ?: ""
    }
}

enum class CoverSize(val code: String) {
    SMALL("S"),
    MEDIUM("M"),
    LARGE("L")
}
