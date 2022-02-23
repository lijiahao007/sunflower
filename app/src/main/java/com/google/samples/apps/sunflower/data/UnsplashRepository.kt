/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.sunflower.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.samples.apps.sunflower.api.UnsplashService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class UnsplashRepository @Inject constructor(
    private val service: UnsplashService // UnsplashService 依赖由 NetworkModule 提供
    ) {

    // 根据 UnsplashPagingSource 创建 PagingData flow
    fun getSearchResultStream(query: String): Flow<PagingData<UnsplashPhoto>> { // (会从PagingSource中检索除一个一个的UnsplashPhoto, 然后通过flow发出去)
        // Pager 后面会调用PagingSource.load()方法，为其提供LoadParams参数，并接收LoadResult
        return Pager(
            config = PagingConfig(enablePlaceholders = false, pageSize = NETWORK_PAGE_SIZE), // 传入配置类PagingConfig，（在这里，不使用占位符、pageSize=25）
            pagingSourceFactory = { UnsplashPagingSource(service, query) } // 获取PagingSource的方法
        ).flow
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 25
    }
}
