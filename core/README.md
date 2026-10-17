# OpenRocket Core Jar Package

このフォルダーは、React + CheerpJ から利用する OpenRocket Core の配布用パッケージです。
Swing GUI は含めず、upstream OpenRocket の `core` モジュールと、ブラウザー起動用の
`CoreBridge` だけを含みます。

## Contents

```text
openrocket-core-jar-package/
├── artifacts/
│   ├── openrocket-core-runtime.jar  # upstream Core Jar
│   ├── openrocket-core.jar          # runtime Jar の安定名
│   ├── openrocket-core-bridge.jar   # CoreBridge の実行 Jar
│   ├── runtime-manifest.json
│   └── lib/                         # Core の runtime 依存 Jar
├── src/bridge/                      # 配布用 Java Bridge の完全なソース
├── upstream/
│   ├── core/src/                    # Core Jar を構成する Java/resource ソース
│   ├── core/build.gradle
│   ├── build.gradle
│   ├── settings.gradle
│   └── gradle wrapper
├── scripts/
│   └── build-package.sh
└── README.md
```

`upstream/core/src` には、Core の Java ソース、組み込みデータベース、サンプル、
翻訳、設定ファイルなど、Core Jar に入るリソースを含めています。
パッケージ内の `upstream/settings.gradle` と `upstream/build.gradle` は Core 専用の
最小構成です。Swing プロジェクトは含めず、実際に実行するビルドタスクは
`:core:jar` だけです。

## そのまま利用する場合

CheerpJ からは `artifacts` 以下をまとめて Web サーバーの `/engine` に配置します。
`artifacts/lib` の依存 Jar は省略しないでください。

```text
/engine/
├── openrocket-core-runtime.jar
├── openrocket-core-bridge.jar
├── runtime-manifest.json
└── lib/*.jar
```

Bridge のメインクラスは次のとおりです。

```text
com.example.openrocket.browser.CoreBridge
```

対応している操作:

```text
smoke
inspect <input.ork>
roundtrip <input.ork> <output.ork>
simulate <input.ork> [simulation-index]
```

`CoreBridge` は起動時に upstream の `OpenRocketCore.initialize()` を呼び出し、
`.ork` の読み込み・保存、ドキュメント情報の取得、既存 Simulation の実行を
OpenRocket Core API で処理します。結果は JSON として出力されます。

## 再ビルド

JDK 17 以上とネットワーク接続可能な Gradle 実行環境が必要です。

```bash
cd openrocket-core-jar-package
bash scripts/build-package.sh
```

生成先は `artifacts/` です。upstream の依存解決結果から `artifacts/lib` と
`runtime-manifest.json` を毎回再生成します。

再ビルド時に使用する Core ソースは `upstream/core/src`、Bridge ソースは
`src/bridge` です。Gradle キャッシュを利用するため、初回は依存取得に時間が
かかる場合があります。

## 検証済みの機能

- 新規 OpenRocket document の初期化
- 実 OpenRocket `.ork`（ZIP 形式）の読み込み
- `.ork` の roundtrip 保存
- 既存 Simulation の実行
- 最大高度、最大速度、飛行時間などの FlightData の取得
- React/Vite 側の TypeScript ビルド

## License / provenance

`upstream/` は OpenRocket upstream の Core モジュールを元にしています。
OpenRocket および同梱依存ライブラリのライセンス条件を確認したうえで利用してください。
upstream のライセンス本文は [upstream/LICENSE.TXT](upstream/LICENSE.TXT) にあります。
このパッケージは OpenRocket 公式の配布物ではありません。
