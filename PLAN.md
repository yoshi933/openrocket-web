# OpenRocket Web移植プロジェクト

## 詳細技術仕様書・プロジェクト計画書

**版:** 1.5（第2版）
**作成日:** 2026-10-01
**対象:** 学生個人開発／OSS公開を想定したブラウザ版OpenRocketクライアント
**目標:** サーバー側に設計データを送信せず、静的ホスティング上で動作するWebアプリケーションとして、OpenRocketの`.ork`読込・基本シミュレーション・結果可視化を提供する。

> 本書は、添付された初期案を基に、実装可能性・技術的リスク・受入基準・ロードマップ・運用方針を追加した更新版である。特に、Java実行方式、JavaScript連携API、ライセンス、無料ホスティングの制約は、実装開始前のPoCで検証し、未検証のAPIを製品仕様として固定しない。

---

## 1. エグゼクティブサマリー

### 1.1 目指すプロダクト

ユーザーがブラウザ上で次の操作を行える、インストール不要のOpenRocket Webクライアントを構築する。

1. `.ork`ファイルをドラッグ＆ドロップまたはファイル選択で読み込む

1. ロケット構成、ステージ、主要パーツ、モーター、シミュレーション設定を確認する

1. ブラウザ内でシミュレーションを実行する

1. 高度・速度・加速度・時間などの結果をインタラクティブなグラフで確認する

1. 可能な範囲でロケットの2D／3Dプレビューを表示する

1. 結果をCSV／JSONとしてローカル保存する

### 1.2 初期リリースの非目標

初期版では、次を実装対象外とする。

- ユーザーアカウント、ログイン、クラウド保存

- サーバー側シミュレーション

- オンライン共同編集

- モーター・気象データの外部API連携

- OpenRocketデスクトップ版の全UI再現

- 高度な設計編集機能の完全移植

- 実機飛行の安全性・性能を保証する機能

### 1.3 成功条件

MVP（Minimum Viable Product）は、次の状態を満たした時点とする。

- 代表的なサンプル`.ork`を3種類以上読み込める

- 読込後にロケット名、ステージ数、主要コンポーネント数を表示できる

- 1件以上のシミュレーションをブラウザ内で実行できる

- 時系列結果をグラフ表示できる

- 同一入力をデスクトップ版OpenRocketで実行した結果と、主要指標が許容誤差内に収まる

- `.ork`ファイルやシミュレーション結果がサーバーへアップロードされないことを確認できる

- GitHub Pages等の静的ホスティングへ自動デプロイできる

---

## 2. 前提・制約・設計原則

### 2.1 前提

- 開発者は学生1名を基本とし、週8〜12時間程度を確保する

- 対象ブラウザは、現行のChrome／Edge／Firefox／Safariを優先する

- まずデスクトップ幅を主対象とし、スマートフォンは閲覧・簡易操作を段階的に対応する

- バックエンドなしのSPAとして構築する

- `.ork`ファイルはユーザー端末のメモリまたはブラウザ内仮想ファイルシステムで処理する

### 2.2 制約

1. **Javaランタイムのブラウザ実行が最大リスクである。** OpenRocketのJavaコアをCheerpJで動かせるとは限らない。依存ライブラリ、ファイルI/O、リフレクション、スレッド、GUI依存、浮動小数点挙動をPoCで確認する。

1. **JavaScript連携APIはバージョン固定しない。** CheerpJ公式ドキュメントには、JavaScript側のnativeメソッド実装と`cheerpjInit`／`cheerpjRunJar`を用いた例があるが、初期案に記載された`cjCall`をそのまま採用することは前提にしない。

1. **完全無料は利用量に依存する。** GitHub Pagesは静的ホスティングとして無料で利用できる一方、公開サイト1GB、推奨ソースリポジトリ1GB、ソフト帯域上限100GB/月などの制約がある。CheerpJランタイムやJARが大きい場合はサイズを継続的に監視する。

1. **OpenRocketのライセンス確認が必要である。** OpenRocket本体はGPLv3で公開されているため、コアコードを改変・再配布する場合のライセンス表示、ソース提供、派生物の扱いをリリース前に確認する。

1. **計算結果は安全保証ではない。** 本アプリは教育・設計支援用途であり、実機の打上げ可否、安全性、法令適合を保証しない。

### 2.3 設計原則

- **MVP優先:** 3Dや高度なエディタより、`.ork`読込からグラフ表示までを先に完成させる

- **計算エンジンとUIの分離:** Java／OpenRocket層をReactコンポーネントから直接呼ばない

- **契約ベース連携:** JavaScriptと計算エンジンの間に型付きアダプタを置く

- **失敗を可視化:** 初期化中、読込中、計算中、失敗時の状態をUIで明確に表示する

- **データ最小化:** サーバー送信、計測、外部APIをMVPでは行わない

- **再現可能なビルド:** Javaコア、CheerpJ設定、フロントエンドを固定バージョンでビルドする

- **段階的なフォールバック:** Java実行方式が成立しない場合に備え、ブラウザ向け計算エンジン移植またはWeb Worker化を代替案として残す

---

## 3. システムアーキテクチャ

### 3.1 論理構成

```
┌─────────────────────────────────────────────────────────────┐
│ Browser                                                     │
│                                                             │
│  React UI                                                    │
│  ├─ FileDrop / FilePicker                                   │
│  ├─ RocketSummary                                            │
│  ├─ SimulationForm                                           │
│  ├─ ResultCharts                                             │
│  ├─ 2D/3D Preview                                            │
│  └─ Error / Progress / Help                                  │
│          │                                                   │
│          ▼                                                   │
│  Application Services                                        │
│  ├─ ProjectStore (状態管理)                                  │
│  ├─ RocketFileService                                        │
│  ├─ SimulationService                                        │
│  ├─ ExportService                                             │
│  └─ FeatureFlags                                              │
│          │                                                   │
│          ▼                                                   │
│  Engine Adapter                                               │
│  ├─ CheerpJAdapter（第一候補）                                │
│  ├─ WorkerBridge（UIスレッド分離）                            │
│  └─ FallbackEngineAdapter（将来候補）                         │
│          │                                                   │
│          ▼                                                   │
│  OpenRocket Core / Browser Runtime                            │
│  ├─ .ork parser                                               │
│  ├─ Rocket model                                              │
│  └─ Simulation engine                                         │
│                                                             │
│  Browser Storage（任意）                                     │
│  └─ IndexedDB：明示的に保存したローカルプロジェクトのみ       │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
              Static hosting only
       GitHub Pages（第一候補）／Vercel（代替）
```

### 3.2 配置単位

| 配置物 | 内容 | 配置先 |
| --- | --- | --- |
| `web-app` | React、TypeScript、CSS、グラフ、UI | 静的ホスティング |
| `openrocket-core` | OpenRocketの計算・モデル・ファイル処理に必要な最小JAR | 静的ホスティングのアセット |
| `cheerpj-runtime` | Java実行用ランタイム | CDNまたは公式推奨配布方式 |
| `sample-projects` | 小容量のサンプル`.ork` | リポジトリまたはリリースアセット |
| `docs` | 使い方、制約、ライセンス、免責事項 | 静的ホスティング |

