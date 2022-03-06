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

package com.google.samples.apps.sunflower

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.google.samples.apps.sunflower.adapters.GalleryAdapter
import com.google.samples.apps.sunflower.databinding.FragmentGalleryBinding
import com.google.samples.apps.sunflower.viewmodels.GalleryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GalleryFragment : Fragment() {

    private val adapter = GalleryAdapter()
    private val args: GalleryFragmentArgs by navArgs()  // 跳转时携带的参数 （safeArgs）
    private var searchJob: Job? = null
    private val viewModel: GalleryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentGalleryBinding.inflate(inflater, container, false)
        context ?: return binding.root
        // 设置 PagingDataAdapter
        binding.photoList.adapter = adapter

        // 用植物名字搜索
        search(args.plantName)

        binding.toolbar.setNavigationOnClickListener { view ->
            view.findNavController().navigateUp() // 返回上一个目的地、并销毁栈顶目的地
        }

        return binding.root
    }

    private fun search(query: String) {
        // Make sure we cancel the previous job before creating a new one
        // lifecycleScope 创建的协程与 GalleryFragment的生命周期相关联
        // 当同一个生命周期内有多个searchJob协程时，先关闭旧的协程，再开启新的协程
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            // 收集 ViewModel中产生的 Flow
            // collectLatest: 当上游流发出新值时，前一个值的处理如果没有完成，会被取消。直接开始处理新值
            // 监听 PagingData 的更新，实时的更新数据
            viewModel.searchPictures(query).collectLatest {
                adapter.submitData(it) // lifecycleScope.launch 自动实现了在 Dispatchers.Main 上执行UI更新的操作
            }
        }
    }
}
