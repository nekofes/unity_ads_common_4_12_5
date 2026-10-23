package com.test.unityadcommon

import android.content.Context
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager

//端末の操作モード
enum class PhoneOperationMode(val value: Int)
{
    NORMAL(0),
    THREE_BUTTON(1),
    GESTURE(2)
}
class Func
{
    //0:本番環境
    //1:テスト環境
    fun testMethod(context: Context): Int {
        val hasDebug = try {
            // 自動的に呼び出し元アプリのパッケージ名を取得し、その中のBuildConfigクラスを探します
            val packageName = context.packageName
            Class.forName("$packageName.BuildConfig")
                .getField("DEBUG")
                .getBoolean(null)
        } catch (e: Exception) {
            false
        }

        return if (hasDebug) 1 else 0
    }
    fun adTestMethod(context: Context): Pair<String, Boolean> {
        val result = testMethod(context)
        // 123456789: あなたのUnity Game ID (本番用)
        val gameId = if (result == 0) "123456789" else "4550882"
        val isTestMode = (result != 0) // デバッグ中ならtrue（テストモードON）

        return Pair(gameId, isTestMode)
    }

    fun placementId(context: Context): String {
        val result = testMethod(context)
        return if (result == 0) "BP_Banner_Android" else "Banner_Android"
    }
    fun statusBarHeight(context: Context): Pair<Int, Int> {
        var statusBarHeight = 0
        var navigationBarHeight = 0

        val resources = context.resources
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT >= 30) {
            val metrics = windowManager.currentWindowMetrics
            statusBarHeight = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
            navigationBarHeight = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom
        } else {
            var resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0

            resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            navigationBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
        }

        return Pair(statusBarHeight, navigationBarHeight)
    }
    fun screenSize(context: Context): Triple<Int, Int, Float> {
        var width = 0
        var height = 0
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT <= 29) {
            width = context.resources.displayMetrics.widthPixels // 画面の幅
            height = context.resources.displayMetrics.heightPixels // 画面の高さ
        } else {
            val bounds = windowManager.currentWindowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
        }

        val density = context.resources.displayMetrics.density
        val adHeight = (50 * density) // 50dpをピクセル化

        return Triple(width, height, adHeight)
    }
    /**
     * 💡 端末の操作モードを完全に判定する関数
     * 戻り値:
     *   0 ＝ 画面外の物理ボタン（ノーマルタイプ、iPhoneのホームボタン風）
     *   1 ＝ 画面内の3つボタン
     *   2 ＝ 画面内のジェスチャー（細い横棒）
     */
    /**
     * 💡 シミュレーターの誤判定を回避し、操作モードを確実に取得する関数
     */
    fun getNavigationMode(context: Context): PhoneOperationMode {
        return try {
            //【改善】OSの設定（ジェスチャーか3ボタンか）を最優先でチェックします
            val resourceId = context.resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
            if (resourceId > PhoneOperationMode.NORMAL.value) {
                val mode = context.resources.getInteger(resourceId)
                if (mode == 2) return PhoneOperationMode.GESTURE // 確実に「2：ジェスチャー（横棒）」が返ります
                if (mode == 1) return PhoneOperationMode.THREE_BUTTON // 3つボタン
            }

            // 上記の設定が取れなかった場合のみ、古い機種や特殊な物理ボタンの可能性をチェックする
            val hasHomeKey = android.view.ViewConfiguration.get(context).hasPermanentMenuKey()
            val hasBackKey = android.view.KeyCharacterMap.deviceHasKey(android.view.KeyEvent.KEYCODE_BACK)
            if (hasHomeKey || hasBackKey) {
                return PhoneOperationMode.NORMAL // 画面外の物理ボタン（ノーマル）
            }

            PhoneOperationMode.THREE_BUTTON // 判定不能時は安全のため3つボタン扱い
        } catch (e: Exception) {
            PhoneOperationMode.THREE_BUTTON
        }
    }
    fun gestureBar():Int
    {
        return 15
    }

}