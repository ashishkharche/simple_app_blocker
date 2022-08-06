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

package jp.co.casl0.android.simpleappblocker.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.InflateException
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import jp.co.casl0.android.simpleappblocker.app.AppBlockerApplication
import jp.co.casl0.android.simpleappblocker.R
import jp.co.casl0.android.simpleappblocker.databinding.ActivityMainBinding
import jp.co.casl0.android.simpleappblocker.service.AppBlockerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var _viewModel: MainViewModel
    var appBlockerService: AppBlockerService? = null
    private val vpnPrepare =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Intent(this, AppBlockerService::class.java).also {
                    bindService(it, connection, Context.BIND_AUTO_CREATE)
                }
            } else {
                Logger.d("VpnService rejected")
                finish()
            }
        }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AppBlockerService.AppBlockerBinder
            appBlockerService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Logger.d("service disconnected")
        }
    }

    private fun configureVpnService() {
        val vpnPrepareIntent = VpnService.prepare(this)
        if (vpnPrepareIntent != null) {
            vpnPrepare.launch(vpnPrepareIntent)
        } else {
            // 既にVPN同意済み
            Logger.d("VpnService already agreed")
            Intent(this, AppBlockerService::class.java).also {
                bindService(it, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.addLogAdapter(
            AndroidLogAdapter(
                PrettyFormatStrategy.newBuilder().tag(getString(R.string.app_name)).build()
            )
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
        configureVpnService()
        _viewModel =
            ViewModelProvider(
                this,
                MainViewModelFactory((applicationContext as AppBlockerApplication).repository)
            ).get(MainViewModel::class.java)

        _viewModel.allowlist.observe(this) { newAllowlist ->
            appBlockerService?.run {
                if (enabled) {
                    // 既に適用中のみフィルターを更新する
                    updateFilters(newAllowlist)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appBlockerService?.disableFilters()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.options, menu)

            // アクションバーのスイッチのイベントハンドラを設定
            val actionSwitch = menu.findItem(R.id.app_bar_switch)?.actionView as? SwitchCompat
            actionSwitch?.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    _viewModel.filtersEnabled = true
                    setActionBarTextColor(
                        supportActionBar,
                        getColorInt(R.color.filters_enabled)
                    )
                    lifecycleScope.launch(Dispatchers.IO) {
                        (application as? AppBlockerApplication)?.repository?.allowlist?.let {
                            appBlockerService?.updateFilters(
                                it.first()
                            )
                        }
                    }
                } else {
                    _viewModel.filtersEnabled = false
                    setActionBarTextColor(
                        supportActionBar,
                        getColorInt(R.color.filters_disabled)
                    )
                    appBlockerService?.disableFilters()
                }
            }
            actionSwitch?.isChecked = _viewModel.filtersEnabled
        } catch (e: InflateException) {
            e.localizedMessage?.let { errMsg ->
                Logger.d(errMsg)
            }
        }
        return true
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    private fun setActionBarTextColor(actionBar: ActionBar?, color: Int) {
        val title = SpannableString(actionBar?.title ?: "")
        title.setSpan(
            ForegroundColorSpan(color),
            0,
            title.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        actionBar?.title = title
    }

    private fun getColorInt(resourceId: Int): Int {
        return if (Build.VERSION.SDK_INT >= 23) {
            resources.getColor(resourceId, theme)
        } else {
            resources.getColor(resourceId)
        }
    }
}