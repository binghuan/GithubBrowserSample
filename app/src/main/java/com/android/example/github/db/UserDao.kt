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

package com.android.example.github.db

import android.util.SparseIntArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.example.github.testing.OpenForTesting
import com.android.example.github.vo.User
import com.android.example.github.vo.UserSearchResult

/**
 * Interface for database access for User related operations.
 */
@Dao
@OpenForTesting
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(result: UserSearchResult)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUsers(users: List<User>)

    @Query("SELECT * FROM User WHERE login = :login")
    fun findByLogin(login: String): LiveData<User>

    @Query("SELECT * FROM UserSearchResult WHERE `query` = :query")
    fun search(query: String): LiveData<UserSearchResult?>

    @Query(
        """
        SELECT * FROM User
        WHERE login = :owner
        """
    )
    fun loadUsers(owner: String): LiveData<List<User>>

    @Query("SELECT * FROM User WHERE login = :ownerLogin AND name = :name")
    fun load(ownerLogin: String, name: String): LiveData<User>

    fun loadOrdered(userIds: List<Int>): LiveData<List<User>> {
        val order = SparseIntArray()
        userIds.withIndex().forEach {
            order.put(it.value, it.index)
        }
        return loadById(userIds).map { users ->
            users.sortedWith(compareBy { order.get(it.id) })
        }
    }

    @Query("SELECT * FROM User WHERE id in (:userIds)")
    fun loadById(userIds: List<Int>): LiveData<List<User>>

    @Query("SELECT * FROM UserSearchResult WHERE `query` = :query")
    fun findSearchResult(query: String): UserSearchResult?
}
