# OpenRocket Web Edition 技術設計書 / README（ドラフト）

---

## 1. プロジェクト概要

### 1.1 目的

- OpenRocket のシミュレーション機能を、Java ランタイムが存在しない環境（iPad, Android タブレット, Chromebook 等）でも利用可能にする。
- OpenRocket Core（Java ベースのシミュレーションロジック）を再実装せず、可能な限り既存コードを活用する。
- ブラウザ上で動作する Web UI（React/TypeScript ベース）を提供し、ロケット設計・シミュレーション・結果可視化を行う。

### 1.2 アプローチ概要

- Java コード（OpenRocket Core から必要部分を抽出）を TeaVM により WebAssembly (WASM) にコンパイルする。
- WASM で公開された関数を、ブラウザ側の JavaScript/TypeScript から呼び出す。
- Web UI は React + TypeScript を用いて構築し、WASM をバックエンド的に利用する。

---

## 2. 全体アーキテクチャ

### 2.1 レイヤ構成

- Java レイヤ
  - OpenRocket Core の一部（または全体）を利用。
  - TeaVM 対応のためのブリッジクラスを追加。
- WASM レイヤ
  - TeaVM により生成された WebAssembly モジュール。
  - JavaScript から呼び出し可能なエクスポート関数を提供。
- Web UI レイヤ
  - React + TypeScript による SPA（Single Page Application）。
  - WASM モジュールをロードし、シミュレーション実行・結果表示を行う。

### 2.2 ディレクトリ構成（提案）

```text
openrocket-web/
├── core-wasm/                 # Java → WASM (TeaVM) プロジェクト
│   ├── src/main/java/
│   │   └── org/openrocket/web/
│   │       └── SimulationBridge.java
│   ├── pom.xml
│   └── target/wasm/
│       ├── app.wasm
│       └── app.js             # 必要に応じて生成
│
├── web-ui/                    # React + TypeScript プロジェクト
│   ├── public/
│   │   └── wasm/
│   │       └── app.wasm       # core-wasm からコピー or ビルド時に配置
│   ├── src/
│   │   ├── wasm/
│   │   │   ├── loader.ts
│   │   │   └── types.d.ts
│   │   ├── components/
│   │   ├── pages/
│   │   └── App.tsx
│   ├── package.json
│   └── tsconfig.json
│
└── docs/
    └── architecture.md        # 本ドキュメント
```

---

## 3. 使用技術と前提条件

### 3.1 使用技術

- Java 8 以降（OpenRocket Core に合わせる）
- Maven（Java ビルド・TeaVM プラグイン実行）
- TeaVM（Java → WebAssembly コンパイル）
- Node.js / npm または pnpm / yarn
- React 18 以降
- TypeScript 5 系想定

### 3.2 TeaVM による制約

TeaVM は JVM の全機能をサポートしているわけではないため、以下の制約を考慮する。

- Reflection（`java.lang.reflect`）は基本的に非推奨または制限付き。
- マルチスレッドは制限あり（ブラウザ環境の制約に依存）。
- ファイル I/O（`java.io.File` 等）はブラウザでは直接利用不可。
- GUI ライブラリ（AWT/Swing）は利用不可。
- ネイティブコード（JNI）は利用不可。

OpenRocket Core から利用する範囲は、上記制約に抵触しないように限定するか、必要に応じてラップ・置き換えを行う。

---

## 4. Java 側設計（core-wasm）

### 4.1 役割

- OpenRocket Core のシミュレーションロジックを呼び出す。
- ブラウザから扱いやすい形（プリミティブ配列や JSON 文字列）で結果を返す。
- TeaVM によって WASM にコンパイルされるエントリポイントを提供する。

### 4.2 Maven 設定例

`core-wasm/pom.xml` の例（要点のみ）:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.openrocket</groupId>
  <artifactId>core-wasm</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>jar</packaging>

  <dependencies>
    <!-- OpenRocket Core への依存（ローカル or Maven リポジトリに配置） -->
    <dependency>
      <groupId>org.openrocket</groupId>
      <artifactId>openrocket-core</artifactId>
      <version>xxx</version>
    </dependency>

    <!-- TeaVM JS interop 用 -->
    <dependency>
      <groupId>org.teavm</groupId>
      <artifactId>teavm-jso</artifactId>
      <version>0.9.2</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.teavm</groupId>
        <artifactId>teavm-maven-plugin</artifactId>
        <version>0.9.2</version>
        <executions>
          <execution>
            <goals>
              <goal>compile</goal>
            </goals>
            <configuration>
              <targetDirectory>${project.build.directory}/wasm</targetDirectory>
              <mainClass>org.openrocket.web.SimulationBridge</mainClass>
              <target>wasm</target>
              <debugInformation>true</debugInformation>
              <!-- 必要に応じて最適化レベルなどを設定 -->
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

