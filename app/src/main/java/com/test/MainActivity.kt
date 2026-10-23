package com.test.unityadcommon

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.ViewCompat
import com.test.unityadcommon.databinding.ActivityMainBinding

//デリゲート（委譲）用のインターフェースを定義
interface OnScreenSizeCalculatedListener {
    fun onSizeCalculated(width: Int, height: Int, statusBarHeight: Int, navigationBarHeight: Int)
}

class MainActivity : AppCompatActivity(), OnScreenSizeCalculatedListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var screenWidth : Int = 0
    private var screenHeight : Int = 0
    private var adHeight : Float = 0F
    private var lang : Int = 0
    private val cmn=Func()
    private var gestureBar:Int=0
    // 画面側（MainActivityなど）のクラスの上のほう（メンバー変数）にフラグを用意
    private var isSizeCalculated = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //OSのサイズ確定タイミングを捉える
        //サイズ確定を待つ処理（ここ自体はシンプルに保つ）
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            //ループによりy票位置がズレてしまう現象を回避
            //すでに計算済みなら、二度目は処理を通さずにスルーする（ループ防止）
            if (!isSizeCalculated)
            {
                val screenSize = cmn.screenSize(this)
                screenWidth = screenSize.first
                screenHeight = screenSize.second
                adHeight = screenSize.third
                val controlSize = cmn.statusBarHeight(this)
                val statusBarHeight = controlSize.first
                val navigationBarHeight = controlSize.second

                /*
                画面内のジェスチャー（細い横棒）の位置を判定して
                画面内の使用可能な高さを計算します
                */
                val type:PhoneOperationMode=cmn.getNavigationMode(this)
                var adjust:Int=0
                when(type)
                {
                    PhoneOperationMode.GESTURE-> {
                        gestureBar = cmn.gestureBar()
                        adjust = (gestureBar * resources.displayMetrics.density).toInt()
                        screenHeight -= adjust
                    }
                    else->{
                        screenHeight -= navigationBarHeight

                    }
                }
                //計算が終わったらフラグを true にしてロックします
                isSizeCalculated = true

                //デリゲートメソッドを呼び出して、実際の処理を委譲する
                onSizeCalculated(screenWidth, screenHeight, statusBarHeight, navigationBarHeight)

            }
            windowInsets
        }
    }
    //デリゲートされた処理をここに集約します
    override fun onSizeCalculated(width: Int, height: Int, statusBarHeight: Int, navigationBarHeight: Int)
    {
        //unityAdsViewのデリゲートを受け取るイベントを実装します
        binding.unityAdsView.unityAdsDisplayListener = { value ->
            when (value) {
                //ここに下記のように判定したい内容を実装することになります
                UnityAdsStatus.INITIALIZATION_FAILED -> { /* 失敗時の処理 */ }
                UnityAdsStatus.BANNER_LOADED -> { /* ロード完了時の処理 */ }
                else -> { /* それ以外の数値が来た場合の処理（必須） */ }
            }

        }
        //unity adsを配置します
        var y : Float = statusBarHeight.toFloat()
        binding.unityAdsView.initAndShowAd(lang,screenWidth,adHeight,statusBarHeight,navigationBarHeight)
        //座標を一番上に指定します
        binding.unityAdsView.x = 0F
        binding.unityAdsView.y = y
        //binding.unityAdsView.initUnityAdsWithRetry()
        binding.unityAdsView.initUnityAdsWithRetry(this)

        y += binding.unityAdsView.adHeight() + 50F

        binding.btn.x = 50F
        binding.btn.y = y
        binding.btn.layoutParams.width = screenWidth-(binding.btn.x * 2).toInt()
        binding.btn.layoutParams.height = (50 * resources.displayMetrics.density).toInt()
        binding.btn.text = "次の画面を開きます"

        binding.btn.setOnClickListener()
        {

            val intent = Intent(this, NextScreen::class.java)
            intent.putExtra("screenWidth",screenWidth)
            intent.putExtra("screenHeight", screenHeight)
            intent.putExtra("adHeight", adHeight)

            intent.putExtra("statusBarHeight", statusBarHeight)
            intent.putExtra("navigationBarHeight", navigationBarHeight)
            intent.putExtra("gestureBar", gestureBar)

            intent.putExtra("lang", lang)

            startActivity(intent)

        }
    }

    //画面が閉じるときに自動で呼ばれる後片付けます
    override fun onDestroy() {
        //デリゲートイベントに null を代入して、共通部品との手を完全に切ります
        binding.unityAdsView.unityAdsDisplayListener = null

        //最後に親（OS）の終了処理を呼びます（必須）
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}