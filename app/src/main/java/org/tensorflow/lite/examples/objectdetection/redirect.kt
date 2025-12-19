package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.content.Intent

/**
 * MainActivityからSettingsActivity（設定画面）へ遷移する
 */
fun Context.navigateToSettings() {
    val intent = Intent(this, SettingsActivity::class.java)
    startActivity(intent)
}