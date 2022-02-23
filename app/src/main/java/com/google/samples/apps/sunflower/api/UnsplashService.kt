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

package com.google.samples.apps.sunflower.api

import com.google.samples.apps.sunflower.BuildConfig
import com.google.samples.apps.sunflower.data.UnsplashSearchResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Used to connect to the Unsplash API to fetch photos
 * Retrofit2 中和 Unsplash 相关的网络请求接口
 */
interface UnsplashService {

    // GET 请求： BASE_URL/search/photos
    // retrofit2 2.6.0 支持协程
    @GET("search/photos")
    suspend fun searchPhotos(
        // @Query 设置 GET 请求方法参数
        @Query("query") query: String, // 植物名
        @Query("page") page: Int, // 页数
        @Query("per_page") perPage: Int, // 每页个数
        @Query("client_id") clientId: String = BuildConfig.UNSPLASH_ACCESS_KEY // unsplash_access_key
    ): UnsplashSearchResponse

    // 内部伴生类，使用上相当于静态成员
    companion object {
        private const val BASE_URL = "https://api.unsplash.com/"

        // 静态方法
        fun create(): UnsplashService {

            // OKHttp 拦截器， 将所有的请求和响应记录下来
            val logger = HttpLoggingInterceptor().apply { level = Level.BASIC }

            // OKHttpClient
            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()

            // 返回 UnsplashService 实例
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create()) // Gson 数据格式转换
                .build()
                .create(UnsplashService::class.java)
        }
    }
}
