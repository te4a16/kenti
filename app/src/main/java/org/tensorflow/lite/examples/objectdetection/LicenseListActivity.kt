package org.tensorflow.lite.examples.objectdetection

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import androidx.appcompat.widget.Toolbar

class LicenseListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license_list)

        val toolbar = findViewById<Toolbar>(R.id.license_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 戻るボタン有効化
        toolbar.setNavigationOnClickListener { finish() } // 押したらこの画面を閉じる

        findViewById<Button>(R.id.btn_tensorflow_license).setOnClickListener {
            val intent = Intent(this, LicenseDetailActivity::class.java)
            intent.putExtra("library_name", "TensorFlow")
            startActivity(intent)
        }
    }
}