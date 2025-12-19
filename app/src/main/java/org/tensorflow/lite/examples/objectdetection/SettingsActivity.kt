package org.tensorflow.lite.examples.objectdetection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
// setting_page.xmlから自動生成されたBindingクラス（ファイル名から推測）
import org.tensorflow.lite.examples.objectdetection.databinding.SettingPageBinding 

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: SettingPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 戻るボタン（close_button）が押されたらこの画面を閉じる
        binding.closeButton.setOnClickListener {
            finish() 
        }

        // 音量設定項目（item_volume_button）が押された時の処理
        binding.itemVolumeButton.setOnClickListener {
            // ここに音量設定の処理を記述
        }

        // リセット項目（item_reset_button）が押された時の処理
        binding.itemResetButton.setOnClickListener {
            // ここにリセットの処理を記述
        }
    }
}