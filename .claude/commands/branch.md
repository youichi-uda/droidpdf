# ブランチ作成

開発ブランチを命名規則に沿って作成してください。

## 命名規則

```
feature/{phase}-{機能名}    # 新機能開発
fix/{issue番号}-{概要}      # バグ修正
refactor/{対象}             # リファクタリング
```

例:
- `feature/phase1a-pdf-parser`
- `feature/phase1b-text-rendering`
- `fix/12-cjk-font-subsetting`
- `refactor/pdf-object-model`

## 手順

1. 現在のブランチとステータスを確認
2. 未コミットの変更があれば警告
3. mainブランチから新しいブランチを作成

$ARGUMENTS にブランチの目的を指定してください（例: `/branch Phase1aのPDFパーサー実装`）
