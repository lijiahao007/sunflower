/*
 * Copyright 2018 Google LLC
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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ShareCompat
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.samples.apps.sunflower.PlantDetailFragment.Callback
import com.google.samples.apps.sunflower.data.Plant
import com.google.samples.apps.sunflower.databinding.FragmentPlantDetailBinding
import com.google.samples.apps.sunflower.viewmodels.PlantDetailViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * A fragment representing a single Plant detail screen.
 */
@AndroidEntryPoint
class PlantDetailFragment : Fragment() {

    private val plantDetailViewModel: PlantDetailViewModel by viewModels() // 使用HiltViewModel来获取目标ViewModel（原理是使用Hilt提供的HiltViewModelFactory工厂类来创建的）

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<FragmentPlantDetailBinding>(
            inflater,
            R.layout.fragment_plant_detail,
            container,
            false
        ).apply {
            // 给布局文件中的变量设置对应元素
            viewModel = plantDetailViewModel // 设置layout文件中viewModel变量对应的对象
            // 只有binding 设置了 lifecycleOwner， LiveData的更新才能够传播到UI那。 （即单向绑定）
            lifecycleOwner = viewLifecycleOwner // 设置数据绑定的生命周期 (这里很关键)

            // 用作浮动按钮的点击事件， 实现Callback功能接口的add方法
            callback = Callback { plant ->
                plant?.let {
                    // 隐藏浮动按钮
                    hideAppBarFab(fab)
                    // 将plantDetailViewModel中plantId 添加到 garden_plantings 表中
                    plantDetailViewModel.addPlantToGarden()
                    // 显示添加成功的信息（root 是显示弹框的界面）
                    Snackbar.make(root, R.string.added_plant_to_garden, Snackbar.LENGTH_LONG)
                        .show()
                }
            }

            // 如果unsplash key 非法， 则可以通过该 ImageView 跳转到 GalleryFragment
            galleryNav.setOnClickListener { navigateToGallery() }


            var isToolbarShown = false

            // 刚开始的 scrollY=0， 这时顶端的图片会全部显示
            // scroll change listener begins at Y = 0 when image is fully collapsed
            plantDetailScrollview.setOnScrollChangeListener(
                NestedScrollView.OnScrollChangeListener { _, scrollX, scrollY, oldScrollX, oldScrollY ->

                    // 当 NestedScrollView 向上滚动的距离达到 工具栏高度时，植物的名字会被遮盖，这时就将植物名字写在工具栏上。这时需要显示工具栏
                    // User scrolled past image to height of toolbar and the title text is
                    // underneath the toolbar, so the toolbar should be shown.
                    val shouldShowToolbar = scrollY > toolbar.height
                    Log.i("OnScrollChangeListener", "$scrollX, $scrollY, $oldScrollX, $oldScrollY， toolbar.height=${toolbar.height}")

                    // 更新工具栏的属性和状态
                    // The new state of the toolbar differs from the previous state; update
                    // appbar and toolbar attributes.
                    if (isToolbarShown != shouldShowToolbar) {
                        isToolbarShown = shouldShowToolbar

                        // 使用阴影动画
                        // Use shadow animator to add elevation if toolbar is shown
                        appbar.isActivated = shouldShowToolbar

                        // 只有在上滑到隐藏植物名字时，标题中的植物名字才会显示
                        // Show the plant name if toolbar is shown
                        toolbarLayout.isTitleEnabled = shouldShowToolbar
                    }
                }
            )

            // 导航按钮的点击事件 （具体设置的图案是箭头）
            toolbar.setNavigationOnClickListener { view ->
                // 返回操作
                view.findNavController().navigateUp()
            }

            // 设置 菜单项点击事件
            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    // 分享操作
                    R.id.action_share -> {
                        createShareIntent()
                        true
                    }
                    else -> false
                }
            }
        }
        setHasOptionsMenu(true)

        return binding.root
    }

    // 当 Unsplash Key 不合法时， 则可以直接去 Unsplash Gallery 查看植物图片
    private fun navigateToGallery() {
        plantDetailViewModel.plant.value?.let { plant ->
            val direction =
                PlantDetailFragmentDirections.actionPlantDetailFragmentToGalleryFragment(plant.name)
            findNavController().navigate(direction)
        }
    }


    // Helper function for calling a share functionality.
    // Should be used when user presses a share button/menu item.
    @Suppress("DEPRECATION")
    private fun createShareIntent() {
        // 获取植物的名字
        val shareText = plantDetailViewModel.plant.value.let { plant ->
            if (plant == null) {
                ""
            } else {
                getString(R.string.share_text_plant, plant.name)
            }
        }

        // ShareCompact 是 便捷的程序间程序共享的组件
        // 使用 ShareCompat.IntentBuilder 来创建开启其他应用或者Activity
        val shareIntent = ShareCompat.IntentBuilder.from(requireActivity())
            .setText(shareText) // 设置文本信息
            .setType("text/plain") // 设置类型
            .createChooserIntent() // 打开Android标准Activity选择器， 让用户选择要共享信息的Activity、App
                // 设置了Flag ： Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                // FLAG_ACTIVITY_NEW_DOCUMENT :  不会为新的Activity 创建新的Task
                // FLAG_ACTIVITY_MULTIPLE_TASK : 强制将Activity启动到一个新的task
                // or : 按位或操作，结果是 134742016
            .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

        startActivity(shareIntent)
    }

    // FloatingActionButtons anchored to AppBarLayouts have their visibility controlled by the scroll position.
    // We want to turn this behavior off to hide the FAB when it is clicked.
    //
    // This is adapted from Chris Banes' Stack Overflow answer: https://stackoverflow.com/a/41442923
    private fun hideAppBarFab(fab: FloatingActionButton) {
        val params = fab.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as FloatingActionButton.Behavior
        // FloatActionButton 在布局文件中固定在了 AppBarLayout 底部，这样当滑动到一定位置时，FAB会自动隐藏和显示
        // 我们需要将FloatingActionButton一直隐藏，那么就要取消自动隐藏和显示的“Behavior” (isAutoHideEnabled=false)
        behavior.isAutoHideEnabled = false
        fab.hide()
    }

    // kotlin中的功能接口，只允许有一个非抽象成员方法
    fun interface Callback {
        fun add(plant: Plant?)
    }
}
