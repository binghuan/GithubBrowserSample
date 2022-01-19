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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.android.example.github.AppExecutors
import com.android.example.github.api.ApiSuccessResponse
import com.android.example.github.api.GithubService
import com.android.example.github.api.UserSearchResponse
import com.android.example.github.db.GithubDb
import com.android.example.github.db.UserDao
import com.android.example.github.testing.OpenForTesting
import com.android.example.github.util.AbsentLiveData
import com.android.example.github.util.RateLimiter
import com.android.example.github.vo.Resource
import com.android.example.github.vo.User
import com.android.example.github.vo.UserSearchResult
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that handles User objects.
 */
@OpenForTesting
@Singleton
class UserRepository @Inject constructor(
    private val appExecutors: AppExecutors,
    private val db: GithubDb,
    private val userDao: UserDao,
    private val githubService: GithubService,
) {

    fun loadUser(login: String): LiveData<Resource<User>> {
        return object : NetworkBoundResource<User, User>(appExecutors) {
            override fun saveCallResult(item: User) {
                userDao.insert(item)
            }

            override fun shouldFetch(data: User?) = data == null

            override fun loadFromDb() = userDao.findByLogin(login)

            override fun createCall() = githubService.getUser(login)
        }.asLiveData()
    }

    private val userListRateLimit = RateLimiter<String>(10, TimeUnit.MINUTES)

    fun loadUsers(owner: String): LiveData<Resource<List<User>>> {
        return object : NetworkBoundResource<List<User>, List<User>>(appExecutors) {
            override fun saveCallResult(item: List<User>) {
                userDao.insertUsers(item)
            }

            override fun shouldFetch(data: List<User>?): Boolean {
                return data == null || data.isEmpty() || userListRateLimit.shouldFetch(owner)
            }

            override fun loadFromDb() = userDao.loadUsers(owner)

            override fun createCall() = githubService.getUsers(owner)

            override fun onFetchFailed() {
                userListRateLimit.reset(owner)
            }

        }.asLiveData()
    }


    fun searchNextPage(query: String): MutableLiveData<Resource<Boolean>> {
        val fetchNextSearchPageTask = FetchNextUserSearchPageTask(
            query = query,
            githubService = githubService,
            db = db
        )
        appExecutors.networkIO().execute(fetchNextSearchPageTask)
        return fetchNextSearchPageTask.liveData
    }

    fun search(query: String): LiveData<Resource<List<User>>> {
        return object : NetworkBoundResource<List<User>, UserSearchResponse>(appExecutors) {

            override fun saveCallResult(item: UserSearchResponse) {
                val userIds = item.items.map { it.id }
                val userSearchResult = UserSearchResult(
                    query = query,
                    userIds = userIds,
                    totalCount = item.total,
                    next = item.nextPage
                )
                db.runInTransaction {
                    userDao.insertUsers(item.items)
                    userDao.insert(userSearchResult)
                }
            }

            override fun shouldFetch(data: List<User>?) = data == null

            override fun loadFromDb(): LiveData<List<User>> {
                return userDao.search(query).switchMap { searchData ->
                    if (searchData == null) {
                        AbsentLiveData.create()
                    } else {
                        userDao.loadOrdered(searchData.userIds)
                    }
                }
            }

            override fun createCall() = githubService.searchUsers(query)

            override fun processResponse(response: ApiSuccessResponse<UserSearchResponse>)
                    : UserSearchResponse {
                val body = response.body
                body.nextPage = response.nextPage
                return body
            }
        }.asLiveData()
    }
}
