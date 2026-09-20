# SuPOS Android Tablet 中控臺發布與回滾檢查清單 (Release & Rollback Checklist)

本清單規範 SuPOS Android Tablet 中控臺應用程式從開發、Staging 驗收、Production 發布至突發狀況緊急回滾（Rollback）之標準作業程序（SOP）。

---

## 1. 版本策略與命名規範 (Versioning Policy)

- **語意化版本 (Semantic Versioning)**：採用 `MAJOR.MINOR.PATCH`（例如 `1.0.0`）。
  - `MAJOR`：重大架構調整或不相容之資料庫/API 協定變更。
  - `MINOR`：新增功能（如自適應佈局擴充、新支付閘道整合）。
  - `PATCH`：Bug 修復、安全性補丁、效能優化。
- **Android Version Code 計算公式**：
  $$\text{versionCode} = \text{MAJOR} \times 10000 + \text{MINOR} \times 100 + \text{PATCH}$$
  - 例：`v1.0.0` $\rightarrow$ `10000`；`v1.2.3` $\rightarrow$ `10203`。
- **建置變體命名 (Artifact Naming)**：
  - Staging：`SuPOS-stagingRelease-v{versionName}-{gitSha}.apk` (Package: `com.supos.center.staging`)
  - Production：`SuPOS-productionRelease-v{versionName}-{gitSha}.apk` (Package: `com.supos.center`)

---

## 2. Staging 驗收檢查清單 (Pre-Release Checklist)

在將版本推向門市正式環境前，必須在實體 Android 平板（至少包含 7 吋與 10 吋設備）上完成以下驗證：

- [ ] **全套自動化 CI 通過**：
  - GitHub Actions CI 綠燈。
  - Android Lint 0 錯誤。
  - 80+ 項單元測試、狀態機測試與 Room Migration 鏈路 100% 通過。
- [ ] **核心營運交易流驗收**：
  - [ ] 線上訂單接收並成功帶入現場結帳購物車，商品品項、選項、加料快照 100% 吻合。
  - [ ] 現場變價、贈送/招待（小計為 0）計算精準。
  - [ ] 支援現金、信用卡、LinePay 結帳，電子發票統編（8碼）與載具（`/`開頭8碼）驗證有效。
  - [ ] 快速連點防重複結帳（Mutex Submission Lock）生效，單次操作僅生成一筆訂單。
- [ ] **離線與網路容錯驗收**：
  - [ ] 平板開啟飛航模式進行現場結帳，資料庫寫入成功，訂單進入 Outbox 待同步隊列。
  - [ ] 關閉飛航模式恢復連線，背景重試機制在 30 秒內自動將訂單補傳至 GAS 雲端並標記 `SYNCED`。
- [ ] **系統健康監控 (System Health Aggregator)**：
  - [ ] 首頁動態徽章正常反映網路狀態（斷網顯示 `○ 離線模式`，恢復連線顯示 `● 系統就緒`）。
  - [ ] 點擊健康徽章開啟唯讀診斷視窗，確認無憑證或 Token 明文洩漏（符合去敏規範）。
- [ ] **資安與備份限制**：
  - [ ] 執行 `adb backup` 或設備遷移，驗證 `supos_database`、`sharedpref` 被嚴格排除，無法被外洩。

---

## 3. Production 發布程序 (Release Execution Steps)

1. **更新版本資訊**：
   - 於 `app/build.gradle.kts` 中確認 `versionCode` 與 `versionName`，或透過 CI 參數注入。
2. **Git Tagging**：
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0: Android Tablet Production Release"
   git push origin v1.0.0
   ```
3. **安全簽章與 APK 建置**：
   - 確認 CI Secrets 中配置有正式 Release Keystore 憑證與密碼。
   - 產出 `SuPOS-productionRelease-v1.0.0-{gitSha}.apk`。
4. **產生 SHA256 校驗碼**：
   ```bash
   shasum -a 256 SuPOS-productionRelease-v1.0.0-*.apk > release_sha256.txt
   ```
5. **部署至門市機台**：
   - 透過專用商用 MDM 推播或以受控 USB/ADB 覆蓋安裝。
   - 登入門市帳號，確認菜單快取自動更新且過往訂單紀錄完整載入。

---

## 4. 緊急回滾標準作業程序 (Emergency Rollback SOP)

當正式版 APK 上線後發生 P0 級重大事故（如 App 頻繁 Crash、資料庫讀取失敗、無法送出訂單），應立即依據以下 SOP 啟動回滾：

### 回滾三大原則
1. **Room 資料庫向後相容**：所有 SQLite Schema 變更一律採追加（Additive）模式，禁止刪除過往資料表或重命名既有欄位，確保舊版 APK 讀取新資料庫時不發生 SQLiteSchemaException。
2. **零破壞性覆蓋 (In-place Downgrade)**：嚴禁執行「清除所有資料」或「解除安裝後重裝」，否則門市尚未同步至雲端的 Outbox 本地交易將永久遺失。
3. **雲端契約相容**：GAS 後端 API 需保持雙向相容至少 2 個版本週期，不因前端回滾造成協定不合。

### 回滾具體步驟
1. **停止災情擴散**：
   - 於 MDM 管理後台暫停推播當前版本，防止其他門市平板升級。
2. **確認未同步交易**：
   - 若平板尚能進入「首頁」，點擊健康徽章查看 Outbox 是否有待同步單。若有，優先嘗試連上穩定網路手動觸發同步。
3. **執行舊版覆蓋安裝 (保留 App 數據)**：
   - 取得前一穩定版 APK（例如 `v0.9.9`）。
   - 透過 ADB 執行保留數據降級安裝：
     ```bash
     adb install -r -d SuPOS-productionRelease-v0.9.9.apk
     ```
4. **資料庫驗證與恢復確認**：
   - 開啟 App，確認過往交易歷史與今日營收正常顯示。
   - 進行一筆現場測試訂單（小計 0 元），確認開單與結帳能正常出單與同步。
5. **事後複盤與修復**：
   - 收集 Crash Log 與系統診斷資訊，於 GitHub 建立 Hotfix Issue。
   - 修復並通過 CI 全套測試矩陣後，再次推進 PATCH 版本重新發布。
