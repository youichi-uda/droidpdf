# DroidPDF - プロジェクトルール

## プロジェクト概要

Android向けKotlin製PDFライブラリ（.aar）。iText7の低価格代替を目指す。
仕様書: `android_pdf_library_spec.md`

## 設計方針（必ず遵守）

- **PDF仕様**: ISO 32000-1 (PDF 1.7) 準拠
- **壊れたPDF**: Lenient方針。仕様外データはスキップ/ワーニングで処理し、クラッシュさせない
- **メモリ**: ページ単位の遅延読み込み。不要なページをメモリに展開しない
- **スレッド**: 単一スレッド前提（v1）。不要な同期処理を入れない
- **API設計**: iText7と概念・構造は揃えるが、名前は独自にする

## 実装方針

| レイヤー | 方針 |
|---|---|
| PDFオブジェクトモデル・Reader/Writer | 自前実装 |
| フォントパース・サブセッティング | Apache FontBox |
| 画像処理 | Android標準API（BitmapFactory） |
| 圧縮 | java.util.zip |
| 暗号化 | javax.crypto |
| レイアウトエンジン | 自前実装 |

## コーディング規約

- 言語: Kotlin（Javaは使わない）
- Kotlinの慣用表現を使う（data class, sealed class, extension function等）
- public APIには戻り値の型を明示する
- PDF仕様由来の定数には仕様セクション番号をコメントで付記する
  ```kotlin
  // ISO 32000-1, Table 28 - Entries in the catalog dictionary
  const val TYPE_CATALOG = "Catalog"
  ```
- ログ出力は `android.util.Log` を使用。タグは `DroidPDF`

## パッケージ構成

```
com.droidpdf.core         # PdfDocument, PdfPage, PdfReader, PdfWriter
com.droidpdf.content      # PdfCanvas, PdfFont, PdfImage
com.droidpdf.layout       # Document, Paragraph, Table, Image
com.droidpdf.forms        # AcroForm操作
com.droidpdf.annotations  # 注釈操作
com.droidpdf.manipulation # 結合・分割・抽出
com.droidpdf.security     # 暗号化
com.droidpdf.license      # ライセンスキー検証
```

## ビルド・テスト

```bash
./gradlew build          # ビルド
./gradlew test           # テスト実行
./gradlew ktlintCheck    # Lintチェック
./gradlew ktlintFormat   # Lint自動修正
```

## ブランチ命名規則

```
feature/{phase}-{機能名}    # 新機能: feature/phase1a-pdf-parser
fix/{issue番号}-{概要}      # バグ修正: fix/12-cjk-font-subsetting
refactor/{対象}             # リファクタ: refactor/pdf-object-model
```

## カスタムSkill一覧

| コマンド | 用途 |
|---|---|
| `/review` | コードレビュー |
| `/test` | テスト実行・分析 |
| `/lint` | Lint/フォーマットチェック |
| `/branch {目的}` | ブランチ作成 |
| `/pr` | PR作成 |
| `/validate-pdf {パス}` | PDF検証 |
| `/spec-check` | 仕様準拠チェック |
| `/progress` | 進捗確認 |
| `/todo` | TODO一覧 |