バージョン番号や依存関係は実際の OpenRocket Core の構成に合わせて調整する。

### 4.3 ブリッジクラス設計

TeaVM では、`@JSExport` アノテーションを付与することで、Java のメソッドを JavaScript から呼び出せるようにできる。

#### 4.3.1 最小ブリッジ例

```java
package org.openrocket.web;

import org.teavm.jso.JSExport;

public class SimulationBridge {

    /**
     * ORK ファイル内容（テキスト）を受け取り、シミュレーション結果を JSON 文字列で返す。
     * JSON 形式は Web UI 側でパースして利用する。
     */
    @JSExport
    public static String runSimulation(String orkData) {
        // TODO: orkData をパースし、OpenRocket Core のロケットモデルに変換
        // TODO: シミュレーションを実行
        // TODO: 結果を JSON 文字列にシリアライズして返す

        // 仮実装（後で OpenRocket Core に置き換え）
        return "{\"maxAltitude\": 123.4, \"timeSeries\": [0.0, 10.0, 20.0]}";
    }
}
```

#### 4.3.2 データ形式の方針

- 入力:
  - 初期段階では `.ork` ファイルの中身をそのまま文字列として渡す。
  - 将来的には、Web UI 側で構築した JSON モデルを渡し、Java 側で内部モデルに変換することも検討。
- 出力:
  - JSON 文字列として返す。
  - 例: 高度・速度・加速度などの時系列データ、最大高度、最大速度など。

例:

```json
{
  "summary": {
    "maxAltitude": 1234.5,
    "maxVelocity": 210.3,
    "flightTime": 35.2
  },
  "timeSeries": {
    "time": [0.0, 0.1, 0.2, ...],
    "altitude": [0.0, 1.2, 2.5, ...],
    "velocity": [0.0, 12.3, 24.1, ...]
  }
}
```

この JSON スキーマは別途 `docs/api.md` 等に定義しておくとよい。

---

## 5. WASM ビルドと成果物

### 5.1 ビルド手順（Java 側）

```bash
cd core-wasm
mvn clean package
```

成功すると、以下のような成果物が生成される想定:

- `core-wasm/target/wasm/app.wasm`
- 必要に応じて `app.js`（TeaVM が生成する JS ラッパ）

### 5.2 Web UI への配置

ビルド成果物を Web UI プロジェクトに配置する方法は複数ある。

- 手動コピー:
  - `core-wasm/target/wasm/app.wasm` → `web-ui/public/wasm/app.wasm`
- ビルドスクリプトで自動コピー:
  - ルートに `Makefile` や `scripts/copy-wasm.js` を用意し、`npm run build` 前後でコピーする。

例: ルートの簡易スクリプトイメージ

```bash
# ルートディレクトリで
mvn -f core-wasm/pom.xml package
cp core-wasm/target/wasm/app.wasm web-ui/public/wasm/app.wasm
```

---

## 6. Web UI 側設計（web-ui）

### 6.1 役割

- ユーザーがロケットモデルを編集・読み込み・保存するための UI を提供する。
- WASM モジュールをロードし、シミュレーションを実行する。
- シミュレーション結果をグラフや数値で可視化する。

### 6.2 TypeScript による WASM ローダー

`src/wasm/loader.ts` の例:

```ts
export interface SimulationExports {
  // TeaVM がエクスポートする関数名に合わせる
  runSimulation: (ptr: number, len: number) => number;
  // 文字列の受け渡し用にメモリ操作関数が必要になる場合もある
  memory?: WebAssembly.Memory;
}

/**
 * WASM モジュールをロードし、エクスポートされた関数を返す。
 */
export async function loadWasm(): Promise<WebAssembly.Instance["exports"]> {
  const response = await fetch("/wasm/app.wasm");
  const bytes = await response.arrayBuffer();
  const { instance } = await WebAssembly.instantiate(bytes, {
    env: {
      // 必要に応じて import 関数を定義
    },
  });
  return instance.exports;
}
```

TeaVM が生成する WASM のインターフェース仕様に応じて、上記は調整が必要になる。  
文字列の受け渡しについては、TeaVM 側で JS ラッパを生成させるか、手動でメモリ操作を行うかを選択する。

### 6.3 React コンポーネントからの呼び出し例

`src/App.tsx` のイメージ:

