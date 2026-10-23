# unity_ads_4_12_5 (共通部品化サンプル)

Unity広告SDK（バージョン4.12.5）をAndroid（Kotlin）に実装するためのサンプルプログラムです。
今回は、**Unity Adsの処理をカスタムViewとして共通部品化**し、各画面への導入を大幅に簡略化できる構成にアップデートしました。

## 📌 主な特徴・機能
* **共通部品化 (`UnityAds.kt`):** 広告の初期化・読み込み・表示処理を一括管理。
* **簡単導入:** 広告を表示したい画面のレイアウト（XML）にカスタムViewを追加するだけで配置可能。
* **複数レイアウト対応:** 画面上部への配置（MainActivity）、画面下部への配置（NextScreen）の双方のサンプルを同梱。
* **テストモード自動判定:** デバッグビルド時に自動でテスト広告に切り替わる仕組みを実装。

---

## 🛠 開発環境
* **開発ツール:** Android Studio
* **言語:** Kotlin
* **Unity Ads SDK Version:** 4.12.5
* **動作確認 coreKtx Version:** 1.10.1

## 📂 主要なファイル構成
* `UnityAds.kt` : Unity Adsの処理・表示を共通化したカスタムViewクラス。
* `view_unity_ads.xml` : `UnityAds.kt` 内で使用している広告用レイアウト。
* `Func.kt` : 環境判定（本番/テスト）や各IDの設定を集約した共通クラス。
* `MainActivity.kt` : 広告を【画面上部】に配置したスタート画面のサンプル。
* `NextScreen.kt` : 広告を【画面下部】に配置した次の画面のサンプル。
* `capture/` : テストモードでの実際の実行画面キャプチャ。

---

## 🔍 実装のポイントと事前設定

ご自身のプロジェクトで動かす前に、`Func.kt` 内の以下の3箇所を必ず修正してください。

### 1. テストモードの自動判定 (`testMethod`)
`Func.kt` 内の `testMethod` は、`BuildConfig.DEBUG` と同等の処理を行います。
* **注意点:** コード内の `Class.forName("ご自身のパッケージ名.BuildConfig")` の部分を、必ずご自身のパッケージ名に置き換えてください。
* **戻り値:** `0`（本番環境） / `1`（テスト環境）

### 2. Unity Game IDの設定 (`adTestMethod`)
`Func.kt` 内の `adTestMethod` メソッドにある `gameId` を書き換えます。
* サンプルでは `123456789` になっています。ご自身の **Unity Game ID** に差し替えてください。

### 3. Placement IDの設定 (`placementId`)
`Func.kt` 内の `placementId` メソッドにある ID を書き換えます。
* サンプルでは `xxx` になっています。Unityダッシュボードで取得した **Placement ID** に差し替えてください。

#### 📊 広告に使用するパラメータ一覧

| パラメータの種類 | 本番モード | テストモード |
| :--- | :--- | :--- |
| **Game ID** | あなたのUnity Game ID | `4550882` |
| **Test Mode** | `false` | `true` |
| **Placement ID** | Unity登録画面で取得したID | `Banner_Android` |

---

## ⚙️ 自分のプロジェクトへの導入・修正手順

### 1. 各種設定ファイルの修正

#### 📄 AndroidManifest.xml
Android 12以降で広告IDを正常に読み取るため、`<manifest>` タグ内に以下のパーミッションを追加します。
```xml
<!-- Android 12以降で広告IDを読み取るために必須 -->
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

#### 📄 libs.version.toml
新規プロジェクトに導入する場合、`coreKtx` のバージョン競合でエラーが発生することがあります。本サンプルは **`1.10.1`** で動作確認をしています。
**[場所]** `Gradle Scripts` ＞ `libs.version.toml`
```toml
[versions]
coreKtx = "1.10.1"
material = "1.10.0"
```

#### 📄 build.gradle.kts (:app)
Unity広告SDKの依存関係と、`BuildConfig` の自動生成設定を追加します。
**[場所]** `Gradle Scripts` ＞ `build.gradle.kts (:app)`
```kotlin
android {
    // ...
    buildFeatures {
        // BuildConfigの自動生成を有効化
        buildConfig = true
    }
}

dependencies {
    // Unity Ads SDK
    implementation("com.unity3d.ads:unity-ads:4.12.5")
}
```

### 2. 共通部品ファイルの配置

1. `Func.kt`、`UnityAds.kt` をご自身のソースコードディレクトリ（パッケージ内）にコピーします。
2. `app/src/main/res/layout/` を右クリック ＞ `New` ＞ `Layout Resource File` から、ファイル名 `view_unity_ads` を作成し、サンプルの `view_unity_ads.xml` の内容を貼り付けます。

### 3. 各画面でのレイアウト配置と解放処理

#### 📄 画面レイアウト（XML）への配置例
バナー広告を表示したい場所に、以下のようにパッケージ名を含めたカスタムViewタグを記述します。
```xml
<!-- パッケージ名はご自身のものに変更してください -->
<com.example.yourpackage.UnityAds
    android:id="@+id/unityAdsView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    />
```

#### 📄 Activityでの解放処理（必須）
メモリリークや意図しない挙動を防ぐため、画面が破棄されるタイミング（`onDestroy`）でリスナーの解放処理を必ず実装してください。
```kotlin
override fun onDestroy() {
    // デリゲートイベントに null を代入して、共通部品との接続を完全に切ります
    binding.unityAdsView.unityAdsDisplayListener = null

    // 最後に親（OS）の終了処理を呼びます
    super.onDestroy()
}
```

---

## 🔗 関連リンク
* **開発者サイト:** [nekofes.github.io](https://nekofes.github.io/)