### 3.3 データフロー：`.ork`読込

1. ユーザーがファイルを選択する

1. MIMEタイプだけでなく、拡張子・ファイルサイズ・バイナリ先頭を検証する

1. 最大ファイルサイズを超える場合は処理を中止する。MVPの初期値は20MBとし、実測に基づき調整する

1. `File.arrayBuffer()`またはストリーム方式でブラウザ内へ読み込む

1. Engine Adapterが仮想ファイルシステムへ配置する

1. OpenRocket Loaderを呼び出し、ドメインモデルへ変換する

1. UI用の正規化済みDTOへ変換する

1. 生のJavaオブジェクトをReact状態へ渡さず、シリアライズ可能なDTOのみ保持する

1. 成功時はファイル名、ロケット名、構成概要、警告を表示する

1. 失敗時は原因分類と再試行方法を表示し、内部スタックトレースは開発者ログに限定する

### 3.4 データフロー：シミュレーション

1. UIでシミュレーション条件を入力する

1. `SimulationRequest`をバリデーションする

1. Worker経由でEngine Adapterへ送る

1. OpenRocketのシミュレーションエンジンを実行する

1. 結果を標準DTOに変換する

1. UIに進捗または完了通知を返す

1. Chart AdapterがPlotly等の表示用データへ変換する

1. 結果を画面表示し、ユーザー操作でCSV／JSONへエクスポートする

---

## 4. 技術スタック選定

### 4.1 採用候補

| レイヤー | 第一候補 | 採用理由 | 注意点 |
| --- | --- | --- | --- |
| 言語 | TypeScript 5.x | UI、DTO、状態遷移の型安全性 | `any`を原則禁止 |
| UI | React 18/19系の安定版 | コンポーネント分割、学習資産、エコシステム | 依存を増やし過ぎない |
| ビルド | Vite | 高速開発、静的配布との相性 | base path設定が必要 |
| スタイル | Tailwind CSS | レスポンシブ、設計速度 | クラス過多を避ける |
| UI部品 | shadcn/ui相当の自前採用部品 | 依存を理解しながら使える | ライセンス・生成コードを確認 |
| 状態管理 | ZustandまたはReducer + Context | 小規模SPAに適する | サーバー状態管理は不要 |
| バリデーション | Zod | ファイル・フォーム・DTO検証 | Java側の検証も併用 |
| グラフ | Plotly.js | ズーム、ホバー、複数系列、CSV連携 | バンドルサイズを確認 |
| 3D | Three.js + React Three Fiber | 将来のプレビュー拡張に適する | MVP後に導入 |
| Java実行 | CheerpJの検証済み版 | 既存Javaコア活用の可能性 | バージョン・ライセンス・互換性をPoCで固定 |
| 並列化 | Web Worker | 計算中のUIフリーズ回避 | JavaランタイムのWorker対応を検証 |
| テスト | Vitest、React Testing Library、Playwright | 単体・UI・E2Eを分担 | 実ブラウザでのCheerpJテストを別枠管理 |
| 品質 | ESLint、Prettier、TypeScript strict | 個人開発でも品質を自動化 | CIで必須化 |
| CI/CD | GitHub Actions | 無料枠とリポジトリ連携 | 秘密情報を持ち込まない |
| ホスティング | GitHub Pages | 静的配布、OSSとの親和性 | 帯域・容量・利用規約を監視 |
| 代替ホスティング | Vercel | プレビュー環境が作りやすい | 利用規約・プラン変更を確認 |

### 4.2 技術選定の結論

初期実装は **Vite + React + TypeScript + Tailwind CSS + Plotly.js + Vitest + Playwright + GitHub Actions + GitHub Pages** を標準とする。

ただし、CheerpJは「確定済みの計算エンジン」ではなく、**Time-boxed PoCで採否判定する基盤候補**と位置付ける。PoC失敗時の代替案は次の優先順とする。

1. OpenRocket coreの依存を整理し、CheerpJで動く最小サブセットへ縮小

1. Javaコアの必要ロジックをWebAssembly／TypeScriptへ段階的に移植

1. サーバーなしを維持しつつ、既存のブラウザ実行可能な数値計算実装を採用

1. サーバー実行へ移行する場合は、無料要件・プライバシー要件を再合意する

### 4.3 バージョン固定方針

- Node.js：LTS

- Java：OpenRocketのビルド要件に合わせて固定

- OpenRocket：特定コミットまたはリリースタグを固定

- CheerpJ：PoCで検証したバージョンを固定

- npm依存：lockfileをコミット

- CDN依存：可能な限りSubresource Integrityまたはセルフホストを利用

---

## 5. 機能仕様

### 5.1 ファイル読込

**対応:** `.ork`、MVPでは単一ファイル。
**操作:** ファイル選択、ドラッグ＆ドロップ、サンプル読込。
**上限:** 初期20MB、設定値として変更可能。
**保持:** 明示的に保存しない限り、ページメモリ内のみ。

受入条件:

- 拡張子不一致、空ファイル、破損ファイル、大容量ファイルに適切なエラーを表示する

- 同一ファイルを再読込しても状態が破綻しない

- 読込中に別ファイルを選択した場合、古い処理をキャンセルまたは無効化する

- ファイル内容がネットワークへ送信されない

### 5.2 ロケット概要

表示項目:

- ファイル名

- ロケット名

- ステージ数

- 全長、質量、重心、圧力中心

- コンポーネント階層

- モーター／シミュレーション設定の概要

- OpenRocket側から取得できない場合の`未取得`表示

### 5.3 シミュレーション設定

MVPで扱う入力:

- 対象シミュレーション

- モーター選択

- 発射角度

- 発射レール長

- 大気条件の基本値

- 風速または無風設定

- 結果サンプリング間隔

入力仕様:

- 単位をラベルで明示する

- 数値範囲をZodで検証する

- 単位変換をUI層で吸収し、Engine Adapterには標準単位を渡す

- 不正値にはフィールド単位のエラーを表示する

### 5.4 結果可視化

必須系列:

- 時間–高度

- 時間–速度

- 時間–加速度

任意系列:

- 質量

- 動圧

- マッハ数

- 姿勢角

- 推力

操作:

- 系列の表示／非表示

- 範囲選択、ズーム、リセット

- ホバーによる正確な値表示

- 単位表示

- CSV／JSONエクスポート

### 5.5 2D／3D表示

MVPは2D簡易プレビューを優先する。ロケットの構成DTOから、ノーズコーン、ボディチューブ、フィン、モーターなどを簡略形状として描画する。

3Dは以下の条件を満たしてから実装する。

- DTOに形状・寸法が安定して含まれる

