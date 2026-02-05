package org.tensorflow.lite.examples.objectdetection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.appcompat.widget.Toolbar

class LicenseDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license_detail)

        val toolbar = findViewById<Toolbar>(R.id.detail_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val libName = intent.getStringExtra("library_name")
        supportActionBar?.title = libName

        val textView = findViewById<TextView>(R.id.license_text)
        // ここに例の英文を貼り付けます
        textView.text = """
            本製品は、Apache License 2.0（以下「本ライセンス」）に基づいて提供されています。

            1. 許諾の範囲
            本ライセンスに基づき、利用者は本ソフトウェアの複製、修正、配布、および使用を無償で行うことができます。

            2. 無保証
            本ソフトウェアは「現状のまま」提供され、明示的か黙示的かを問わず、いかなる保証（商品性や特定の目的への適合性など）もいたしません。作者または著作権者は、本ソフトウェアの使用に起因するいかなる請求、損害、その他の責任についても負わないものとします。

            3. 著作権表示の維持
            本ソフトウェアの複製または修正したものを配布する場合、元のソフトウェアに含まれていた著作権表示、および本ライセンスの写しを同梱する必要があります。

            詳細は以下の原文（英語）をご確認ください。
            --------------------------------------------------
            TensorFlow Lite

            Copyright 2022 The TensorFlow Authors. All Rights Reserved.

            This product is licensed under the Apache License, Version 2.0.
            You may obtain a copy of the License at:
            http://www.apache.org/licenses/LICENSE-2.0


        """.trimIndent()
    }
}