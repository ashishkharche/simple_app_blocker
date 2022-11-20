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

import android.Manifest
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
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.orhanobut.logger.Logger
import dagger.hilt.android.AndroidEntryPoint
import jp.co.casl0.android.simpleappblocker.R
import jp.co.casl0.android.simpleappblocker.databinding.ActivityMainBinding
import jp.co.casl0.android.simpleappblocker.service.AppBlockerService
import jp.co.casl0.android.simpleappblocker.utils.AppUpdateController
import jp.co.casl0.android.simpleappblocker.utils.Result
import jp.co.casl0.android.simpleappblocker.utils.popupSnackbar
import jp.co.casl0.android.simpleappblocker.utils.requestPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), AppUpdateController.OnAppUpdateStateChangeListener {

    private lateinit var binding: ActivityMainBinding
    private val _viewModel: MainViewModel by viewModels()
    var appBlockerService: AppBlockerService? = null
    private lateinit var appUpdateManager: AppUpdateManager

    private val updateFlowResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            when (result.resultCode) {
                RESULT_OK -> Logger.d("update ok")
                RESULT_CANCELED -> Logger.d("update canceled")
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> Logger.d("update failed")
            }
        }

    private val prepareVpnServiceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                requestNotificationPermission()
                Intent(this, AppBlockerService::class.java).also {
                    bindService(it, connection, Context.BIND_AUTO_CREATE)
                }
            } else {
                Logger.d("VpnService rejected")
                finish()
            }
        }

    /**
     * 通知権限のリクエスト時のコールバック
     */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Logger.d("notification permission granted")
        } else {
            Logger.d("notification permission not granted")
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AppBlockerService.AppBlockerBinder
            appBlockerService = binder.getService()
            lifecycleScope.launch {
                _viewModel.allowlist.collect { newAllowlist ->
                    appBlockerService?.run {
                        if (enabled) {
                            // 既に適用中のみフィルターを更新する
                            updateFilters(newAllowlist)
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Logger.d("service disconnected")
        }
    }

    private fun prepareVpnService() {
        val vpnPrepareIntent = VpnService.prepare(this)
        if (vpnPrepareIntent != null) {
            prepareVpnServiceLauncher.launch(vpnPrepareIntent)
        } else {
            // 既にVPN同意済み
            Logger.d("VpnService already agreed")
            Intent(this, AppBlockerService::class.java).also {
                bindService(it, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            // 通知の権限をリクエストする
            // 通知が必須というわけではないので、一度でも拒否した場合はリクエストしない
            requestPermission(
                Manifest.permission.POST_NOTIFICATIONS,
                null,
                notificationPermissionLauncher
            )
        }
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
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
                        _viewModel.allowlist.let {
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

    // AppUpdateController.OnAppUpdateStateChangeListener
    override fun onAppUpdateStateChange(state: Int) {
        when (state) {
            InstallStatus.DOWNLOADED -> {
                // アップデート適用のためスナックバーを表示
                popupSnackbar(
                    view = binding.root,
                    message = R.string.update_downloaded_message,
                    duration = Snackbar.LENGTH_INDEFINITE,
                    actionLabel = R.string.restart_for_update
                ) {
                    // アプリを再起動し更新を適用する
                    appUpdateManager.completeUpdate()
                }
            }
        }
    }

    // ライフサイクルメソッド
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateController = AppUpdateController(appUpdateManager).apply {
            setOnAppUpdateStateChangeListener(this@MainActivity)
        }
        lifecycle.addObserver(appUpdateController)
        lifecycleScope.launch {
            appUpdateController.checkForUpdateAvailability().collect {
                Logger.d(it)
                if (it is Result.Success) {
                    if (it.value.updateAvailability == UpdateAvailability.UPDATE_AVAILABLE && it.value.flexibleAllowed) {
                        appUpdateController.startFlexibleUpdate(updateFlowResultLauncher)
                    }
                } else if (it is Result.Error) {
                    Logger.d("checkForUpdateAvailability failed")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        prepareVpnService()
    }

    override fun onDestroy() {
        super.onDestroy()
        appBlockerService?.disableFilters()
    }
}