- 主要サンプルで表示崩れがない

- 3D表示がシミュレーション実行を遅くしない

- モバイルで強制的に重いWebGLを起動しない

### 5.6 ローカル保存

初期版では保存操作を明示的に行った場合のみIndexedDBを利用する。保存対象は次のいずれかとする。

- 元ファイルのBlob

- 正規化済みプロジェクトJSON

- 直近のシミュレーション結果

削除・全消去機能、容量超過時のエラー表示、プライバシー説明を必須とする。

---

## 6. 内部インターフェース仕様

### 6.1 Engine Adapter

```
export interface EngineAdapter {
  initialize(): Promise<EngineStatus>;
  loadRocket(input: RocketFileInput): Promise<RocketSummary>;
  listSimulations(): Promise<SimulationDefinition[]>;
  runSimulation(request: SimulationRequest): Promise<SimulationResult>;
  dispose(): Promise<void>;
}
```

### 6.2 DTO例

```
export type RocketFileInput = {
  name: string;
  size: number;
  bytes: ArrayBuffer;
  lastModified?: number;
};

export type SimulationRequest = {
  simulationId: string;
  launchAngleDeg?: number;
  rodLengthM?: number;
  windSpeedMps?: number;
  samplingIntervalSec?: number;
};

export type SimulationResult = {
  metadata: {
    engine: string;
    engineVersion?: string;
    completedAt: string;
    warnings: string[];
  };
  series: Array<{
    name: string;
    unit: string;
    values: number[];
  }>;
  timeSec: number[];
};
```

### 6.3 エラー分類

| コード | 意味 | UI対応 |
| --- | --- | --- |
| `ENGINE_INIT_FAILED` | Java／CheerpJ初期化失敗 | 再読込、対応ブラウザ案内 |
| `UNSUPPORTED_FILE` | 非対応形式 | 対応形式を表示 |
| `FILE_TOO_LARGE` | サイズ上限超過 | 上限値を表示 |
| `PARSE_FAILED` | `.ork`解析失敗 | ファイル破損・版違いを案内 |
| `SIMULATION_INVALID` | 入力値不正 | フィールドエラー |
| `SIMULATION_FAILED` | 計算失敗 | 条件変更・ログ取得案内 |
| `TIMEOUT` | 処理時間超過 | キャンセル・再試行 |
| `OUT_OF_MEMORY` | ブラウザメモリ不足 | ファイル縮小・タブ整理案内 |

### 6.4 非同期・キャンセル

- 初期化、読込、計算はすべてPromiseベースとする

- 計算要求にはリクエストIDを付与する

- 新しい要求が来た場合、古い結果をUIへ反映しない

- 可能なら`AbortController`とWorker終了を使ってキャンセルする

- 長時間計算では進捗イベントを表示する

---

## 7. セキュリティ・プライバシー・ライセンス

### 7.1 ファイル安全性

`.ork`ファイルはユーザー入力として扱う。XML／ZIP等の内部構造を直接DOMへ挿入せず、表示文字列は必ずエスケープする。ブラウザ内Javaランタイムがファイルを処理する範囲、外部ネットワークアクセスの有無、仮想ファイルシステムの挙動をPoCで確認する。

### 7.2 送信しないデータ

MVPでは、以下を外部へ送信しない。

- `.ork`ファイル

- ロケット名、設計データ

- シミュレーション結果

- IPアドレス以外のアクセスログを自前で収集する仕組み

- 個人識別情報

外部CDNを使う場合、ランタイム取得のためのネットワーク通信が発生し得ることをプライバシー説明に記載する。

### 7.3 ライセンス

OpenRocketはGPLv3のオープンソースプロジェクトであり、本プロジェクトがコアコードを改変・再配布する場合は、次をリリースチェックリストに含める。

- LICENSEファイルの同梱

- 著作権表示とOpenRocketへの帰属表示

- 変更点の明示

- 対象バージョン・コミットの記録

- GPLv3に基づくソースコード提供方法の整理

- CheerpJ、Plotly.js、Three.js等の依存ライセンス一覧

- OpenRocket公式と無関係な派生Webアプリである旨の明示

法的判断が必要な場合は、OSSライセンスに詳しい専門家へ確認する。

### 7.4 免責表示

画面とREADMEに次の趣旨を記載する。

> 本ソフトウェアは教育・研究・設計補助を目的とした非公式Webクライアントです。計算結果の正確性、実機の安全性、法令適合、打上げ成功を保証しません。実際のロケット製作・打上げは、関連法令・施設規則・専門家の指導に従ってください。

---

## 8. 品質保証・テスト計画

### 8.1 テスト階層

| 層 | 対象 | 目標 |
| --- | --- | --- |
| Unit | 単位変換、DTO変換、入力検証、エラー分類 | 高いカバレッジを目指す |
| Component | ファイル選択、フォーム、グラフ状態、エラー表示 | 主要状態を網羅 |
| Adapter | JavaScript–CheerpJ境界、型変換、ファイル配置 | 代表ケースを固定 |
| E2E | 起動→読込→実行→グラフ→エクスポート | MVPの主要導線を毎回確認 |
| Compatibility | Chrome、Firefox、Safari、Edge | 対象ブラウザの主要版 |
| Performance | 起動、読込、計算、描画、メモリ | 目標値を継続測定 |
| Regression | OpenRocketデスクトップ版との比較 | 主要指標の差分監視 |

### 8.2 目標性能

初期目標値。実測後に更新する。

- 初期HTML表示：3秒以内（一般的な固定回線）

- エンジン初期化：30秒以内、または進捗表示あり

- 5MB以下の`.ork`読込：10秒以内

- 標準サンプルのシミュレーション：20秒以内

- UIスレッドの長時間ブロック：500msを超えない

- 配布アセット総量：GitHub Pages公開サイト1GB制限を十分下回る。目標は初期100MB未満

### 8.3 数値比較

デスクトップ版OpenRocketで基準結果を生成し、同一の`.ork`と条件をブラウザ版へ入力する。次の指標を比較する。

- 最大高度

- 最大速度

- 最大加速度

- 到達時間

- 着地またはシミュレーション終了時の状態

許容誤差は、まず相対誤差1%または各指標に定めた絶対誤差の大きい方を仮置きし、単位変換・サンプリング間隔・浮動小数点差を分析した上で決定する。安全性を意味する閾値ではない。

---

## 9. リポジトリ構成案

