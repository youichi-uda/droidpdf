# Android PDF ライブラリ 開発仕様書

## プロジェクト概要

| 項目 | 内容 |
|---|---|
| **プロダクト名** | （仮）DroidPDF |
| **形態** | Android向けKotlinライブラリ（.aar） |
| **配布** | GitHub公開 + JitPack / Maven Central |
| **ライセンスモデル** | 個人・非商用：無料 / 商用：有料ライセンスキー制 |
| **決済** | Gumroad（購入後ライセンスキー自動発行） |
| **言語** | Kotlin |
| **最低対応API** | Android API 26（Android 8.0） |

---

## コンセプト

> **「iText7 Androidの商用ライセンス代替を、年間$99で提供するMIT互換PDFライブラリ」**

### 市場の空白

| カテゴリ | 既存の選択肢 | 問題 |
|---|---|---|
| 無料OSS | PdfiumAndroid / AndroidPdfViewer | **表示のみ**、操作機能なし |
| 商用SDK | iText7 / Apryse / ComPDFKit | **要見積もり高額**、個人開発者には手が出ない |
| **空白地帯** | **操作系・商用利用可・低価格** | **←ここを取りに行く** |

iText7が「AGPL or 要見積もり高額商用ライセンス」の二択を迫るのに対し、
本ライブラリは**$99/年のインディープランから**商用利用可能にする。

---

## ライセンスモデル詳細

### Business Source License（BSL）方式

```
個人・非商用・OSS開発 → 無料（ライセンスキー不要）
商用アプリへの組み込み → 有料ライセンスキーが必要
```

- コードはGitHubで全公開（誰でも読める・フォーク可）
- ライセンスキーはGumroad発行のキーをそのまま使用
- キーなしでも全機能動作する（技術的制限なし）
- キー未設定時は初回の`PdfDocument`生成時にワーニングログを1回出力
- 商用利用でのキー未設定はライセンス違反となる

### 価格プラン

| プラン | 価格 | 対象 |
|---|---|---|
| **Personal** | 無料 | 個人学習・非商用・OSS |
| **Commercial** | $99 / 年 | 商用アプリへの組み込み（規模問わず） |

---

## 機能仕様

### Phase 1：コア機能（iText7 Androidの主要機能を網羅）

#### PDF生成
- 新規PDFドキュメント作成
- テキスト追加（フォント・サイズ・色・配置）
- 日本語を含む多言語フォント埋め込み
- 画像追加（JPEG / PNG / WebP）
- 表（テーブル）描画
- ページサイズ指定（A4 / Letter / カスタム）
- ヘッダー・フッター追加
- ウォーターマーク（テキスト・画像）

#### PDFページ操作
- 複数PDFの結合（merge）
- PDFの分割（split）
- ページの抽出
- ページの回転
- ページの並び替え
- ページの削除・挿入

#### 注釈（Annotation）
- テキストハイライト
- 下線・取り消し線
- フリーテキスト注釈
- スタンプ注釈
- 図形注釈（矩形・円・線）
- インク注釈（手書き）
- リンク注釈

#### フォーム（AcroForm）
- テキストフィールドの読み取り・書き込み
- チェックボックスの操作
- ラジオボタンの操作
- ドロップダウンの操作
- フォームデータのフラット化（flatten）

---

### Phase 2：拡張機能

#### セキュリティ
- パスワード保護（128bit / 256bit AES暗号化）
- パスワード解除
- 権限設定（印刷禁止・コピー禁止等）

#### テキスト処理
- テキスト抽出
- テキスト検索（座標付き）
- テキストの置換（既存コンテンツへの上書き）

#### その他
- PDF圧縮・最適化
- PDF/A準拠チェック
- メタデータの読み書き（タイトル・作者・作成日等）

---

### 対象外（v1.0スコープ外）

- デジタル署名（PKI・証明書）
- OCR
- HTML → PDF変換
- XFAフォーム対応
- iOS対応

---

## 技術仕様

### 技術スタック

| 項目 | 選択 |
|---|---|
| **言語** | Kotlin |
| **PDFオブジェクトモデル・Reader/Writer** | 自前実装（ISO 32000-1 / PDF 1.7準拠） |
| **フォントパース・サブセッティング** | Apache FontBox（Apache 2.0） |
| **画像処理** | Android標準（BitmapFactory / Canvas） |
| **圧縮** | java.util.zip（Flate/Deflate） |
| **暗号化** | javax.crypto（AES 128/256） |
| **レイアウトエンジン** | 自前実装 |
| **ライセンスキー検証** | Gumroadキーのフォーマットチェック（オフライン） |
| **テスト** | JUnit5 + Robolectric |
| **配布形式** | .aar（Android Archive） |

### 設計方針

| 項目 | 方針 |
|---|---|
| **仕様外PDFの扱い** | Lenient（最善努力でパースし、読めない部分はスキップ/ワーニング） |
| **メモリ戦略** | ページ単位の遅延読み込み（必要なページだけメモリに載せる） |
| **iText7 API互換** | 概念・構造は揃えるが、名前は独自。移行ガイドで対応表を提供 |
| **スレッドセーフティ** | 単一スレッド前提（v1）。ドキュメントに明記 |

### 開発中に決めること（TODO）

- [ ] テスト戦略の詳細（PDF出力の検証方法等）→ Phase 1aのパーサー実装時に具体化
- [ ] ProGuard/R8難読化ルールの提供 → aar作成時に対応
- [ ] バージョニング戦略（SemVer想定）→ リリース前に確定
- [ ] minSdk 26の妥当性検証 → 開発中に下げられそうなら下げる
- [ ] ドキュメント生成（Dokka想定）→ Phase 4で整備

