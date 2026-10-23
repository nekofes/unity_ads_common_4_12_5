package com.test.unityadcommon

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.test.unityadcommon.databinding.ActivityNextScreenBinding

class NextScreen : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityNextScreenBinding

    private var screenWidth : Int = 0
    private var screenHeight : Int = 0
    private var adHeight : Float = 0F
    private var lang : Int = 0
    private var gestureBar : Int = 0
    private var statusBarHeight : Int = 0
    private var navigationBarHeight : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNextScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lang = intent.getIntExtra("lang",0)
        screenWidth = intent.getIntExtra("screenWidth",0)
        screenHeight = intent.getIntExtra("screenHeight",0)
        adHeight = intent.getFloatExtra("adHeight",0F)
        statusBarHeight = intent.getIntExtra("statusBarHeight", 0)
        navigationBarHeight = intent.getIntExtra("navigationBarHeight", 0)
        gestureBar = intent.getIntExtra("gestureBar",0)

        binding.btn.x = 50F
        binding.btn.y = 0F
        binding.btn.layoutParams.width = screenWidth -(binding.btn.x * 2).toInt()
        binding.btn.layoutParams.height = (50 * resources.displayMetrics.density).toInt()
        binding.btn.text = "前の画面に戻ります"

        binding.unityAdsView.initAndShowAd(lang,screenWidth,adHeight,statusBarHeight,navigationBarHeight)
        //座標を一番下に指定します
        binding.unityAdsView.x = 0F
        binding.unityAdsView.y = (screenHeight - (adHeight + statusBarHeight)).toFloat()
        //binding.unityAdsView.initUnityAdsWithRetry()
        binding.unityAdsView.initUnityAdsWithRetry(this)

        binding.btn.setOnClickListener()
        {
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    //画面が閉じるときに自動で呼ばれる後片付けます
    override fun onDestroy() {
        //デリゲートイベントに null を代入して、共通部品との手を完全に切ります
        binding.unityAdsView.unityAdsDisplayListener = null

        //最後に親（OS）の終了処理を呼びます（必須）
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_next_screen)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}