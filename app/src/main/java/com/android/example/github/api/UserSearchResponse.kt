package com.android.example.github.api


import com.android.example.github.vo.User
import com.google.gson.annotations.SerializedName

/**
 * Simple object to hold repo search responses. This is different from the Entity in the database
 * because we are keeping a search result in 1 row and denormalizing list of results into a single
 * column.
 */
data class UserSearchResponse(
    @SerializedName("total_count")
    val total: Int = 0,
    @SerializedName("items")
    val items: List<User>
) {
    var nextPage: Int? = null
}
