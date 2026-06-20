// Copies the shared mini-program runtime utils into each mini program's
// miniprogram/shared/ folder. Run before opening WeChat DevTools or type-checking.
//   node tools/sync-miniapp-shared.mjs
import { cpSync, mkdirSync, rmSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const src = resolve(root, 'frontend/shared/miniapp')
const targets = [
  resolve(root, 'frontend/tourist-miniapp/miniprogram/shared'),
  resolve(root, 'frontend/guide-miniapp/miniprogram/shared'),
]

for (const dest of targets) {
  rmSync(dest, { recursive: true, force: true })
  mkdirSync(dest, { recursive: true })
  cpSync(src, dest, { recursive: true })
  console.log(`synced shared -> ${dest}`)
}
