# OpenRocket Web移植プロジェクト
## 詳細技術仕様書・プロジェクト計画書

**版:** 1.0（初版）  
**作成日:** 2026-09-30  
**対象:** 学生個人開発／OSS公開を想定したブラウザ版OpenRocketクライアント  
**目標:** サーバー側に設計データを送信せず、静的ホスティング上で動作するWebアプリケーションとして、OpenRocketの`.ork`読込・基本シミュレーション・結果可視化を提供する。

> 本書は、添付された初期案を基に、実装可能性・技術的リスク・受入基準・ロードマップ・運用方針を追加した更新版である。特に、Java実行方式、JavaScript連携API、ライセンス、無料ホスティングの制約は、実装開始前のPoCで検証し、未検証のAPIを製品仕様として固定しない。

---

## 1. エグゼクティブサマリー

### 1.1 目指すプロダクト

ユーザーがブラウザ上で次の操作を行える、インストール不要のOpenRocket Webクライアントを構築する。

1. `.ork`ファイルをドラッグ＆ドロップまたはファイル選択で読み込む
2. ロケット構成、ステージ、主要パーツ、モーター、シミュレーション設定を確認する
3. ブラウザ内でシミュレーションを実行する
4. 高度・速度・加速度・時間などの結果をインタラクティブなグラフで確認する
5. 可能な範囲でロケットの2D／3Dプレビューを表示する
6. 結果をCSV／JSONとしてローカル保存する

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
2. **JavaScript連携APIはバージョン固定しない。** CheerpJ公式ドキュメントには、JavaScript側のnativeメソッド実装と`cheerpjInit`／`cheerpjRunJar`を用いた例があるが、初期案に記載された`cjCall`をそのまま採用することは前提にしない。
3. **完全無料は利用量に依存する。** GitHub Pagesは静的ホスティングとして無料で利用できる一方、公開サイト1GB、推奨ソースリポジトリ1GB、ソフト帯域上限100GB/月などの制約がある。CheerpJランタイムやJARが大きい場合はサイズを継続的に監視する。
4. **OpenRocketのライセンス確認が必要である。** OpenRocket本体はGPLv3で公開されているため、コアコードを改変・再配布する場合のライセンス表示、ソース提供、派生物の扱いをリリース前に確認する。
5. **計算結果は安全保証ではない。** 本アプリは教育・設計支援用途であり、実機の打上げ可否、安全性、法令適合を保証しない。

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

```text
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
|---|---|---|
| `web-app` | React、TypeScript、CSS、グラフ、UI | 静的ホスティング |
| `openrocket-core` | OpenRocketの計算・モデル・ファイル処理に必要な最小JAR | 静的ホスティングのアセット |
| `cheerpj-runtime` | Java実行用ランタイム | CDNまたは公式推奨配布方式 |
| `sample-projects` | 小容量のサンプル`.ork` | リポジトリまたはリリースアセット |
| `docs` | 使い方、制約、ライセンス、免責事項 | 静的ホスティング |

### 3.3 データフロー：`.ork`読込

1. ユーザーがファイルを選択する
2. MIMEタイプだけでなく、拡張子・ファイルサイズ・バイナリ先頭を検証する
3. 最大ファイルサイズを超える場合は処理を中止する。MVPの初期値は20MBとし、実測に基づき調整する
4. `File.arrayBuffer()`またはストリーム方式でブラウザ内へ読み込む
5. Engine Adapterが仮想ファイルシステムへ配置する
6. OpenRocket Loaderを呼び出し、ドメインモデルへ変換する
7. UI用の正規化済みDTOへ変換する
8. 生のJavaオブジェクトをReact状態へ渡さず、シリアライズ可能なDTOのみ保持する
9. 成功時はファイル名、ロケット名、構成概要、警告を表示する
10. 失敗時は原因分類と再試行方法を表示し、内部スタックトレースは開発者ログに限定する

### 3.4 データフロー：シミュレーション

1. UIでシミュレーション条件を入力する
2. `SimulationRequest`をバリデーションする
3. Worker経由でEngine Adapterへ送る
4. OpenRocketのシミュレーションエンジンを実行する
5. 結果を標準DTOに変換する
6. UIに進捗または完了通知を返す
7. Chart AdapterがPlotly等の表示用データへ変換する
8. 結果を画面表示し、ユーザー操作でCSV／JSONへエクスポートする

---

## 4. 技術スタック選定

### 4.1 採用候補

| レイヤー | 第一候補 | 採用理由 | 注意点 |
|---|---|---|---|
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
2. Javaコアの必要ロジックをWebAssembly／TypeScriptへ段階的に移植
3. サーバーなしを維持しつつ、既存のブラウザ実行可能な数値計算実装を採用
4. サーバー実行へ移行する場合は、無料要件・プライバシー要件を再合意する

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

```ts
export interface EngineAdapter {
  initialize(): Promise<EngineStatus>;
  loadRocket(input: RocketFileInput): Promise<RocketSummary>;
  listSimulations(): Promise<SimulationDefinition[]>;
  runSimulation(request: SimulationRequest): Promise<SimulationResult>;
  dispose(): Promise<void>;
}
```

### 6.2 DTO例

```ts
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
|---|---|---|
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
|---|---|---|
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

```text
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
|---|---:|---:|---|---|
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
|---|---|
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
2. `ci: add lint typecheck unit test workflow`
3. `poc: run official CheerpJ interoperability sample`
4. `poc: validate Java string and numeric array conversion`
5. `poc: load minimal OpenRocket core artifact`
6. `feat: add file drop zone and local file validation`
7. `feat: add EngineAdapter interface and mock adapter`
8. `feat: add RocketSummary DTO and display panel`
9. `feat: run first simulation through adapter`
10. `feat: render altitude time series`
11. `test: add desktop-vs-browser baseline comparison`
12. `deploy: publish preview to GitHub Pages`

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
