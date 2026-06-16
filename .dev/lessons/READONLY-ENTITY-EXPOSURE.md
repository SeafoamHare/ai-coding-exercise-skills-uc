# Read-only Entity Exposure（唯讀實體跨界回傳）

## 來源

Hu, Chen, Cheng, Yang, *"Supplemental Patterns for Domain-Driven Design: Read-only
Entities, Internal Domain Events, and External Domain Events,"* Journal of Information
Science and Engineering 42(4), 881–904, 2026, §2。作者即本框架 Teddysoft 團隊。

## 問題描述

Aggregate Root 把內部 child entity 交給外部 client 時（例如 `Product.getGoal()`、
`Workflow.getStage()`），Evans 藍皮書允許「暫時交出 reference，但 client 不可持有並修改」。
這條規則**只靠開發者紀律**，編譯期與執行期都擋不住 client 抓著 reference 改壞 aggregate
的不變式。

## 症狀

```java
// Examle-1 的 ProductGoal（before）
public List<GoalMetric> getMetrics() { return metrics; }   // ❌ 回傳活的內部 list

// 外部 client 一行就能改到 aggregate 內部狀態：
product.getGoal().getMetrics().clear();                     // 💥 繞過 root
```

ezKanban 真實案例（論文 §5.1）：查 `Workflow` 取得 `Stage`，測試卻因為「透過 `Stage`
reference 改到 `Workflow`」而失敗。

## 根本原因

`ezddd-java` 原本只用 **package-private mutator**（entity.md Rule 2）保護 entity。它只能在
**編譯期**擋住*跨 package* 的呼叫者，擋不住三件事：

1. collection getter 外洩活的內部 list；
2. 回傳的巢狀 entity 被同 package 程式碼修改；
3. 沒有執行期 fail-fast（force 3「Informing misuses」未滿足）。

## 解決方案：Read-only Entities（Special Case，預設）

1. Entity 維持 **non-final** + package-private mutator；新增 **protected copy constructor**
   （shallow-copy 屬性、defensive-copy 集合）。
2. 產生 `ReadOnly{Entity} extends {Entity}`：
   - command 方法 override 成 `throw new UnsupportedOperationException(...)`；
   - 回傳 immutable 的 query 直接繼承；
   - 回傳 entity 的 query 換成 read-only 版；
   - 回傳 collection 的 query 換成 `Collections.unmodifiable*`。
3. Aggregate Root getter 回傳 `source == null ? null : new ReadOnly{Entity}(source)`。
4. 順手修掉 base getter：collection getter 一律回傳 `Collections.unmodifiableList(...)`。

對照範例：`Examle-2/`（before = `Examle-1/`）。

## 取捨與注意事項

- **刻意違反 LSP**：`ReadOnly{Entity}` is-a `{Entity}` 但 command 丟例外，是論文明示、用 LSP
  換 encapsulation 的故意設計。**Code review 不可把它當 bug。**
- **Event Sourcing nuance**：本專案 mutator 已是 package-private，跨 package 的 use-case
  service 本來就叫不到 `revise()`。因此 read-only 在本 codebase 的**外部可見**價值集中在：
  (a) 堵 collection 外洩、(b) 巢狀 entity 回傳、(c) 執行期 fail-fast。command override 屬
  defense-in-depth 與 pattern 完整性。
- **Value Object 免除**：record / immutable VO 不需 read-only，只有裝它們的 collection 要 unmodifiable。
- **觸發範圍（YAGNI）**：只有被 Aggregate Root query 方法回傳到邊界外的 entity 才產生
  `ReadOnly` 版；不會被回傳的 entity 不必產生。

## 影響的 skill 檔案

- `references/patterns/domain/entity.md` § Rule 11（模板）
- `references/patterns/domain/aggregate.md` § Rule 13（回傳轉換）
- `references/rules/domain-patterns.md` § Read-only Entities + `readonly_exposure` 語意標籤
- `references/rules/critical-rules.md` FORBIDDEN #34 / ALWAYS #22
- `references/patterns/testing/contract-test.md` § Read-only Entity Verification
- `references/code-reviewer/checklist.md`（Read-only Entity Checklist + Aggregate Root 回傳檢查）

## Proxy 替代方案

多型 entity 階層（一個 ReadOnly 服務多個子型別）可改用 Proxy：抽 `I{Entity}` 介面、
`ReadOnly{Entity} implements I{Entity}` 持 `real` 委派。代價是要為每個 entity 抽介面、
aggregate 與 client 改用介面型別。一般情況用 Special Case。
