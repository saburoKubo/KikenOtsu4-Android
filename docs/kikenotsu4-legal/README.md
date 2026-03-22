# kikenotsu4-legal（GitHub Pages 用ファイル）

## Android 向け紹介ページ（`android.html`）

- アプリの「公式サイト」から開く想定のランディングです。iOS 向けサイトの雰囲気（ヒーロー・機能一覧・スクリーンショット枠）に寄せつつ、**広告（バナー・インタースティシャル）と Pro** の説明を Android 用に記載しています。
- 公開 URL（デプロイ後）: `https://saburokubo.github.io/kikenotsu4-legal/android.html`
- スクリーンショットのプレースホルダを、実機キャプチャや iOS 版サイトと揃えた画像に差し替えてください。
- Google Play のリンクは `applicationId` `com.kubosaburo.kikenotsu4` に合わせています。

---

## 利用規約（`terms.html`）

アプリからリンクする **利用規約**のひな形です。

## デプロイ手順

1. リポジトリ [saburokubo/kikenotsu4-legal](https://github.com/saburokubo/kikenotsu4-legal) の **公開ブランチのルート**（通常は `main`）に、このフォルダ内の **`terms.html`** および **`android.html`** をコピーしてコミット・プッシュしてください。
2. 公開 URL 例:
   - 利用規約: `https://saburokubo.github.io/kikenotsu4-legal/terms.html`
   - Android 紹介: `https://saburokubo.github.io/kikenotsu4-legal/android.html`
3. サイトトップ（`index.html`）や `privacy.html` から各ページへのリンクを追加すると迷子になりにくくなります。

アプリ側は `SupportLegalTexts.TERMS_OF_SERVICE_URL` / `OFFICIAL_SITE_URL` で上記 URL を参照しています。
