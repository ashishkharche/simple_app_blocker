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

package jp.co.casl0.android.simpleappblocker.utilities

import android.view.View
import androidx.annotation.NonNull
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar

/**
 * スナックバーを表示する
 */
internal fun popupSnackbar(
    @NonNull view: View,
    @StringRes message: Int,
    duration: Int = Snackbar.LENGTH_SHORT,
    @StringRes actionLabel: Int,
    actionListener: View.OnClickListener = View.OnClickListener { }
) {
    Snackbar.make(
        view,
        message,
        duration
    ).apply {
        setAction(actionLabel, actionListener)
    }.show()
}