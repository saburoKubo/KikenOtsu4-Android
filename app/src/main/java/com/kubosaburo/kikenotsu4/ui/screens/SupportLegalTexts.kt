package com.kubosaburo.kikenotsu4.ui.screens

/**
 * サポート画面・法務系の固定文面（公式URL・使い方・利用規約）。
 */
object SupportLegalTexts {

    /** Android アプリ「公式サイト」から開く紹介ページ（リポジトリ直下の index.html） */
    const val OFFICIAL_SITE_URL = "https://saburokubo.github.io/kikenotsu4-legal-Android/index.html"
    const val PRIVACY_POLICY_URL = "https://saburokubo.github.io/kikenotsu4-legal-Android/privacy.html"
    /** GitHub Pages に `terms.html` を配置したときの URL */
    const val TERMS_OF_SERVICE_URL = "https://saburokubo.github.io/kikenotsu4-legal-Android/terms.html"
    const val SUPPORT_EMAIL = "kikenotsuinfo@gmail.com"

    val HOW_TO_USE: String = """
        危険物乙4の学習アプリの基本的な使い方です。

        【ホーム】
        ・「学習スタート」から、カリキュラムの続きや本日の復習などに進めます。
        ・下部のタブで「進捗」「設定」にも切り替えられます。

        【カリキュラム】
        ・講義の流れに沿って、テキスト（読む・問題）とクイズを順に進めていきます。
        ・前回の続きから再開できるよう、進み方は端末内に保存されます。

        【自分で学ぶ】
        ・テキスト一覧から、学びたい項目を選んで学習できます。
        ・ブックマークしたテキストは一覧から素早く開けます。

        【復習】
        ・忘却曲線の考え方に近いスケジュールで、出題すべき問題が案内されます。
        ・ホームや学習の流れの中から復習セッションに入れます。

        【模擬テスト】
        ・本番に近い形式で実力チェックができます（画面の案内に従ってください）。

        【設定】
        ・効果音・音量・視聴学習のリマインド通知などは「学習効果」から変更できます。
        ・「学習データを消去」で、進捗・復習・ブックマークなどをまとめて削除できます（元に戻せません）。
        ・有料版（Pro）では広告の非表示や学習上限の解除などの案内があります（課金の実装状況は画面の表示に従ってください）。

        【データについて】
        ・ログインはありません。学習データは原則としてお使いの端末内にだけ保存されます。
        ・機種変更やアプリ削除でデータが失われることがあります。重要なメモは別途控えておくことをおすすめします。
        ・利用規約の全文は公式サイト（ブラウザ）でご確認いただけます。
    """.trimIndent()
}
