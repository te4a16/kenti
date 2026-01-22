package org.tensorflow.lite.examples.objectdetection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
// setting_page.xmlから自動生成されたBindingクラス（ファイル名から推測）
import org.tensorflow.lite.examples.objectdetection.databinding.SettingPageBinding 
import android.widget.SeekBar
import android.content.Context

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: SettingPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. 共有設定（保存先）を開く
        val sharedPrefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)

        // 2. 現在保存されている音量をシークバーに反映させる（デフォルトは80%）
        val savedVolume = sharedPrefs.getInt("alert_volume", 80)
        binding.volumeSeekBar.progress = savedVolume

        // 3. シークバーを動かした時の処理
        binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // バーを動かしたらその値を保存する
                sharedPrefs.edit().putInt("alert_volume", progress).apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 戻るボタン
        binding.closeButton.setOnClickListener {
            finish() 
        }

        // リセットボタン（80に戻す）
        binding.itemResetButton.setOnClickListener {
            binding.volumeSeekBar.progress = 80
            sharedPrefs.edit().putInt("alert_volume", 80).apply()
        }
    }
}