```
openrocket-web/
├─ apps/
│  └─ web/
│     ├─ src/
│     │  ├─ app/
│     │  ├─ components/
│     │  ├─ features/
│     │  │  ├─ file-import/
│     │  │  ├─ rocket-summary/
│     │  │  ├─ simulation/
│     │  │  ├─ charts/
│     │  │  └─ preview/
│     │  ├─ engine/
│     │  ├─ workers/
│     │  ├─ lib/
│     │  └─ types/
│     ├─ public/
│     │  ├─ samples/
│     │  └─ engine/
│     └─ tests/
├─ engine/
│  ├─ openrocket-core/
│  ├─ patches/
│  ├─ build/
│  └─ README.md
├─ docs/
│  ├─ architecture.md
│  ├─ compatibility.md
│  ├─ licensing.md
│  └─ adr/
├─ scripts/
│  ├─ build-engine.*
│  ├─ verify-assets.*
│  └─ compare-results.*
├─ .github/workflows/
│  ├─ ci.yml
│  └─ deploy-pages.yml
├─ package.json
├─ pnpm-lock.yaml
├─ README.md
└─ LICENSE
```

### 9.1 コミット・ブランチ方針

- `main`: 常にデプロイ可能

- `feature/*`: 機能開発

- `fix/*`: 不具合修正

- 小さくレビュー可能なコミットを作る

- PRテンプレートにテスト結果、スクリーンショット、ライセンス影響を含める

---

## 10. 詳細ロードマップ

### Phase 0：企画確定・環境構築（W1）

**成果物:** リポジトリ、README初版、ADR、開発環境、CIの雛形。

- 目的、MVP、非目標を確定

- OpenRocketの対象バージョンを仮決定

- Node.js、Java、Gradle、CheerpJ検証環境を固定

- React + TypeScript + Viteを起動

- lint、format、typecheck、unit testをCIへ追加

- サンプル`.ork`の利用許諾と出典を整理

**Exit criteria:** 空のSPAがCIを通り、GitHub Pagesへデプロイできる。

### Phase 1：CheerpJ実行可能性PoC（W2–W3）

**成果物:** `hello-cheerpj`、Java–JavaScript連携PoC、PoC報告書。

- 公式サンプルをそのまま動かす

- JavaからJavaScriptへのコールバックを確認

- JavaScriptからJavaメソッドを呼ぶ方式を確認

- 文字列、数値配列、例外、Promise、ファイルI/Oを検証

- Worker内実行の可否を検証

- OpenRocketの最小クラスまたはcoreビルドを読み込む

- 初期化時間とアセットサイズを測定

**Go／No-Go判定:**

- Go：coreの一部が実行でき、必要なデータ交換方式が確認できる

- Conditional Go：依存整理が必要だが4週間以内に解決見込み

- No-Go：Javaコアの主要機能が動かず、代替方式の検討へ移る

### Phase 2：OpenRocket core分離・.ork読込（W4–W7）

**成果物:** 固定コミットのcoreビルド、ファイル読込アダプタ、概要表示。

- `core/`とGUI依存の境界を調査

- 不要なSwing、印刷、デスクトップ連携を除外

- 必要な依存JARを一覧化

- `.ork`読込を最小APIにラップ

- RocketSummary DTOを定義

- 破損、巨大、未知バージョンのエラー処理

- サンプル3種類で回帰テスト

**Exit criteria:** `.ork`からロケット名、構成、シミュレーション候補を取得できる。

### Phase 3：シミュレーションパイプライン（W8–W11）

**成果物:** SimulationService、Worker連携、結果DTO、比較テスト。

- SimulationRequest／Resultを確定

- 入力単位を統一

- Java結果をFloat64Arrayまたはシリアライズ可能な配列へ変換

- Workerまたは代替非同期方式を実装

- キャンセル、タイムアウト、重複要求を処理

- デスクトップ版との基準比較を自動化

- 主要3指標の誤差レポートを出力

**Exit criteria:** 標準サンプルのシミュレーションを再現可能に実行できる。

### Phase 4：MVP UI・グラフ（W12–W15）

**成果物:** 読込からグラフ表示までの利用可能なSPA。

- ドロップゾーン、ファイル選択、サンプルボタン

- ローディング、進捗、エラー、空状態

- ロケット概要パネル

- シミュレーション条件フォーム

- Plotlyグラフ3種類

- CSV／JSONエクスポート

- 日本語のヘルプと免責事項

- キーボード操作、色だけに依存しないUI

**MVP受入テスト:** 初見ユーザーがREADMEなしで、サンプルを開きグラフを表示できる。

### Phase 5：品質強化・静的公開（W16–W18）

**成果物:** 公開版0.1、ドキュメント、互換性表。

- Playwright E2E

- Chrome／Firefox／Safari／Edgeで確認

- GitHub Pagesのbase path対応

- アセットサイズ測定とバンドル分析

- Lighthouse等による基本性能確認

- LICENSE、NOTICE、OSS一覧

- 公開URL、障害時の案内、問い合わせ方法を整備

### Phase 6：2D／3Dプレビュー（W19–W22）

**成果物:** 実験的3Dプレビュー、機能フラグ。

- コンポーネントDTOを形状DTOへ変換

- 2D断面表示

- Three.js／React Three Fiberの最小導入

- 回転、ズーム、リセット

- WebGL非対応時の静的2Dフォールバック

- 大規模モデルの描画性能確認

### Phase 7：公開後改善（W23以降）

候補機能:

- プロジェクトのIndexedDB保存

- モバイルUI改善

- 複数シミュレーション比較

- PDF／画像出力

- 追加チャート

- 多言語対応

- 公式OpenRocketの変更追従自動化

- 依存更新とセキュリティ監査

---

## 11. リスク管理

| リスク | 可能性 | 影響 | 対策 | 発生時の判断 |
| --- | --- | --- | --- | --- |
| CheerpJでOpenRocket coreが動かない | 高 | 高 | W2–W3でPoC、代替案を維持 | No-Go判定で方式変更 |
| Java–JS型変換が複雑 | 中 | 高 | DTO境界、変換テスト、配列の段階検証 | 文字列JSON経由を一時採用 |
| 計算中にUIが固まる | 中 | 高 | Worker、キャンセル、進捗表示 | Worker不可なら分割処理・警告 |
| JAR／ランタイムが大きい | 中 | 中 | 依存縮小、圧縮、アセット計測 | CDN／代替ホスト検討 |
| OpenRocket更新で互換性が壊れる | 中 | 中 | コミット固定、サンプル回帰テスト | 更新を手動承認 |
| GPLv3対応の不備 | 低〜中 | 高 | NOTICE、ソース提供、専門家確認 | 公開を止めて是正 |
| モバイルブラウザのメモリ不足 | 高 | 中 | ファイル上限、低負荷UI、2Dフォールバック | モバイルを閲覧中心に限定 |
| GitHub Pagesの帯域制限 | 低 | 中 | サイズ監視、利用状況確認、代替ホスト | CDNまたはVercelへ移行 |
| 個人開発の時間不足 | 高 | 高 | MVP固定、週次レビュー、3D後回し | Phase 6以降を削る |

### 11.1 週次リスクレビュー

毎週末に次を確認する。

- 今週のExit criteriaを満たしたか

- 次週の技術的不確実性は何か

- 予定工数と実績の差

- 依存更新・ライセンス変更

- MVPに不要な作業を追加していないか

---

## 12. 開発者向け受入チェックリスト

