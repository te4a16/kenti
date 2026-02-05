/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.objectdetection

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.objectdetection.databinding.ActivityMainBinding
import android.content.res.Configuration
import androidx.activity.OnBackPressedCallback

/**
 * アプリのエントリーポイントとなるメインアクティビティ。
 * シングルアクティビティ構造で、各機能はフラグメントとして実装される。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBindingによるレイアウトの初期化
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        // 設定画面へ遷移するボタンのイベント登録
        activityMainBinding.settingsButton.setOnClickListener {
            navigateToSettings()
        }

        // Android 10 (Q) 専用：メモリリーク対策用の戻る処理登録
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishAfterTransition()
                }
            })
        }
    }

    /**
     * ホームボタン押下時などのシステムイベント。
     * PiP（ピクチャーインピクチャー）モードへの移行契機として利用。
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // 実際の移行処理は CameraFragment の onPause 等で行われる
    }

    /**
     * 設定画面 (SettingsActivity) を起動する
     */
    private fun navigateToSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    /**
     * 戻るボタンが押された際の処理
     */
    override fun onBackPressed() {
        // Android 10(Q) の既知のメモリリーク回避用
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }
}