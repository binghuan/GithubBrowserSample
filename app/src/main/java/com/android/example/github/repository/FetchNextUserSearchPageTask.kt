/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.example.github.repository

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import com.android.example.github.api.*
import com.android.example.github.db.GithubDb
import com.android.example.github.vo.Resource
import com.android.example.github.vo.UserSearchResult
import java.io.IOException

/**
 * A task that reads the user search result in the database and fetches the next page, if it has one.
 */
class FetchNextUserSearchPageTask constructor(
    private val query: String,
    private val githubService: GithubService,
    private val db: GithubDb
) : Runnable {
    private val _liveData = MutableLiveData<Resource<Boolean>>()
    val liveData: MutableLiveData<Resource<Boolean>> = _liveData

    @SuppressLint("NullSafeMutableLiveData")
    override fun run() {
        val current = db.userDao().findSearchResult(query)
        if (current == null) {
            _liveData.postValue(null)
            return
        }
        val nextPage = current.next
        if (nextPage == null) {
            _liveData.postValue(Resource.success(false))
            return
        }
        val newValue = try {
            val response = githubService.searchUsers(query, nextPage).execute()
            when (val apiResponse = ApiResponse.create(response)) {
                is ApiSuccessResponse -> {
                    // we merge all user ids into 1 list so that it is easier to fetch the
                    // result list.
                    val ids = arrayListOf<Int>()
                    ids.addAll(current.userIds)

                    ids.addAll(apiResponse.body.items.map { it.id })
                    val merged = UserSearchResult(
                        query, ids,
                        apiResponse.body.total, apiResponse.nextPage
                    )
                    db.runInTransaction {
                        db.userDao().insert(merged)
                        db.userDao().insertUsers(apiResponse.body.items)
                    }
                    Resource.success(apiResponse.nextPage != null)
                }
                is ApiEmptyResponse -> {
                    Resource.success(false)
                }
                is ApiErrorResponse -> {
                    Resource.error(apiResponse.errorMessage, true)
                }
            }

        } catch (e: IOException) {
            Resource.error(e.message!!, true)
        }
        _liveData.postValue(newValue)
    }
}