### MVP必須

- [ ] 初回起動時のランタイム初期化状態が表示される

- [ ] `.ork`の選択とD&Dが動く

- [ ] 読込エラーをユーザー向けに説明できる

- [ ] ロケット概要が表示される

- [ ] シミュレーションを実行できる

- [ ] 高度・速度・加速度のグラフが表示される

- [ ] CSV／JSONをローカル保存できる

- [ ] 計算中にUIが操作不能にならない

- [ ] 代表サンプルで回帰テストが通る

- [ ] GitHub Pagesへデプロイできる

- [ ] ライセンス、出典、免責事項が公開されている

### リリース前必須

- [ ] 本番ビルドで開発用ログを無効化または最小化

- [ ] ネットワーク送信内容を確認

- [ ] アセットサイズを記録

- [ ] 主要ブラウザを確認

- [ ] ページ再読み込み、別ファイル読込、処理失敗からの復旧を確認

- [ ] 変更したOpenRocketコードとソース提供方法を記録

- [ ] 既知の制約をREADMEと画面ヘルプに記載

---

## 13. 成果物一覧

| 時期 | 成果物 |
| --- | --- |
| W1 | 開発環境、リポジトリ、CI雛形、ADR |
| W3 | CheerpJ実行可能性PoC報告書 |
| W7 | coreビルド、.ork読込、RocketSummary |
| W11 | SimulationService、結果比較レポート |
| W15 | MVP SPA、グラフ、エクスポート |
| W18 | 公開版0.1、ライセンス・互換性文書 |
| W22 | 実験的3Dプレビュー |
| 継続 | issue、変更履歴、依存更新記録 |

---

## 14. 推奨する最初の実装チケット

1. `chore: Vite React TypeScript strict project setup`

1. `ci: add lint typecheck unit test workflow`

1. `poc: run official CheerpJ interoperability sample`

1. `poc: validate Java string and numeric array conversion`

1. `poc: load minimal OpenRocket core artifact`

1. `feat: add file drop zone and local file validation`

1. `feat: add EngineAdapter interface and mock adapter`

1. `feat: add RocketSummary DTO and display panel`

1. `feat: run first simulation through adapter`

1. `feat: render altitude time series`

1. `test: add desktop-vs-browser baseline comparison`

1. `deploy: publish preview to GitHub Pages`

モックアダプタを先に実装することで、CheerpJの不確実性が残っていてもUI、状態管理、エラー処理、グラフ表示を並行開発できる。

---

## 15. 技術判断の記録（ADR候補）

### ADR-001：MVPではバックエンドを持たない

**決定:** ユーザーデータと計算をブラウザ内で処理する。
**理由:** サーバー費用、運用、プライバシー、個人開発の複雑性を抑えられる。
**保留:** 大規模計算、共有、認証が必要になった場合は別プロジェクトとして再評価する。

### ADR-002：CheerpJはPoC通過後に正式採用する

**決定:** CheerpJを第一候補とするが、API・バージョン・互換性はPoCで固定する。
**理由:** OpenRocketの全依存、GUI依存、ファイルI/O、数値配列変換が未検証だからである。

### ADR-003：3DはMVP後に実装する

**決定:** まずファイル読込、計算、グラフを完成させる。
**理由:** 3Dは価値が高いが、計算機能の成立を保証しないため、早期の主要リスク解決にならない。

---

## 16. 参考情報

