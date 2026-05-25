# kikenotsu4-legal（GitHub Pages 用ファイル）

## ⚠️ ブラウザで 404 のとき

**アプリの URL ではなく、GitHub Pages がまだ公開されていません。**  
手順は **[GITHUB_PAGES_404のとき.md](./GITHUB_PAGES_404のとき.md)** を参照してください。

---

## Android 向け紹介ページ（ローカルでは `android.html`、Android 用リポジトリでは `index.html`）

- アプリの「公式サイト」から開く想定のランディングです。iOS 向けサイトの雰囲気（ヒーロー・機能一覧・スクリーンショット枠）に寄せつつ、**広告（バナー・インタースティシャル）と Pro** の説明を Android 用に記載しています。
- リポジトリ [kikenotsu4-legal-Android](https://github.com/saburokubo/kikenotsu4-legal-Android) ではルートに **`index.html`** として置く想定です（`android.html` というファイル名は不要）。
- Android アプリが開く URL（デプロイ後）: `https://saburokubo.github.io/kikenotsu4-legal-Android/index.html`（末尾 `/` のみでも可）
- **404 になるとき**: GitHub リポジトリの **Settings → Pages** で **Deploy from branch** を有効にし、**`main` / `/ (root)`** を公開してください。反映まで数分かかることがあります。
- iOS 用トップなど従来の `kikenotsu4-legal` リポジトリも併用する場合は、その README を参照してください。
- スクリーンショットのプレースホルダを、実機キャプチャや iOS 版サイトと揃えた画像に差し替えてください。
- Google Play のリンクは `applicationId` `com.kubosaburo.kikenotsu4` に合わせています。

---

## 利用規約（`terms.html`）

アプリからリンクする **利用規約**のひな形です。

## デプロイ手順

1. Android 向けはリポジトリ [saburokubo/kikenotsu4-legal-Android](https://github.com/saburokubo/kikenotsu4-legal-Android) の **公開ブランチのルート**（通常は `main`）に、**`index.html`**（このフォルダの `android.html` の内容を `index.html` として配置してよい）・**`terms.html`**・**`privacy.html`**・**`app-ads.txt`**・**`assets/`** などをコピーしてコミット・プッシュしてください。
2. 公開 URL 例:
   - トップ（Android 紹介）: `https://saburokubo.github.io/kikenotsu4-legal-Android/index.html`
   - 利用規約: `https://saburokubo.github.io/kikenotsu4-legal-Android/terms.html`
3. `privacy.html` から各ページへのリンクを追加すると迷子になりにくくなります。

アプリ側は `SupportLegalTexts.TERMS_OF_SERVICE_URL` / `OFFICIAL_SITE_URL` で上記 URL を参照しています。

## AdMob app-ads.txt

AdMob の app-ads.txt は、Play Console の「デベロッパーのウェブサイト」に設定したドメインのルート直下で公開される必要があります。

- 配置ファイル: `app-ads.txt`
- 内容: `google.com, pub-1948125585168961, DIRECT, f08c47fec0942fa0`
- 確認 URL: `https://saburokubo.github.io/app-ads.txt`

GitHub Pages のプロジェクトページ（例: `https://saburokubo.github.io/kikenotsu4-legal-Android/`）を Play Console のウェブサイトにしている場合でも、AdMob が `https://saburokubo.github.io/app-ads.txt` を見に行くことがあります。そのため、ユーザー Pages リポジトリ（通常は `saburokubo.github.io`）のルートにも同じ `app-ads.txt` を公開してください。
