package com.test.unityadcommon

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import com.test.unityadcommon.databinding.ViewUnityAdsBinding

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope

//イベントの結果に使用するステータス
enum class UnityAdsStatus(val value: Int)
{
    RESULT_OK(0),
    INITIALIZATION_FAILED(1),
    INITIALIZATION_FAILED_FIRST_TIME(2),
    BANNER_LOADED(3),
    BANNER_SHOWN(4),
    BANNER_LOAD_FAILED(5),
    INITIALIZATION_COMPLETE(6)
}
enum class UnityAdsDisplay(val value:Int)
{
    NONE(0),
    DISPLAY(1)
}
class UnityAds @JvmOverloads constructor(
    context: Context, // 👈 自身のコンストラクタ引数
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    //view_unity_ads.xml と紐付いたバインディング変数
    private val binding: ViewUnityAdsBinding

    private val cmn=Func()
    // アクティビティのプロパティ（メンバー変数）として定義
    private var isInitializing = false
    private var unityAdsDisplay : UnityAdsDisplay = UnityAdsDisplay.NONE
    private var lang:Int=0
    private var screenWidth:Int=0
    private var adHeight: Float=0F
    private var statusBarHeight:Int=0
    private var navigationBarHeight:Int=0

    //[デリゲート関連]
    var unityAdsDisplayListener: ((status: UnityAdsStatus) -> Unit)? = null
    private var initState:Int=0

    init {
        // 共通の広告XMLをインフレートして自分自身（LinearLayout）に合体させる
        binding = ViewUnityAdsBinding.inflate(LayoutInflater.from(context), this, true)
        orientation = VERTICAL // 縦並びをコード側でも保証
    }
    /*
    unity adsの表示有無を返すプロパティ
    NONE:0
    DISPLAY:1
    */
    fun unityAdsDisplay():UnityAdsDisplay
    {
        return this.unityAdsDisplay
    }

    /**
     * 広告を表示・制御するための共通関数
     * 各画面（MainActivityやS1など）からこの関数を呼び出します
     */
    fun initAndShowAd(lang:Int,
                      screenWidth: Int,
                      adHeight:Float,
                      statusBarHeight: Int,
                      navigationBarHeight: Int
    )
    {
        this.lang=lang
        this.screenWidth=screenWidth
        this.adHeight=adHeight
        this.statusBarHeight=statusBarHeight
        this.navigationBarHeight=navigationBarHeight

        control()
    }
    private fun control()
    {
        binding.adArea.x = 0F
        binding.adArea.y =0F//finalY
        binding.adArea.textAlignment = TEXT_ALIGNMENT_CENTER
        binding.adArea.setBackgroundColor(Color.argb(240,102,153,255))
        binding.adArea.setTextColor(Color.WHITE)
        binding.adArea.text="ここの上にunity adsの広告が表示されます。\n広告表示後は、このラベルは非表示に切り替わります。"
        //広告本体（adView）をadAreaの上にぴったり重ねるように配置
        binding.adView.x = binding.adArea.x
        binding.adView.y = binding.adArea.y

        //Unity広告専用の「設定用紙」を新しく作ります。
        //サイズは320dp×50dpに相当する値（adWidthPx, adHeightPx）
        //第3引数に「android.view.Gravity.CENTER_HORIZONTAL」を設定します
        val layoutParams = FrameLayout.LayoutParams(
            screenWidth, //計算した320dp相当のピクセル幅
            adHeight.toInt(), //計算した50dp相当のピクセル高,
            Gravity.CENTER_HORIZONTAL //自動的に中央寄せになります！
        )

        // 設定用紙をUnity広告に貼り付けます
        //bannerView.layoutParams = bannerParams
        // サイズ（LayoutParams）を適用
        binding.adArea.layoutParams = layoutParams
        binding.adView.layoutParams = layoutParams
    }

    //バナー広告の高さ
    fun adHeight(): Float {
        val density = context.resources.displayMetrics.density
        return 50 * density
    }

    fun initUnityAdsWithRetry(context: Context)
    {
        Log.d("UnityAds", "initialize呼び出し")
        if (isInitializing) return
        isInitializing = true
        //val result=cmn.adTestMethod()
        val result=cmn.adTestMethod(context)
        val gameId = result.first //game id
        val testMode = result.second //デバッグ中ならtrue

        //UnityAds.isInitialized:公式チェック関数
        if (UnityAds.isInitialized == false)
        {
            UnityAds.initialize(context, gameId, testMode, object : IUnityAdsInitializationListener
            {
                override fun onInitializationComplete()
                {
                    if(initState==0)
                    {
                        initState=1
                        unityAdsDisplayListener?.invoke(UnityAdsStatus.INITIALIZATION_COMPLETE)
                    }
                    isInitializing = false
                    //初期化完了後にバナーを読み込む
                    loadUnityBanner(context)
                }

                override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError, message: String)
                {
                    if(initState==0)
                    {
                        initState=1
                        unityAdsDisplayListener?.invoke(UnityAdsStatus.INITIALIZATION_FAILED_FIRST_TIME)
                    }
                    isInitializing = false
                    unityAdsDisplay=UnityAdsDisplay.NONE//広告非表示のため
                    binding.adArea.visibility= VISIBLE
                    Log.e("UnityAds", "初期化失敗: $message")

                    unityAdsDisplayListener?.invoke(UnityAdsStatus.INITIALIZATION_FAILED)

                    //10秒後に自動で再初期化を試みます（コルーチン）。
                    // ライフサイクルに紐づいた安全なコルーチンスコープでリトライ（メモリリーク防止）
                    findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                        Log.e("UnityAds", "delay(10000)実行前")
                        delay(10000)
                        initUnityAdsWithRetry(context)
                    }
                }
            })
        }
        else
        {
            //初期化完了後にバナーを読み込む
            loadUnityBanner(context)
        }
    }
    /**
     * ContextWrapperを遡って安全にActivityを取得する補助関数（クラッシュ防止）
     */
    private fun getActivity(): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }

    private fun loadUnityBanner(context: Context)
    {
        val placementId = cmn.placementId(context)
        val activity = getActivity() ?: return // 安全にActivityを取得
        val bannerView = BannerView(activity, placementId, UnityBannerSize(320, 50))

        //密度(density)を考慮した「320dp」の本当のピクセル幅を計算する
        val density = context.resources.displayMetrics.density
        val adWidthPx = (320 * density).toInt()  // 320dpをpxに変換
        val adHeightPx = adHeight.toInt()       // 50dpをpxに変換

        //横幅いっぱいの器（adView）ではなく、「Unity広告本体」に中央寄せを指定します
        val bannerParams = FrameLayout.LayoutParams(
            adWidthPx,  //画面いっぱいの1080ではなく、広告自身の幅（約960pxなど）にする
            adHeightPx,
            Gravity.CENTER_HORIZONTAL //中央に自動で生成します
        )
        bannerView.layoutParams = bannerParams

        //1. 器（adView）自体は画面幅いっぱいのまま配置します
        binding.adView.layoutParams = FrameLayout.LayoutParams(screenWidth, adHeightPx)

        //2. 超重要：重複追加を防ぐために、一度器の中身を完全にクリアする
        binding.adView.removeAllViews()

        //3. リスナーを設定
        //ロードの状態を監視するリスナー
        bannerView.listener = object : BannerView.IListener
        {
            override fun onBannerLoaded(bannerAdView: BannerView?)
            {

                Log.d("UnityAds", "★バナーのロードに成功しました。")
                unityAdsDisplay=UnityAdsDisplay.DISPLAY//広告が表示
                binding.adArea.visibility= INVISIBLE

                unityAdsDisplayListener?.invoke(UnityAdsStatus.BANNER_LOADED)

            }
            override fun onBannerShown(bannerAdView: BannerView?)
            {
                Log.d("UnityAds", "バナーが表示されました。")
                unityAdsDisplay=UnityAdsDisplay.DISPLAY//広告が表示
                unityAdsDisplayListener?.invoke(UnityAdsStatus.BANNER_SHOWN)
                binding.adArea.visibility= INVISIBLE
            }
            override fun onBannerFailedToLoad(bannerAdView: BannerView?, error: BannerErrorInfo?)
            {
                Log.e("UnityAds", "❌バナーのロードに失敗しました。理由: ${error?.errorMessage}")
                unityAdsDisplay=UnityAdsDisplay.NONE//広告が非表示
                binding.adArea.visibility= VISIBLE
                unityAdsDisplayListener?.invoke(UnityAdsStatus.BANNER_LOAD_FAILED)

                //【対策】広告が取れなくても、枠のサイズ（高さ144pxなど）を強制的に固定して残します。
                //これにより、No Fillになっても領域が 0,0-0,0 になって消えるのを防ぎます。
                val layoutParams = binding.adView.layoutParams
                layoutParams.height = adHeight().toInt() // プロジェクトのバナーの高さ（dpかpxか）に合わせて数値を調整してください。
                binding.adView.layoutParams = layoutParams

                // 確認用に、広告枠の背景色を黄色などにして「枠が消えていないこと」を目視できるようにします。
                binding.adView.setBackgroundColor(Color.argb(100,255,255,100))
            }
            override fun onBannerClick(bannerAdView: BannerView?) {}
            override fun onBannerLeftApplication(bannerAdView: BannerView?) {}
        }
        //4. ロードする前に、器にバナーをしっかりと追加します
        binding.adView.addView(bannerView)

        //5. 最後に広告のロード（読み込み）を実行します
        bannerView.load()
    }

    //UnityAds.kt（共通クラス）の内部に実装するコード

    /**
     * 🧹 画面からこの広告パーツが取り外された（画面が閉じられた）ときに自動で呼ばれる部屋
     */
    override fun onDetachedFromWindow() {
        // 親（LinearLayout）の終了処理を呼ぶ
        super.onDetachedFromWindow()
    }

}