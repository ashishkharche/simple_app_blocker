/*
 * Copyright 2022 CASL0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.casl0.android.simpleappblocker.apppackagelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import jp.co.casl0.android.simpleappblocker.AppBlockerApplication
import jp.co.casl0.android.simpleappblocker.MainActivity
import jp.co.casl0.android.simpleappblocker.PackageInfo
import jp.co.casl0.android.simpleappblocker.databinding.FragmentAppPackageListBinding

class AppPackageListFragment : Fragment() {

    private var _binding: FragmentAppPackageListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val appPackageListViewModel =
            ViewModelProvider(
                this,
                AppPackageListViewModelFactory((context?.applicationContext as AppBlockerApplication).repository)
            ).get(AppPackageListViewModel::class.java)

        _binding = FragmentAppPackageListBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // パッケージリストの設定
        val recyclerView = binding.appPackageRecyclerview
        val adapter = AppPackageListAdapter(context, listOf()).apply {
            setOnItemClickListener(object : AppPackageListAdapter.OnItemClickListener {
                override fun onItemClicked(view: View, position: Int, packageInfo: PackageInfo) {
                    appPackageListViewModel.changeFiltersRule(packageInfo)
                }
            })
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        appPackageListViewModel.packageInfoList.observe(viewLifecycleOwner) {
            if (it != null) {
                adapter.packageInfoList = it
                recyclerView.adapter?.notifyItemRangeChanged(0, adapter.packageInfoList.size)
            }
        }
        appPackageListViewModel.loadInstalledPackages(context)

        appPackageListViewModel.allowlist.observe(viewLifecycleOwner) { newAllowlist ->
            (activity as? MainActivity)?.appBlockerService?.run {
                if (enabled) {
                    // 既に適用中のみフィルターを更新する
                    updateFilters(newAllowlist)
                }
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}