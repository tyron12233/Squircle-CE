/*
 * Copyright 2020 Brackeys IDE contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brackeys.ui.database.delegate

import android.content.Context
import androidx.room.Room
import com.brackeys.ui.database.AppDatabase
import com.brackeys.ui.database.AppDatabaseImpl

object DatabaseDelegate {

    fun provideAppDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabaseImpl::class.java, AppDatabaseImpl.DATABASE_NAME)
            .build()
    }
}