### ファイル構成

```
droidpdf/
├── library/
│   ├── src/main/kotlin/com/droidpdf/
│   │   ├── core/
│   │   │   ├── PdfDocument.kt          # ドキュメント本体
│   │   │   ├── PdfPage.kt              # ページ操作
│   │   │   ├── PdfReader.kt            # 既存PDF読み込み
│   │   │   └── PdfWriter.kt            # PDF書き出し
│   │   ├── content/
│   │   │   ├── PdfCanvas.kt            # 低レベル描画
│   │   │   ├── PdfFont.kt              # フォント管理
│   │   │   └── PdfImage.kt             # 画像埋め込み
│   │   ├── layout/
│   │   │   ├── Document.kt             # 高レベルレイアウトAPI
│   │   │   ├── Paragraph.kt
│   │   │   ├── Table.kt
│   │   │   └── Image.kt
│   │   ├── forms/
│   │   │   └── PdfAcroForm.kt          # フォーム操作
│   │   ├── annotations/
│   │   │   └── PdfAnnotation.kt        # 注釈操作
│   │   ├── manipulation/
│   │   │   └── PdfMerger.kt            # 結合・分割・抽出
│   │   ├── security/
│   │   │   └── PdfEncryption.kt        # 暗号化
│   │   └── license/
│   │       └── LicenseValidator.kt     # ライセンスキー検証
│   └── src/test/
├── sample-app/                         # 使用例アプリ
├── docs/                               # APIドキュメント
└── LICENSE
```

---

## APIデザイン方針

iText7のAPIに近い設計にすることで、**移行コストを最小化**する。
iText7ユーザーがほぼそのまま乗り換えられるAPIを目指す。

### 使用例イメージ

```kotlin
// ライセンス初期化（商用利用時）
DroidPDF.initialize(context, licenseKey = "your-license-key")

// PDF生成
val writer = PdfWriter(outputStream)
val pdf = PdfDocument(writer)
val document = Document(pdf)

document.add(Paragraph("Hello, DroidPDF!").setFontSize(16f))
document.add(
    Table(3).apply {
        addCell("Name")
        addCell("Age")
        addCell("City")
    }
)
document.close()

// PDF結合
val merger = PdfMerger(outputPdf)
merger.merge(inputPdf1, 1, 3)  // 1〜3ページ
merger.merge(inputPdf2)         // 全ページ
merger.close()

// 注釈追加
val pdf = PdfDocument(PdfReader(inputStream), PdfWriter(outputStream))
val annotation = PdfTextAnnotation(Rectangle(100f, 700f, 200f, 800f))
    .setContents("レビューコメント")
    .setColor(ColorConstants.YELLOW)
pdf.firstPage.addAnnotation(annotation)
pdf.close()
```

---

## 収益化インフラ

### Gumroad販売フロー

```
1. 開発者がGumroad上でプラン選択・決済
2. Gumroadがライセンスキーを自動発行
3. 開発者がライブラリにキーをセット
4. ライブラリがオフラインでキーフォーマットを検証
5. 年間更新はGumroad上で管理
```

### ライセンスキーの動作

- キーセット済み → 通常動作（ログなし）
- キー未設定 → 全機能動作するが、初回の`PdfDocument`生成時にワーニングログを1回出力

```
W/DroidPDF: No license key set. Commercial use requires a license.
W/DroidPDF: Purchase at https://y1uda.gumroad.com/l/DroidPDF
```

### インフラ構成（最小構成）

| コンポーネント | 選択肢 |
|---|---|
| **販売・決済・ライセンス管理** | Gumroad |
| **ドキュメントサイト** | GitHub Pages or Next.js（静的サイト） |

---

## 開発優先順位

| フェーズ | 内容 | 備考 |
|---|---|---|
| **Phase 1a** | PDFパーサー・ライター基盤（低レベルコア） | 最重要、全機能の土台 |
| **Phase 1b** | テキスト・画像・テーブルのPDF生成 | iText7の最も使われる機能 |
| **Phase 1c** | ページ操作（結合・分割・回転） | 需要が高い |
| **Phase 2a** | 注釈・フォーム操作 | iText7移行ユーザーに必須 |
| **Phase 2b** | 暗号化・テキスト抽出 | |
| **Phase 3** | ライセンスキー検証・Gumroad設定・ドキュメントサイト | リリース準備 |
| **Phase 4** | GitHub公開・JitPack登録・ドキュメント整備 | リリース |

---

## 競合優位性まとめ

| 比較軸 | iText7 Android | DroidPDF |
|---|---|---|
| **ライセンス** | AGPL or 要見積もり高額 | 個人無料 / $99〜 |
| **価格透明性** | 不透明（要営業コンタクト） | Webで即購入可 |
| **Android最適化** | Javaの移植版（重め） | Kotlinネイティブ |
| **API互換性** | - | iText7互換APIで移行容易 |
| **コード公開** | OSS（AGPL） | OSS（BSL） |

---

## itch.io販売との関係

本ライブラリはitch.ioではなく**GitHub + 自社サイト**での販売が適切。
itch.ioはゲーム・ツール向けで、開発者向けライブラリの購入者層とずれる。

Gumroadで販売することでitch.ioの10%より手数料を抑えられる（Gumroad手数料：10%、ただしインフラ構築・運用コストがゼロ）。