```tsx
import React, { useEffect, useState } from "react";
import { loadWasm } from "./wasm/loader";

function App() {
  const [wasmExports, setWasmExports] = useState<any | null>(null);
  const [resultJson, setResultJson] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      const exports = await loadWasm();
      setWasmExports(exports);
    })();
  }, []);

  const handleRunSimulation = async () => {
    if (!wasmExports) return;

    const orkData = "..."; // 実際にはファイル入力やフォームから取得
    // ここは TeaVM の JS ラッパの仕様に合わせて呼び出し方を調整する
    const result = (wasmExports as any).runSimulation(orkData);
    setResultJson(result);
  };

  return (
    <div>
      <h1>OpenRocket Web Edition</h1>
      <button onClick={handleRunSimulation} disabled={!wasmExports}>
        Run Simulation
      </button>
      {resultJson && (
        <pre>{resultJson}</pre>
      )}
    </div>
  );
}

export default App;
```

実際には、TeaVM が生成する JS ラッパを利用する場合、`runSimulation` が直接文字列を受け取れる形になることもある。その場合は、上記のようなメモリ操作は不要になる。

---

## 7. データモデルと API 設計

### 7.1 シミュレーション入力モデル

初期段階では、以下のいずれかの方式を採用する。

- `.ork` ファイルの XML/テキストをそのまま Java 側に渡す。
- Web UI 側で JSON モデルを構築し、Java 側で内部モデルに変換する。

将来的な拡張性を考えると、JSON モデルを定義しておくとよい。

例（簡略化したロケットモデル JSON）:

```json
{
  "rocket": {
    "name": "Test Rocket",
    "stages": [
      {
        "name": "Stage 1",
        "motors": [
          {
            "name": "Motor A",
            "thrustCurveId": "motorA-curve-1"
          }
        ]
      }
    ]
  },
  "environment": {
    "windSpeed": 3.0,
    "temperature": 15.0
  }
}
```

### 7.2 シミュレーション出力モデル

Web UI で扱いやすいように、JSON 形式で返す。

例:

```json
{
  "summary": {
    "maxAltitude": 1234.5,
    "maxVelocity": 210.3,
    "maxAcceleration": 15.2,
    "flightTime": 35.2
  },
  "timeSeries": {
    "time": [0.0, 0.1, 0.2],
    "altitude": [0.0, 1.2, 2.5],
    "velocity": [0.0, 12.3, 24.1],
    "acceleration": [0.0, 123.4, 110.2]
  }
}
```

このスキーマは、`docs/api-simulation.md` といった別ファイルに正式に定義しておくとよい。

---

## 8. テスト戦略

### 8.1 Java 側テスト

- JUnit によるユニットテストを実施。
- OpenRocket Core を JVM 上で直接実行した結果と、TeaVM/WASM 経由で実行した結果を比較する「数値差分テスト」を用意する。
  - 同一入力に対して、許容誤差内で一致することを確認する。

### 8.2 Web UI 側テスト

- Jest + React Testing Library によるコンポーネントテスト。
- Playwright などによる E2E テストで、ブラウザ上での WASM 呼び出しと結果表示を検証する。

---

## 9. ビルド・開発フロー

### 9.1 初期セットアップ

```bash
# Java 側
cd core-wasm
mvn clean package

# Web UI 側
cd ../web-ui
npm install
npm start
```

### 9.2 開発サイクル

1. Java 側（core-wasm）でブリッジやロジックを修正。
2. `mvn package` で WASM を再生成。
3. 生成された `app.wasm` を `web-ui/public/wasm/` にコピー（スクリプトで自動化推奨）。
4. Web UI 側で `npm start` によりローカル開発サーバーを起動し、ブラウザで動作確認。

---

## 10. リスクと対策

### 10.1 TeaVM 非対応 API の利用

- 問題:
  - OpenRocket Core が Reflection やファイル I/O など、TeaVM 非対応の API に依存している可能性がある。
- 対策:
  - 依存分析を行い、シミュレーションに必要な最小限のクラス・メソッドに絞る。
  - 必要に応じて、問題となる部分をラップ・スタブ化する。

### 10.2 パフォーマンス

- 問題:
  - WASM 上での計算は高速だが、JS との境界を跨ぐ呼び出しが多いとオーバーヘッドが増える。
- 対策:
  - 1 回のシミュレーションを 1 回の関数呼び出しで完結させる。
  - 時系列データをまとめて返す（逐次呼び出しを避ける）。

### 10.3 デバッグの難しさ

- 問題:
  - WASM 内部のデバッグは JVM 上のデバッグに比べて難しい。
- 対策:
  - まず JVM 上でロジックを十分にテストし、その後 WASM 化する。
  - TeaVM のデバッグ情報出力を有効にし、必要に応じてブラウザの DevTools で確認する。

---

## 11. 今後の拡張案

- PWA 対応によるオフライン動作。
- Three.js 等を用いた 3D モデル表示。
- ロケットモデルの GUI ベース編集機能（フィン形状、質量分布など）。
- シミュレーション条件（風、気温、発射角度など）のインタラクティブな設定 UI。

