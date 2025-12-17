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

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.objectdetection.databinding.ActivityMainBinding
import android.content.res.Configuration//PIP

/**
 * Main entry point into our app. This app follows the single-activity pattern, and all
 * functionality is implemented in the form of fragments.
 */
class MainActivity : AppCompatActivity() {

    // ViewBinding 用の変数
    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // layout/activity_main.xml を ViewBinding 経由で読み込む
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)

        // アクティビティの表示内容としてセット
        setContentView(activityMainBinding.root)
    }

    //PiP
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        // PiPモードがサポートされており、かつ現在 PiP モードではない場合、
        // PiPモードへの移行を試みるよう Fragment に促します。
        // 実際のPiP移行は CameraFragment 内で行われます。
    }

    

    override fun onBackPressed() {
        // Android 10(Q) の戻る操作で起きるメモリリーク対策
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Android Qにおけるメモリリーク問題の回避策（IRequestFinishCallback$Stub内）
            // (https://issuetracker.google.com/issues/139738913)
            // 特殊処理
            finishAfterTransition()
        } else {
            // 通常の戻る動作
            super.onBackPressed()
        }
    }

}
