import { copyFile, mkdir, stat } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';

async function fileExists(filePath) {
  try {
    await stat(filePath);
    return true;
  } catch {
    return false;
  }
}

async function main() {
  const repoRoot = path.resolve(process.cwd(), '..');
  const wasmDir = path.join(repoRoot, 'core-wasm', 'target', 'wasm');
  const outDir = path.join(process.cwd(), 'public', 'wasm');

  const wasmPath = path.join(wasmDir, 'app.wasm');
  const runtimePath = path.join(wasmDir, 'app.wasm-runtime.js');

  const missing = [];
  if (!(await fileExists(wasmPath))) missing.push(wasmPath);
  if (!(await fileExists(runtimePath))) missing.push(runtimePath);

  if (missing.length > 0) {
    console.error('Missing WASM artifacts. Build core-wasm first:');
    console.error('  mvn -f core-wasm/pom.xml clean package');
    for (const m of missing) console.error(`  - ${m}`);
    process.exit(1);
  }

  await mkdir(outDir, { recursive: true });
  await copyFile(wasmPath, path.join(outDir, 'app.wasm'));
  await copyFile(runtimePath, path.join(outDir, 'app.wasm-runtime.js'));

  console.log(`Copied:\n- ${wasmPath} -> ${path.join(outDir, 'app.wasm')}\n- ${runtimePath} -> ${path.join(outDir, 'app.wasm-runtime.js')}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

