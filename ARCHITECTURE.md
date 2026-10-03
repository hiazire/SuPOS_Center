# SuPOS Android Tablet 架構

## 工作區導覽

```text
Dashboard
├── OrderHistory    線上訂單接收、狀態處理、帶入結帳
├── Main            現場開單、暫存、客製化、結帳
└── Performance     今日／7 日／30 日業績戰報
```

`MainNavigation` 以 Navigation 3 的型別化 `NavKey` 管理工作區。各頁透過 callback 返回，
不直接修改導覽 back stack。

## 狀態與資料流

- `MainScreenViewModel`：菜單、購物車、訂單暫存及現場結帳狀態。
- `OrderHistoryViewModel`：雲端線上訂單、日期篩選、狀態更新與帶入結帳。
- `PerformanceViewModel`：戰報區間、載入／成功／失敗狀態。
- `SalesReportCalculator`：只以 `paymentStatus == PAID` 計算營收、單數與客單價；
  未付款且未取消的訂單計入待處理。
- `OrderRepository`：Room local-first 訂單與 GAS API 的整合邊界。

## 點餐菜單來源

- 權威資料是 [門市試算表的 `Menu` 工作頁](https://docs.google.com/spreadsheets/d/191x1EVmp5tzc1ODrl3zDisVz4sq7LpWsRVsIUUeJiGI/edit)。Android 透過既有 GAS `fetchMenu()` 取得內容，驗證後以 Room transaction 更新本機菜單。
- `id`、`category`、`name`、`price`、`imageUrl`、`options_group`、`icon`、`printer`、`sizeTypes`、`isAvailable` 均從該回應保存。`printer` 為菜單出單站別原始值，實際列印仍須完成印表機權威 mapping。
- 分類順序取 `Menu` 工作頁首次出現的順序，商品順序取列順序；Room 以 `sourceOrder` 保存，舊版菜單在首次同步前以商品 ID 作穩定排序。
- 現行表格將可售值拼為 `TURE`，Android 同時接受 `TURE` 和 `TRUE`。空白或其他非可售值不進入點餐選單；升級前沒有此欄的本機舊菜單維持可見，直到成功同步。
- 空白／非法價格與無效／重複 ID 的商品不進入可售菜單，點餐畫面會顯示本次跳過的品項數。`sizeTypes` 目前全部為空，不產生規格選擇介面。

## 整合規則

- 訂單 ID 是同步與去重的主鍵。
- 遠端 mutation 只有在明確成功回應後才視為完成。
- 線上訂單的接單／付款狀態，與 Radar 的列印狀態分開處理。
- 金額在 App 內以新臺幣元顯示；若 POS 來源為分，必須在整合層正規化。
- 網路錯誤需保留本機訂單與可重試狀態，不可呈現假成功。
