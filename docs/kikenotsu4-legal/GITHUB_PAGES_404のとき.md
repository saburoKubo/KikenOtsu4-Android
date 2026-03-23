# GitHub Pages が 404 になるとき（kikenotsu4-legal-Android）

アプリの URL は正しくても、**GitHub 側でサイトが「公開」されていないと必ず 404** になります。次を上から順に確認してください。

## 1. 公開 URL の形

- プロジェクト用 Pages: `https://<ユーザー名>.github.io/<リポジトリ名>/index.html`
- 例: `https://saburokubo.github.io/kikenotsu4-legal-Android/index.html`
- **リポジトリ名の大文字小文字**（`Android` の `A` など）は GitHub 上の名前と一致させます。

PC のブラウザで上の URL を開いても 404 なら、アプリの問題ではなく **Pages 未設定**です。

## 2. Pages を有効にする（いちばん多い原因）

1. GitHub で **`kikenotsu4-legal-Android`** リポジトリを開く  
2. **Settings（設定）** → 左メニュー **Pages**  
3. **Build and deployment** の **Source** を確認する  

### A. 「Deploy from a branch」を使う（手軽）

1. Source: **Deploy from a branch**  
2. **Branch**: `main`（または HTML を置いているブランチ）  
3. **Folder**: **`/ (root)`**（`index.html` がリポジトリのルートにある場合）  
4. **Save**  
5. 画面上部に `Your site is live at https://....` と出るまで **数分〜10分程度** 待つ  

`index.html` を **`docs` フォルダ**にだけ置いている場合は、Folder を **`/docs`** にします。

### B. 「GitHub Actions」だけ選んで workflow が無い（404 の典型）

Source が **GitHub Actions** になっているのに、リポジトリに **`.github/workflows/*.yml` が無い**と、ビルドされず **ずっと 404** のままです。

- **対処1**: Source を **Deploy from a branch** に戻して、上記 A のとおり `main` / `(root)` を指定する  
- **対処2**: このフォルダにある **`deploy-github-pages.yml`** を、リポジトリの  
  `.github/workflows/deploy-github-pages.yml`  
  としてコピーし、push したうえで、Pages の Source を **GitHub Actions** のままにする  

## 3. Jekyll 由来の 404 を避ける（任意・おすすめ）

リポジトリの**ルート**（`index.html` と同じ階層）に、**中身が空の `.nojekyll`** ファイルを置いて commit します。  
（この `docs/kikenotsu4-legal` フォルダにも `.nojekyll` のコピー用を置いてあります。）

## 4. それでも 404 のとき

- リポジトリが **Private** の場合、無料アカウントでは Pages の扱いが制限されることがあります。**Public** にするか、GitHub の [Pages のドキュメント](https://docs.github.com/pages) を確認してください。  
- **Actions** タブで、Pages 用 workflow が **失敗**していないか見る  
- URL を **シークレットウィンドウ**で開き直す（キャッシュ対策）

## 5. 公開できたかの確認

PC ブラウザで次が **200 で表示**されれば OK です。

- `https://saburokubo.github.io/kikenotsu4-legal-Android/index.html`
- `https://saburokubo.github.io/kikenotsu4-legal-Android/privacy.html`
- `https://saburokubo.github.io/kikenotsu4-legal-Android/terms.html`

ここが開けてから、アプリの「公式サイト」「プライバシー」「利用規約」を試してください。
