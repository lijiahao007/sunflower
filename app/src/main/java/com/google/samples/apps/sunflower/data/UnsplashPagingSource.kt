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

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.samples.apps.sunflower.api.UnsplashService

private const val UNSPLASH_STARTING_PAGE_INDEX = 1

class UnsplashPagingSource(
    private val service: UnsplashService, // 在 UnsplashRepository 中传入
    private val query: String
) : PagingSource<Int, UnsplashPhoto>() {  // key: Int， value: UnsplashPhoto

    // params 包含 要加载的key 以及对应的 分页
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UnsplashPhoto> {
        Log.i("Paging3", "load(),  params.key = ${params.key}")

        // 如果 params.key 没有定义，则从第一页开始 (这里 key 就是加载的页码)
        val page = params.key ?: UNSPLASH_STARTING_PAGE_INDEX
        return try {
            // loadSize ?
            // 使用 retrofit2 进行网络请求
            val response = service.searchPhotos(query, page, params.loadSize)
            val photos = response.results

            // LoadResult 是一个密封类。（有两个子类 ： LoadResult.Page 、 LoadResult.Error）
            // 加载成功，load 返回 LoadResult.Page
            LoadResult.Page(
                data = photos, // 数据是 List<UnsplashPhoto>
                prevKey = if (page == UNSPLASH_STARTING_PAGE_INDEX) null else page - 1,
                nextKey = if (page == response.totalPages) null else page + 1
            )
        } catch (exception: Exception) {

            // 加载失败， load 返回 LoadResult.Error
            LoadResult.Error(exception)
        }
    }

    // 当数据在初始加载后刷新或失效时，该方法会返回要传递给 load() 方法的键（params.key）。  在后续刷新数据时，Paging 库会自动调用此方法获取key。
    // PagingState : 分页系统的快照状态。 包括： 1. 已加载的pages    2. 上次访问的 anchorPosition    3. 使用的 config。
    // 该函数返回的key，应可以让 load() 加载足够的item， 来填充窗口。 这些item应在该函数返回的key附近
    override fun getRefreshKey(state: PagingState<Int, UnsplashPhoto>): Int? {
        Log.i("Paging3", "getRefreshKey : anchorPosition=${state.anchorPosition}")
        return state.anchorPosition?.let { anchorPosition ->
            // This loads starting from previous page, but since PagingConfig.initialLoadSize spans
            // multiple pages, the initial load will still load items centered around
            // anchorPosition. This also prevents needing to immediately launch prepend due to
            // prefetchDistance.
            // anchorPosition ： 上一次访问的位置， 通常是顶部项 或者 底部项
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