- [OpenRocket公式GitHubリポジトリ](https://github.com/openrocket/openrocket) — `core/`、`swing/`、GPLv3表記、ビルド構成を確認

- [CheerpJ公式 Interoperability Tutorial](https://cheerpj.com/docs/tutorials/interoperability-tutorial.html) — JavaScript nativeメソッド、`cheerpjInit`、`cheerpjRunJar`等の公式例

- [GitHub Pagesの利用制限](https://docs.github.com/en/pages/getting-started-with-github-pages/github-pages-limits) — 公開サイト容量、帯域、ビルド等の制限

- [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.html) — OpenRocket派生・再配布時の確認先

> 参考情報のURLやAPI仕様は将来変更される可能性があるため、実装開始時とリリース前に再確認する。

---

# 17. CheerpJ運用前提版：詳細実装・配布・運用計画

## 17.1 前提の更新

前章まででは、CheerpJによるOpenRocket Coreのブラウザ実行を技術検証項目として扱っていた。本計画では、次の条件を満たすことを確認済み、または実装開始時に確認できるものとして、CheerpJを正式な第一実行基盤として計画する。

- CheerpJが対象ブラウザで初期化できる

- OpenRocket Coreおよび必要な依存JARをブラウザ内Java環境へ読み込める

- Javaブリッジから`.ork`ファイルを読み込める

- OpenRocketのJavaオブジェクトから、ロケット概要とシミュレーション結果をJavaScriptへ返せる

- OpenRocketのSwing／デスクトップUIに依存せず、Coreの処理だけで必要な機能を実行できる

- 主要サンプルでデスクトップ版との数値比較が可能である

> ここでいう「動く」とは、OpenRocketデスクトップ版の画面をそのまま表示することではない。CheerpJ上でOpenRocket Coreを実行し、React／TypeScriptのWeb画面からファイル読込・シミュレーション・結果取得を行えることを意味する。

## 17.2 CheerpJを含む最終実行構成

```
┌──────────────────────────────────────────────────────────────┐
│ Browser                                                      │
│                                                              │
│  React + TypeScript UI                                       │
│  ├─ FileDrop                                                 │
│  ├─ RocketSummary                                             │
│  ├─ SimulationForm                                            │
│  ├─ ResultCharts                                               │
│  └─ Error / Progress / Help                                   │
│          │                                                   │
│          ▼                                                   │
│  OpenRocketEngineAdapter                                      │
│  ├─ request/response ID管理                                   │
│  ├─ DTO変換                                                   │
│  ├─ タイムアウト                                               │
│  └─ エラー分類                                                │
│          │                                                   │
│          ▼                                                   │
│  CheerpJ Bridge Layer                                         │
│  ├─ JavaScript native methods                                 │
│  ├─ Javaメソッド呼出し                                        │
│  ├─ byte[] / JSON / Float64Array変換                          │
│  ├─ 仮想ファイルシステム連携                                  │
│  └─ ランタイム状態管理                                        │
│          │                                                   │
│          ▼                                                   │
│  CheerpJ Runtime                                              │
│  ├─ cheerpjInit                                               │
│  ├─ cheerpjRunJar                                             │
│  └─ Java実行環境                                               │
│          │                                                   │
│          ▼                                                   │
│  OpenRocket Browser Bridge JAR                                │
│  ├─ BrowserBridge                                              │
│  ├─ RocketFileFacade                                            │
│  ├─ SimulationFacade                                            │
│  └─ ResultSerializer                                            │
│          │                                                   │
│          ▼                                                   │
│  OpenRocket Core + 依存ライブラリ                              │
│  ├─ GeneralRocketLoader                                        │
│  ├─ OpenRocketDocument                                         │
│  ├─ Rocket                                                     │
│  └─ Simulation                                                 │
└──────────────────────────────────────────────────────────────┘
```

## 17.3 役割分担

| 部品 | 担当する処理 | 担当しない処理 |
| --- | --- | --- |
| React UI | 入力、ボタン、状態、表示 | Javaオブジェクトの直接操作 |
| `OpenRocketEngineAdapter` | アプリ向けAPI、DTO、タイムアウト | 画面レイアウト |
| CheerpJ Bridge | JavaScriptとJavaの型・呼出し変換 | グラフ描画 |
| Browser Bridge JAR | Core APIを呼ぶ、結果を整形 | React状態管理 |
| OpenRocket Core | `.ork`解析、ロケットモデル、飛行計算 | Web UI |
| Web Worker | 可能な範囲で重い処理を分離 | 永続データ保存 |
| GitHub Actions | ビルド、テスト、アセット検査、公開 | ユーザーデータ処理 |

## 17.4 ブリッジの基本方針

### 17.4.1 Javaブリッジを必ず置く

ReactからOpenRocket Coreのクラスを直接呼び出さず、専用のJavaブリッジを経由する。

```
React
  ↓
TypeScript EngineAdapter
  ↓
CheerpJ JavaScript API
  ↓
BrowserBridge.java
  ↓
OpenRocket Core API
```

Javaブリッジを置く理由:

- OpenRocket Coreの内部変更をUIから隠せる

- JavaオブジェクトをDTOへ変換できる

- 例外をWeb用エラーコードへ変換できる

- JavaScriptに公開するメソッドを最小限にできる

- 将来CheerpJから別実行方式へ変更しやすい

### 17.4.2 公開するJava APIを限定する

ブラウザから呼び出すAPIは、次のような少数のFacadeメソッドに限定する。

```java
public interface BrowserRocketFacade {
    String loadRocketSummary(byte[] orkBytes, String fileName);
    String listSimulations();
    String runSimulation(String requestJson);
    String getEngineInfo();
}
```

実際のJavaコードでは、CheerpJの相互運用方式に合わせて公開形式を調整する。最初からOpenRocket Coreの全クラスをJavaScriptへ公開しない。

### 17.4.3 JSONを境界に使う

初期実装では、JavaScriptとJavaの境界にJSON文字列を使う。

```
JavaScript request object
        ↓ JSON.stringify
JSON文字列
        ↓ CheerpJ
Java String
        ↓ JSONパーサー
Java request object
```

結果も同じ方法で返す。

```
Java result object
        ↓ JSONシリアライズ
Java String
        ↓ CheerpJ
JavaScript string
        ↓ JSON.parse
TypeScript DTO
```

JSONは、数値配列を直接渡す方式よりも初期実装が分かりやすく、デバッグしやすい。性能が問題になった場合だけ、`double[]`、`Float64Array`、バイナリ転送へ最適化する。

## 17.5 CheerpJランタイムの初期化状態

画面では、CheerpJを単なる「読み込み中」とせず、状態を明示する。

```
type EngineStatus =
  | 'not-started'
  | 'loading-runtime'
  | 'initializing-java'
  | 'loading-openrocket'
  | 'ready'
  | 'failed';
```

表示例:

| 状態 | ユーザー表示 | 操作 |
| --- | --- | --- |
| `not-started` | エンジン未起動 | 起動ボタンまたは自動起動 |
| `loading-runtime` | Java実行環境を読み込んでいます | 待機 |
| `initializing-java` | ブラウザ内Javaを初期化しています | 待機 |
| `loading-openrocket` | OpenRocket Coreを読み込んでいます | 待機 |
| `ready` | シミュレーション準備完了 | ファイル選択可能 |
| `failed` | エンジンを起動できませんでした | 再試行・対応ブラウザ案内 |

## 17.6 起動シーケンス

```
1. Reactアプリを表示
2. CheerpJ loaderを読み込む
3. cheerpjInitを実行
4. nativeメソッドを登録
5. Browser Bridge JARを読み込む
6. Java側の初期化メソッドを実行
7. EngineInfoを取得
8. バージョン・依存・機能フラグを確認
9. ready状態へ移行
```

実装上の注意:

- 初期化を複数回実行しない

- 初期化Promiseを共有し、同時に複数の起動処理を走らせない

- 初期化失敗後はリロードなしの再試行を可能にする

- Javaランタイムのロード完了とOpenRocket Coreの準備完了を分けて表示する

- 初期化中はファイル選択とシミュレーションボタンを無効にする

### 17.6.1 初期化Promiseの概念例

```
let engineInitPromise: Promise<void> | undefined;

export function initializeEngine(): Promise<void> {
  if (!engineInitPromise) {
    engineInitPromise = initializeEngineInternal().catch((error) => {
      engineInitPromise = undefined;
      throw error;
    });
  }

  return engineInitPromise;
}
```

この設計により、画面の複数箇所から初期化を要求しても、同じ初期化処理を共有できる。

## 17.7 `.ork`ファイル読込の運用

### 17.7.1 推奨する読込方式

初期実装では、次の順番で対応する。

1. ブラウザでファイルを選択

1. 拡張子・サイズ・空ファイルを検証

1. `ArrayBuffer`として読み込む

1. CheerpJ／Javaブリッジへ渡す

1. Java側で`GeneralRocketLoader`相当の読込処理を行う

1. `RocketSummary` JSONを返す

1. UIで概要を表示

仮想ファイルシステムへ一時保存する方式が必要な場合は、ファイル名にユーザー入力をそのまま使わず、アプリ側で安全な一時名へ変換する。

```
user_rocket.ork
        ↓
/tmp/openrocket-upload-<requestId>.ork
```

### 17.7.2 ファイル管理規則

- 一度に処理するファイルは1件

- 初期上限は20MB

- 読込完了後も不要なArrayBufferを保持し続けない

- 新しい要求が来たら古い要求の結果を無効にする

- エラー時に入力ファイルをサーバーへ送信しない

- 読込後の設計データをIndexedDBへ保存する場合は、ユーザーの明示操作を必要とする

### 17.7.3 読込リクエストのID

```
type RequestId = string;

const requestId = crypto.randomUUID();
```

Javaから返ってきた結果に同じ`requestId`を付け、現在の要求と一致する場合だけ画面へ反映する。

## 17.8 シミュレーション運用

### 17.8.1 JavaScriptからJavaへ渡すリクエスト

```
type SimulationRequest = {
  requestId: string;
  simulationId: string;
  launchAngleDeg?: number;
  rodLengthM?: number;
  windSpeedMps?: number;
  samplingIntervalSec?: number;
};
```

Java側では次を行う。

1. JSONを解析

1. 必須項目を確認

1. 単位と値の範囲を確認

1. OpenRocketのシミュレーション設定へ変換

1. 計算を実行

1. 結果を標準JSONへ変換

1. `requestId`付きで返す

### 17.8.2 シミュレーション結果

```
type SimulationResult = {
  requestId: string;
  engine: {
    name: 'openrocket-core';
    version: string;
    cheerpjVersion?: string;
  };
  timeSec: number[];
  series: Array<{
    name: string;
    unit: string;
    values: number[];
  }>;
  warnings: string[];
};
```

### 17.8.3 計算中の表示

OpenRocket Coreの計算が進捗を返せない場合でも、次の状態は表示する。

- 実行開始

- 計算中

- 結果変換中

- グラフ描画中

- 完了

- 失敗

長時間処理では、経過時間とキャンセルボタンを表示する。

## 17.9 Web Worker運用

### 第一候補

CheerpJとOpenRocket CoreをWeb Worker内で動かせる場合は、シミュレーション処理をWorkerへ分離する。

```
メインスレッド：React、入力、グラフ
Worker：CheerpJ、Javaブリッジ、OpenRocket Core
```

### Worker非対応時の代替

CheerpJがWorker内で安定しない場合は、次の段階的対策を行う。

1. JavaScriptのUI更新を細かく分ける

1. 1回のシミュレーションの最大時間を決める

1. 実行中に操作を制限する

1. タイムアウトを設定する

1. 大き過ぎるファイルを拒否する

1. 「計算中は他の操作が制限される」ことを明示する

Workerを使えるかどうかは、対象ブラウザごとにE2Eテストで確認する。

## 17.10 配布アセットの構成

```
public/
├─ engine/
│  ├─ openrocket-browser-bridge.jar
│  ├─ openrocket-core.jar または統合JAR
│  └─ runtime-manifest.json
├─ samples/
│  ├─ basic.ork
│  └─ multi-stage.ork
└─ legal/
   ├─ LICENSE.txt
   ├─ NOTICE.txt
   └─ THIRD_PARTY_LICENSES.txt
```

### `runtime-manifest.json`の例

```json
{
  "openrocketCoreVersion": "固定したリリースまたはコミット",
  "bridgeVersion": "0.1.0",
  "cheerpjVersion": "4.3",
  "javaVersion": 8,
  "supportedFeatures": [
    "ork-load",
    "rocket-summary",
    "simulation"
  ]
}
```

実際のバージョン値はPoC完了後に固定する。

### アセット配布方針

- CheerpJ公式ランタイムの配布規則に従う

- Coreとブリッジのバージョンを固定する

- 開発版と本番版でJARを混在させない

- アセットのSHA-256をCIで記録する

- 配布サイズをCIで測定する

- 依存ライセンスを公開する

## 17.11 ビルドパイプライン

```
1. OpenRocketの対象コミットをcheckout
2. JDKをセットアップ
3. :core:jarをビルド
4. Browser Bridgeをビルド
5. 必要な依存を統合または配置
6. Java側の単体テスト
7. CheerpJ用アセット検査
8. Reactアプリをビルド
9. Playwrightでブラウザテスト
10. アセットサイズ・ハッシュを出力
11. GitHub Pagesへデプロイ
```

### CIの主要ジョブ

| ジョブ | 内容 | 失敗時 |
| --- | --- | --- |
| `frontend-check` | lint、typecheck、単体テスト | マージ不可 |
| `core-build` | OpenRocket Coreのビルド | マージ不可 |
| `bridge-build` | Javaブリッジのビルド | マージ不可 |
| `cheerpj-smoke` | CheerpJ起動と最小呼出し | リリース不可 |
| `ork-load-e2e` | サンプル読込 | リリース不可 |
| `simulation-e2e` | シミュレーションとグラフ | リリース不可 |
| `asset-audit` | サイズ、ハッシュ、ライセンス | リリース不可 |
| `deploy` | GitHub Pages公開 | 前ジョブ成功時のみ |

## 17.12 バージョン管理

CheerpJを含む構成では、次の組み合わせを1つの互換性セットとして扱う。

```
OpenRocket Coreバージョン
+ Browser Bridgeバージョン
+ CheerpJバージョン
+ Java実行バージョン
+ TypeScript Adapterバージョン
```

互換性セットを変更した場合は、次のテストを必ず実行する。

- CheerpJ起動

- Coreクラス初期化

- `.ork`読込

- 概要DTO取得

- シミュレーション

- 結果JSON変換

- Chrome／Firefox／Edge／Safari

## 17.13 具体的なリリース段階

### Release 0.0.1：CheerpJ Hello

目的：JavaとJavaScriptの連携を確認する。

含めるもの:

- CheerpJ初期化

- Javaメソッド呼出し

- JavaからJavaScriptへの通知

- エラー表示

含めないもの:

- OpenRocket Core

- `.ork`読込

- シミュレーション

### Release 0.0.2：Core Smoke Test

目的：OpenRocket Coreのクラスと依存がブラウザで初期化できることを確認する。

含めるもの:

- Coreのバージョン表示

- `BrowserBridge`の起動

- Java側の簡単な診断結果

- ランタイム情報表示

### Release 0.0.3：ORK Reader

目的：`.ork`を読み、ロケット概要を表示する。

含めるもの:

- ファイル選択

- ファイル検証

- `GeneralRocketLoader`相当の読込

- ロケット名

- コンポーネント数

- 読込警告

### Release 0.0.4：Simulation Preview

目的：1つのシミュレーションを実行し、結果を表示する。

含めるもの:

- 条件入力

- シミュレーション実行

- 時間–高度グラフ

- 時間–速度グラフ

- 時間–加速度グラフ

- JSON／CSV保存

### Release 0.1.0：MVP

目的：一般ユーザーがサンプルを使って一連の操作を完了できる状態にする。

含めるもの:

- 対応ブラウザの整理

- エラー・進捗表示

- ライセンス・免責事項

- E2Eテスト

- GitHub Pages公開

- 互換性表

## 17.14 追加ロードマップ

### W2：CheerpJ起動基盤

- CheerpJバージョンを固定

- `cheerpjInit`実行

- `cheerpjRunJar`実行

- nativeメソッド登録

- 初期化状態UI

- 公式サンプルのCI用スモークテスト

**完了条件:** Javaの文字列をJavaScriptへ返せる。

### W3：Javaブリッジ雛形

- `BrowserBridge`作成

- Java側Facade API決定

- JSONリクエスト・レスポンス形式決定

- Java例外のエラーコード化

- CoreなしのモックBridgeを作成

**完了条件:** ReactからJavaブリッジへリクエストを送り、JSON結果を受け取れる。

### W4：Core接続

- OpenRocket Coreの対象コミット固定

- Coreのビルド

- 依存ライブラリ一覧化

- Coreクラスの初期化

- Coreバージョン表示

**完了条件:** ブラウザ上でCoreクラスを参照できる。

### W5：固定サンプル読込

- サンプル`.ork`をJARまたは仮想ファイルへ配置

- `GeneralRocketLoader`で読込

- `OpenRocketDocument`取得

- `Rocket`情報をJSONで返す

- 読込エラーの分類

**完了条件:** 固定サンプルのロケット名と部品数をブラウザに表示できる。

### W6：ユーザーファイル読込

- File API実装

- ArrayBuffer変換

- 仮想ファイルシステムまたは`byte[]`方式を固定

- サイズ制限

- 連続読込・キャンセル

**完了条件:** ユーザーが選択した代表`.ork`を読み込める。

### W7–W8：シミュレーションBridge

- OpenRocketのシミュレーションAPIをFacade化

- SimulationRequest変換

- 結果系列の取得

- JSON結果DTO

- 例外・警告・タイムアウト

**完了条件:** 1件のシミュレーション結果をJavaScriptで受け取れる。

### W9–W10：グラフと比較

- Plotly接続

- 主要3系列表示

- デスクトップ版基準データ作成

- 誤差比較スクリプト

- 許容誤差決定

**完了条件:** ブラウザ版とデスクトップ版の比較レポートが出力できる。

### W11：Worker・性能

- Worker実行可否確認

- 初期化時間計測

- 読込時間計測

- 計算時間計測

- メモリ使用量確認

- 大きな結果配列の変換確認

**完了条件:** 対象端末でUIフリーズが許容範囲内である。

### W12：公開準備

- 本番アセット作成

- サイズ検査

- ハッシュ記録

- LICENSE・NOTICE整理

- GitHub Actions

- GitHub PagesへのPreview公開

**完了条件:** CIを通過した版だけがPreviewへ公開される。

## 17.15 運用時の監視項目

サーバーでユーザーデータを処理しないため、一般的なサーバー監視とは異なる。ブラウザ上の診断情報を、ユーザーの同意なく収集しない前提で、次の項目を開発者向けに確認できるようにする。

### 開発者向け診断パネル

- ブラウザ名・バージョン

- CheerpJバージョン

- Java実行バージョン

- OpenRocket Coreバージョン

- Bridgeバージョン

- Web Worker対応状況

- WebGL対応状況

- 初期化時間

- 最後の処理時間

- ファイルサイズ

- 直近のエラーコード

### 本番で収集しないもの

- `.ork`の内容

- ロケット名

- 設計データ

- シミュレーション結果

- 入力ファイルの中身

診断情報は、ユーザーが「診断情報をコピー」を押した場合だけ、画面上でテキストとして出力する方式を採用する。

## 17.16 障害対応手順

### A. CheerpJが起動しない

1. ブラウザの対象バージョンを確認

1. loaderの読み込み失敗を確認

1. `cheerpjInit`の例外を確認

1. キャッシュを無効化して再試行

1. CheerpJバージョンとアセットの組み合わせを確認

1. 直近のリリースでバージョン変更がなかったか確認

### B. Coreクラスが見つからない

1. Browser Bridge JARにクラスが含まれるか確認

1. Core JARのパスを確認

1. 依存JARの不足を確認

1. Javaのパッケージ名を確認

1. Shadow／fat JAR化によるServiceLoader問題を確認

1. 対応する互換性セットへ戻す

### C. `.ork`読込に失敗する

1. ファイルサイズを確認

1. OpenRocketデスクトップ版で開けるか確認

1. ファイルの拡張子と内容を確認

1. Java側の`RocketLoadException`を分類

1. Coreの対象バージョンを確認

1. 最小サンプルで再現するか確認

### D. シミュレーション結果が一致しない

1. 同じ`.ork`を使っているか確認

1. モーターが同一か確認

1. 発射条件と大気条件を確認

1. 単位変換を確認

1. サンプリング間隔を確認

1. OpenRocketの対象コミットを確認

1. 浮動小数点と丸め処理を確認

### E. ブラウザが固まる

1. ファイルサイズを確認

1. 3Dを無効化

1. シミュレーション時間を計測

1. Workerの使用可否を確認

1. 結果配列のサイズを確認

1. タイムアウトを発動

1. 端末別の制限を表示

## 17.17 ロールバック方針

CheerpJの更新やOpenRocket Coreの更新で障害が出た場合、次の順序で戻す。

1. `runtime-manifest.json`の直前バージョンを確認

1. CheerpJのバージョンを直前へ戻す

1. Browser Bridgeのバージョンを戻す

1. OpenRocket Coreのコミットを戻す

1. React Adapterの互換性を確認

1. CIの回帰テストを再実行

1. GitHub Pagesで直前の正常版を再公開

本番公開前に、少なくとも1つ前の正常な静的アセットを保存しておく。

## 17.18 最終的な受入基準

### CheerpJ基盤

- [ ] CheerpJのバージョンが固定されている

- [ ] Java実行環境の初期化状態が画面に表示される

- [ ] 初期化失敗時に再試行できる

- [ ] JavaScriptからJavaへリクエストを送れる

- [ ] JavaからJavaScriptへ結果を返せる

- [ ] Java例外がユーザー向けエラーコードへ変換される

### OpenRocket Core

- [ ] 対象コミットが記録されている

- [ ] Coreと依存ライブラリの構成が再現可能である

- [ ] Browser BridgeがCoreの内部クラスをUIへ直接漏らさない

- [ ] `.ork`サンプルを3種類以上読める

- [ ] ロケット概要DTOを取得できる

- [ ] 主要シミュレーションを実行できる

### ブラウザ運用

- [ ] Chromeで動作する

- [ ] Firefoxで動作する

- [ ] Edgeで動作する

- [ ] Safariで動作する、または対応範囲を明記する

- [ ] 20MB超のファイルを安全に拒否できる

- [ ] 読込中に別ファイルを選んでも古い結果が表示されない

- [ ] 長時間計算の状態が分かる

- [ ] エラー時に再読み込み以外の復旧方法がある

### 公開・運用

- [ ] CIでCore、Bridge、フロントエンドをビルドできる

- [ ] CheerpJスモークテストがCIで成功する

- [ ] サンプル`.ork`のE2Eテストが成功する

- [ ] アセットサイズを記録している

- [ ] GPLv3と依存ライセンスを公開している

- [ ] OpenRocket公式との関係を誤認させない表示がある

- [ ] 設計データをサーバーへ送信しないことを確認している

## 17.19 この前提での実装優先順位

CheerpJで動作する前提になった場合でも、次の順番は変更しない。

```
1. CheerpJランタイム初期化
2. JavaScript–JavaのHello World
3. Browser Bridge
4. OpenRocket Core初期化
5. 固定.ork読込
6. ユーザー.ork読込
7. RocketSummary表示
8. SimulationFacade
9. 結果JSON
10. Plotlyグラフ
11. Worker・性能改善
12. GitHub Pages公開
13. 3Dプレビュー
```

3Dを先に作らない理由は、CheerpJとCoreの運用安定性、ファイル読込、シミュレーション結果の正確性がプロダクトの中心だからである。

