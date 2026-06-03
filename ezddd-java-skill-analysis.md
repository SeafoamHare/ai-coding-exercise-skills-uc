# ezddd-java Skill 完整分析

> 分析日期：2026-06-03
> 來源：`.claude/skills/ezddd-java/SKILL.md` 及其 `references/` 目錄（62 檔案，~24,400 行）

---

## 1. Execute-UC 執行流程

Execute-UC (`/execute-uc`) 是一個 7 Phase Pipeline，從 JSON UseCase Specification 自動產生 DDD 程式碼。

### 流程總覽

```
┌─────────────────────────────────────────────────────────────────────┐
│                    execute-uc Pipeline (7 Phases)                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Phase 0: Preparation                                               │
│  ┌───────────┐  ┌───────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │ Step 0.1  │→ │   Step 0.2    │→ │  Step 0.3    │→ │ Step 0.4 │ │
│  │ 檢查 Init │  │ 讀取 JSON Spec│  │ 驗證必要欄位 │  │ 讀取     │ │
│  │ Project   │  │ 偵測 Spec Type│  │ ⛔ BLOCKING  │  │ project- │ │
│  │ 基礎設施  │  │ CMD/QRY/REACT │  │              │  │ config   │ │
│  └───────────┘  └───────────────┘  └──────────────┘  └──────────┘ │
│       │                                                             │
│       │ missing? → auto-trigger /init-project                       │
│                                                                     │
│  Phase 1: JIT Learning (DO NOT SKIP!)                               │
│  ┌──────────────────────┐  ┌─────────────────────────────┐         │
│  │ Step 1.1             │→ │ Step 1.2                    │         │
│  │ LOAD critical-rules  │  │ LOAD json-to-pattern-mapping│         │
│  │ (27 FORBIDDEN +      │  │ (JSON → Pattern 欄位對照)   │         │
│  │  16 ALWAYS REQUIRED) │  │                             │         │
│  └──────────────────────┘  └─────────────────────────────┘         │
│                                                                     │
│  Phase 2: Code Generation (依 Spec Type 分流)                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              ┌──────────┐                                    │   │
│  │              │Spec Type │                                    │   │
│  │              └────┬─────┘                                    │   │
│  │         ┌─────────┼─────────┐                                │   │
│  │         ▼         ▼         ▼                                │   │
│  │    ┌─────────┐┌────────┐┌─────────┐                          │   │
│  │    │ COMMAND ││ QUERY  ││ REACTOR │                          │   │
│  │    └────┬────┘└───┬────┘└────┬────┘                          │   │
│  │         │         │          │                                │   │
│  │  ┌──────▼──────┐  │   ┌──────▼──────┐                        │   │
│  │  │4.1 Aggregate│  │   │4.1 Reactor  │                        │   │
│  │  │  + Events   │  │   │  Interface  │                        │   │
│  │  │  + Enums    │  │   │  + Service  │                        │   │
│  │  │  + VOs      │  │   └──────┬──────┘                        │   │
│  │  │  + Entities │  │          │                                │   │
│  │  └──────┬──────┘  │   ┌──────▼──────┐                        │   │
│  │  ┌──────▼──────┐  │   │4.2 Inquiry  │                        │   │
│  │  │4.1.5       │  │   │  + Test     │                        │   │
│  │  │Contract Test│  │   └─────────────┘                        │   │
│  │  └──────┬──────┘  │                                          │   │
│  │  ┌──────▼──────┐  ├──┐                                       │   │
│  │  │4.2 UseCase  │  │  │                                       │   │
│  │  │ Interface   │  │  ▼                                       │   │
│  │  │ + Service   │  │ ┌─────────────┐                          │   │
│  │  └──────┬──────┘  │ │4.1 Projection│                         │   │
│  │  ┌──────▼──────┐  │ │ + DTO       │                          │   │
│  │  │4.3 UseCase  │  │ └──────┬──────┘                          │   │
│  │  │ Test (ezSpec│  │ ┌──────▼──────┐                          │   │
│  │  │  BDD)       │  │ │4.2 Query    │                          │   │
│  │  └──────┬──────┘  │ │ UseCase +   │                          │   │
│  │  ┌──────▼──────┐  │ │ Service     │                          │   │
│  │  │4.4 Infra    │  │ └──────┬──────┘                          │   │
│  │  │ Data/Mapper │  │ ┌──────▼──────┐                          │   │
│  │  │ Config      │  │ │4.3 Test     │                          │   │
│  │  └─────────────┘  │ └─────────────┘                          │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  Phase 3: Compilation                                               │
│  ┌──────────────────────────────────┐                               │
│  │ mvn compile -q                   │                               │
│  │ 驗證所有產生的程式碼可編譯        │                               │
│  └──────────────────────────────────┘                               │
│                                                                     │
│  Phase 4: Gate 1 — Dual-Profile Test ⛔ BLOCKING                    │
│  ┌──────────────────────────────────────────────────┐               │
│  │ InMemory Profile         Outbox Profile          │               │
│  │ ┌─────────────────┐      ┌──────────────────┐   │               │
│  │ │ SPRING_PROFILES  │      │ SPRING_PROFILES   │   │               │
│  │ │ =test-inmemory   │      │ =test-outbox      │   │               │
│  │ │ mvn test         │      │ mvn test          │   │               │
│  │ │ -Dtest=...Test   │      │ -Dtest=...Test    │   │               │
│  │ └─────────────────┘      └──────────────────┘   │               │
│  │              ↓                    ↓              │               │
│  │        BOTH must PASS → otherwise STOP           │               │
│  └──────────────────────────────────────────────────┘               │
│  (--only-inmemory 時只跑 InMemory profile)                          │
│                                                                     │
│  Phase 5: Gate 2.5 — Deterministic Review ⛔ BLOCKING               │
│  ┌──────────────────────────────────────────────────┐               │
│  │ validate-generated-code.sh --aggregate {name}    │               │
│  │ 55 Rules = 30F + 15R + 5S + 1X + 4TS            │               │
│  │                                                  │               │
│  │ Phase 1: FORBIDDEN Pattern Scan (30 rules)       │               │
│  │ Phase 2: REQUIRED Pattern Scan (15 rules)        │               │
│  │ Phase 3: Package Structure Validation (5 rules)  │               │
│  │ Phase 4: Cross-File Consistency (1 rule)         │               │
│  │ Phase 5: TestSuite Coverage (4 rules)            │               │
│  │                                                  │               │
│  │ CRITICAL violation → BLOCKED → 回到 Phase 2 修正 │               │
│  └──────────────────────────────────────────────────┘               │
│                                                                     │
│  Phase 7: Completion Report                                         │
│  ┌──────────────────────────────────────────────────┐               │
│  │ - 產生檔案清單                                    │               │
│  │ - 測試結果統計                                    │               │
│  │ - Gate 狀態彙整                                   │               │
│  │ - 更新 task JSON（如有 task 檔案）                │               │
│  └──────────────────────────────────────────────────┘               │
└─────────────────────────────────────────────────────────────────────┘
```

### Spec Type 偵測規則

| JSON 欄位組合 | Spec Type | 產生內容 |
|-------------|-----------|---------|
| `useCase` + `domainEvent` | **COMMAND** | Aggregate + Events + UseCase + Service + Tests + Infra |
| `query` + `projections` | **QUERY** | Projection + Mapper + DTO + UseCase + Tests |
| `reactor` + `events` | **REACTOR** | Reactor Interface + Service + Inquiry + Tests |

### COMMAND 子類型

| Sub-Type | `method` 欄位 | 範例 |
|----------|--------------|------|
| **Constructor** | 包含 "constructor" | `"Product constructor"` |
| **Method-call** | 包含 "." | `"Sprint.start(...)"` |

### 強制阻擋閘門 (Blocking Gates)

| Gate | Phase | 說明 | 失敗後果 |
|------|-------|------|---------|
| JSON Validation | 0.3 | 必要欄位檢查 | STOP，要求修正 spec |
| Dual-Profile Test | 4 | InMemory + Outbox 全部通過 | STOP，回到 Phase 2 |
| Deterministic Review | 5 | 55 rules 靜態掃描 | STOP，回到 Phase 2 |

---

## 2. ezddd-java Skill 用途

ezddd-java 是一個**完全自包含的 AI Coding 工具包**，專為 Java + Spring Boot + ezddd 框架的 DDD 開發設計。

### 四大功能模組

| 模組 | 指令 | 用途 | 說明 |
|------|------|------|------|
| **Project Initializer** | `/init-project` | 初始化專案基礎設施 | 產生 15 個檔案：Main App、Shared Config、Test Infra、Properties |
| **UC Executor** | `/execute-uc` | 從 JSON Spec 產生 DDD 程式碼 | 支援 COMMAND / QUERY / REACTOR 三種 spec type |
| **Code Reviewer** | `/code-review` | 程式碼審查 | 5 種模式：Default、Strict、IntelliJ、Multi-Model、Parallel Layer |
| **Mutation Tester** | `/mutation-test` | Mutation Coverage 提升 | 使用 PIT 識別存活 mutants 並補充測試 |

### 核心設計原則

1. **SPEC-DRIVEN** — Spec defines what to generate, no more, no less
2. **Event Sourcing Golden Rule** — State can ONLY be set in `when()` method
3. **Layered Validation** — UseCase 層業務規則 + Aggregate 層領域不變量
4. **Dual-Profile** — InMemory (快速測試) + Outbox (真實 PostgreSQL) 雙軌並行
5. **自包含架構** — 62 個參考檔案 (~24,400 行)，不依賴外部知識庫

### 與宿主專案的介面

ezddd-java Skill 只需要宿主專案提供兩個東西：

| Convention | Path | 用途 |
|-----------|------|------|
| Project Config | `.dev/project-config.json` | 專案配置 (rootPackage, DB, architecture) |
| JSON Specs | `.dev/specs/{aggregate}/usecase/*.json` | UC 規格檔案 |

這使得 Skill 可以**移植到任何新專案**，只要提供這兩個介面。

### Skill 規模統計

| 類別 | 檔案數 | 行數 |
|------|-------|------|
| Patterns (Domain/UC/Infra/Test/Adapter) | 16 | ~13,000 |
| Rules | 5 | ~1,900 |
| Templates | 8 | ~2,000 |
| Examples | 3 | ~2,100 |
| Framework API | 2 | ~960 |
| Code-Reviewer | 5 | ~1,060 |
| Init-Project | 3 | ~1,040 |
| Mutation-Tester | 4 | ~978 |
| Multi-Model Engine | 9 | ~600 |
| Scripts + Assertions | 7 | ~600 |
| **Total** | **62** | **~24,400** |

---

## 3. Gate 2.5 解說

Gate 2.5 是一個**確定性審查閘門 (Deterministic Review Gate)**——100% pattern matching，0% LLM judgment。

### 設計理念

傳統 Code Review 依賴 LLM 判斷，容易有幻覺和遺漏。Gate 2.5 用 pure regex/grep 驗證 55 條規則，確保生成程式碼不含已知反模式。

### 55 條規則分布

```
55 Rules
├── FORBIDDEN (30 rules)     — 程式碼中不允許出現的 pattern
│   ├── DI/Annotation: F-01~F-04   (@Service/@Component, @ActiveProfiles...)
│   ├── Wrong Import:  F-05~F-08   (javax.persistence, 錯誤 relay path...)
│   ├── Domain:        F-09~F-16   (field initializers, Instant.now()...)
│   ├── UseCase:       F-17~F-20   (raw CqrsOutput, Input record...)
│   ├── Infrastructure:F-21~F-26   (no-arg InMemoryOrmDb, CrudRepository...)
│   └── Security:      F-27~F-30   (hardcoded secrets, @CrossOrigin...)
│
├── REQUIRED (15 rules)      — 程式碼中必須出現的 pattern
│   ├── R-01~R-05: Aggregate 必要結構 (when(), mapper(), metadata...)
│   ├── R-06~R-10: UseCase 必要結構 (Input class, DateProvider...)
│   └── R-11~R-15: Test 必要結構 (@DirtiesContext, setUpEventCapture...)
│
├── STRUCTURE (5 rules)      — Package 結構驗證
│   ├── S-01~S-05: 檔案放置位置 (entity/, usecase/, adapter/...)
│
├── CROSS-FILE (1 rule)      — 跨檔案一致性
│   └── X-01: InMemory + Outbox bean name 一致
│
└── TESTSUITE (4 rules)      — TestSuite 覆蓋率
    ├── TS-01: @SelectPackages 包含 aggregate
    ├── TS-02: OutboxTestSuite 存在
    ├── TS-03: ProfileSetter 存在
    └── TS-04: Source 對應
```

### 執行方式

```bash
# 基本用法
./validate-generated-code.sh --aggregate product

# JSON 輸出（供 CI 整合）
./validate-generated-code.sh --aggregate product --json

# 從檔案清單
./validate-generated-code.sh --files-from filelist.txt
```

### 嚴重等級與行為

| Severity | 行為 | 說明 |
|----------|------|------|
| **CRITICAL** | BLOCKING | 必須修正才能通過 Gate |
| **WARN** | Non-blocking | 記錄但不阻擋 |
| **SKIP** | Informational | 無法檢查（缺少檔案對） |

### 與 Hooks 的整合

Gate 2.5 透過 Claude Code hooks 自動觸發：

- **SessionStart**: `gate25-session-start.sh` — 記錄 Java 檔案 baseline
- **Stop**: `gate25-stop-guard.sh` — 結束時驗證所有新增/修改的 Java 檔案

---

## 4. ezddd-java Skill 用到的 Patterns

ezddd-java Skill 包含 16 個 Pattern 參考檔案（~13,000 行），涵蓋 DDD 所有層次。

### Pattern 分層總覽

```
┌─────────────────────────────────────────────────────────┐
│                    Adapter Layer (1)                      │
│  ┌──────────────────────────────────────────────────┐   │
│  │ controller.md — REST Controller 模式             │   │
│  │ - Constructor injection, @Valid, /v1/api prefix  │   │
│  │ - Request/Response inner classes                 │   │
│  └──────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│                    UseCase Layer (3)                      │
│  ┌───────────────┐ ┌──────────────┐ ┌───────────────┐  │
│  │ command.md    │ │ query.md     │ │ reactor.md    │  │
│  │ - Input class │ │ - Projection │ │ - Event-driven│  │
│  │ - CqrsOutput  │ │ - DTO record │ │ - Inquiry     │  │
│  │ - @Bean reg.  │ │ - Profile-   │ │ - Cross-      │  │
│  │ - Blanket     │ │   aware bean │ │   aggregate   │  │
│  │   catch       │ │ - No blanket │ │               │  │
│  │               │ │   catch      │ │               │  │
│  └───────────────┘ └──────────────┘ └───────────────┘  │
├─────────────────────────────────────────────────────────┤
│                    Domain Layer (4)                       │
│  ┌───────────────┐ ┌──────────────┐ ┌───────────────┐  │
│  │ aggregate.md  │ │ entity.md    │ │value-object.md│  │
│  │ - ES golden   │ │ - Mutable    │ │ - record type │  │
│  │   rule        │ │   child      │ │ - 5 categories│  │
│  │ - when()      │ │ - Field      │ │   (Simple/    │  │
│  │ - apply()     │ │   Mutability │ │    Complex/   │  │
│  │ - Contract    │ │   Analysis   │ │    ID/Enum/   │  │
│  │   helpers     │ │ - pkg-private│ │    Semantic)  │  │
│  │ - Replay ctor │ │   mutation   │ │ - @JsonIgnore │  │
│  └───────────────┘ └──────────────┘ └───────────────┘  │
│  ┌──────────────────────────────────────────────────┐   │
│  │ domain-event.md — Sealed interface + ADR-047     │   │
│  │ - static mapper(), MAPPING_TYPE_PREFIX           │   │
│  │ - Event metadata, Enum degradation (Rule 3.5)    │   │
│  └──────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│                 Infrastructure Layer (5)                  │
│  ┌──────────┐ ┌────────┐ ┌────────┐ ┌──────────────┐  │
│  │config.md │ │mapper  │ │outbox  │ │persistent-   │  │
│  │- UseCaseC│ │.md     │ │.md     │ │object.md     │  │
│  │- InMemory│ │- toData│ │- Outbox│ │- JPA Entity  │  │
│  │  Config  │ │- toDom │ │  Store │ │- OutboxData  │  │
│  │- Outbox  │ │- setVer│ │- Relay │ │  interface   │  │
│  │  Config  │ │  before│ │  Config│ │- @Transient  │  │
│  │- Bean    │ │  clear │ │        │ │              │  │
│  │  naming  │ │        │ │        │ │              │  │
│  └──────────┘ └────────┘ └────────┘ └──────────────┘  │
│  ┌──────────────────────────────────────────────────┐   │
│  │ security-config.md — CORS, Request validation    │   │
│  └──────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│                    Testing Layer (4)                      │
│  ┌───────────────┐ ┌──────────────┐                     │
│  │usecase-test   │ │contract-test │                     │
│  │.md            │ │.md           │                     │
│  │- ezSpec BDD   │ │- Pure JUnit5 │                     │
│  │- @DirtiesCtx  │ │- Precondition│                     │
│  │- setUpEvent   │ │- Postcondition                     │
│  │  Capture()    │ │- DBC contract│                     │
│  └───────────────┘ └──────────────┘                     │
│  ┌───────────────┐ ┌──────────────┐                     │
│  │controller-test│ │repository-   │                     │
│  │.md            │ │test.md       │                     │
│  │- @WebMvcTest  │ │- Profile     │                     │
│  │- MockMvc      │ │  isolation   │                     │
│  │- REST Assured │ │- @DirtiesCtx │                     │
│  └───────────────┘ └──────────────┘                     │
└─────────────────────────────────────────────────────────┘
```

### 每個 Pattern 的核心規則

| Pattern | 核心規則 |
|---------|---------|
| **aggregate.md** | State ONLY in `when()`; public constructor (not factory); no field initializers; replay constructor `(List<Events>)` |
| **domain-event.md** | `sealed interface`; `static mapper()` for ADR-047; `MAPPING_TYPE_PREFIX`; Enum degradation to String (Rule 3.5) |
| **value-object.md** | `record implements ValueObject`; compact constructor + `Objects.requireNonNull()`; `toString()` returns raw value |
| **entity.md** | Field Mutability Analysis; package-private mutation methods; mutable child of Aggregate |
| **command.md** | Input is `class` (not record) with `create()` factory; `CqrsOutput<?>`; blanket catch; `@Bean` registration |
| **query.md** | Projection in profile-specific config; NO blanket catch; DTO as record |
| **reactor.md** | Event-driven; Inquiry for cross-aggregate; no `@Service`/`@Component` |
| **config.md** | Same bean name for InMemory/Outbox; UseCaseConfig no `@Profile`; `InMemoryOrmDb(Map)` constructor |
| **mapper.md** | `OutboxMapper<Agg, Data>` (2 type params); `setVersion()` before `clearDomainEvents()` |
| **persistent-object.md** | `implements OutboxData<String>` (interface, not class); `@Transient` on interface methods |
| **usecase-test.md** | ezSpec BDD; `@DirtiesContext(AFTER_EACH_TEST_METHOD)`; `setUpEventCapture()`; no `@ActiveProfiles` |
| **contract-test.md** | Pure JUnit 5, no Spring; precondition + postcondition + invariant |
| **controller.md** | Constructor injection; `@Valid @RequestBody`; `/v1/api` prefix; thin delegation |

---

## 5. SSOA 機制 (Single Source of Authority)

SSOA 是 ezddd-java Skill 的**知識一致性治理機制**，確保每個概念只在一處完整定義。

### 核心原則

> 修改 pattern/rule 前，先查 `AUTHORITY-REGISTRY.yaml` 找到權威來源。
> 只修改 authority 檔案，再同步所有 consumers。

### 運作架構

```
┌─────────────────────────────────────────────────────────────┐
│                 AUTHORITY-REGISTRY.yaml                       │
│                 (Single Source of Truth Index)                │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Topic: command_input_type                            │    │
│  │ ┌───────────────────────────────────────────────┐   │    │
│  │ │ Authority: patterns/usecase/command.md (L172) │   │    │
│  │ │ Canonical: "class.*Input implements Input"    │   │    │
│  │ │ Anti-pattern: "record.*Input.*implements"     │   │    │
│  │ │ Severity: CRITICAL                            │   │    │
│  │ └───────────────────────────────────────────────┘   │    │
│  │                     │                                │    │
│  │          ┌──────────┴──────────┐                     │    │
│  │          ▼                     ▼                     │    │
│  │  ┌──────────────┐    ┌──────────────────┐           │    │
│  │  │ consumers:   │    │ jit_consumers:   │           │    │
│  │  │ - rules/     │    │ - critical-rules │           │    │
│  │  │   usecase-   │    │   .md #22        │           │    │
│  │  │   patterns   │    └──────────────────┘           │    │
│  │  │ - rules/     │                                   │    │
│  │  │   framework  │    ┌──────────────────┐           │    │
│  │  │ - code-      │    │template_consumers│           │    │
│  │  │   reviewer/  │    │ - templates/     │           │    │
│  │  │   checklist  │    │   test-case-full │           │    │
│  │  │ - framework/ │    └──────────────────┘           │    │
│  │  │   ezapp-api  │                                   │    │
│  │  └──────────────┘                                   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  Topics (37 個): usecase(5), domain(8), infrastructure(7),   │
│  testing(7), workflow(3), forbidden(5), security(3),         │
│  multi-model(2), uc-executor(3), conventions(5)              │
└─────────────────────────────────────────────────────────────┘
```

### Topic 結構定義

每個 Topic 包含以下欄位：

| 欄位 | 用途 |
|------|------|
| `description` | 一行說明 |
| `authority` | 權威來源檔案 + 行號 hint |
| `canonical_pattern` | 正確寫法的 regex |
| `anti_pattern` | 錯誤寫法的 regex |
| `severity` | critical / high / must / warn |
| `group` | 分類 (usecase, domain, infrastructure...) |
| `consumers[]` | 必須與 authority 同步的檔案清單 |
| `jit_consumers[]` | JIT 載入時的 consumer (如 critical-rules.md) |
| `template_consumers[]` | 模板類 consumer |

### Consumer 類型

| Type | 說明 | 驗證方式 |
|------|------|---------|
| **reference** (default) | 必須包含 canonical pattern | grep for canonical |
| **rule** | 用文字描述規則 | 不檢查 canonical pattern |
| **template_consumers** | 模板檔案 | 驗證對齊 |
| **jit_consumers** | 載入時的 consumer | Rule number 對照 |

### 自動化驗證

```bash
# Pattern 一致性檢查（6 phases）
./scripts/check-pattern-consistency.sh

# Pattern 斷言執行（3 assertion files）
./scripts/run-pattern-assertions.sh
```

### 維護規則

1. 修改前查 `AUTHORITY-REGISTRY.yaml` 找 authority
2. 只修改 authority 檔案
3. 更新所有 consumers
4. 新概念先在 registry 註冊
5. 模板修改後執行 `consistency-checklist.yaml XR-07`

---

## 6. ezddd-java Skill 的 Hooks

ezddd-java Skill 透過 Claude Code 的 hooks 系統實現自動化品質控管。

### Hook 設定檔位置

`.claude/settings.json`

### 三個 Hook 觸發點

```
┌──────────────────────────────────────────────────────────────┐
│                    Claude Code Hooks                          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  1. SessionStart (每次對話開始)                               │
│  ┌──────────────────────────────────────────────┐            │
│  │ Matcher: *                                    │            │
│  │ Script:  gate25-session-start.sh              │            │
│  │ Timeout: 10s                                  │            │
│  │ Purpose: 記錄 Java 檔案 baseline              │            │
│  │          (Gate 2.5 用於比對新增/修改的檔案)    │            │
│  └──────────────────────────────────────────────┘            │
│                                                              │
│  2. PostToolUse (每次 Edit/Write 工具呼叫後)                  │
│  ┌──────────────────────────────────────────────┐            │
│  │ Matcher: Edit|Write                           │            │
│  │                                               │            │
│  │ Hook A: check-authority-consumers.py          │            │
│  │   Timeout: 10s                                │            │
│  │   Purpose: SSOA 一致性檢查                     │            │
│  │   - 偵測被修改的檔案是否為某 topic 的 authority │            │
│  │   - 若是，提醒必須同步更新所有 consumers       │            │
│  │   - 雙向檢查 (bidirectional v2.0)              │            │
│  │                                               │            │
│  │ Hook B: audit-docs-hook.sh                    │            │
│  │   Timeout: 10s                                │            │
│  │   Purpose: 文件引用審計                        │            │
│  │   - 檢查是否引用了不存在的參考文件              │            │
│  └──────────────────────────────────────────────┘            │
│                                                              │
│  3. Stop (對話結束時)                                         │
│  ┌──────────────────────────────────────────────┐            │
│  │ Matcher: *                                    │            │
│  │ Script:  gate25-stop-guard.sh                 │            │
│  │ Timeout: 30s                                  │            │
│  │ Purpose: Gate 2.5 終止守衛                     │            │
│  │   - 比對 SessionStart 記錄的 baseline          │            │
│  │   - 對所有新增/修改的 Java 檔案執行驗證         │            │
│  │   - 有 CRITICAL 違規時發出警告                  │            │
│  └──────────────────────────────────────────────┘            │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Hook 腳本位置

| Hook Script | Location | Type |
|------------|----------|------|
| `gate25-session-start.sh` | `.claude/scripts/` | Bash |
| `check-authority-consumers.py` | `.claude/scripts/` | Python 3 |
| `audit-docs-hook.sh` + `audit-docs.py` | `.claude/scripts/` | Bash + Python 3 |
| `gate25-stop-guard.sh` | `.claude/scripts/` | Bash |

### Skill 內部腳本（非 Hook，手動或 Pipeline 觸發）

| Script | Location | 用途 |
|--------|----------|------|
| `validate-generated-code.sh` | `.claude/skills/ezddd-java/scripts/` | Gate 2.5 確定性審查 (55 rules) |
| `check-pattern-consistency.sh` | `.claude/skills/ezddd-java/scripts/` | SSOA pattern 一致性檢查 (6 phases) |
| `run-pattern-assertions.sh` | `.claude/skills/ezddd-java/scripts/` | Pattern 斷言自動化驗證 |
| `analyze-multi-model-metrics.sh` | `.claude/skills/ezddd-java/scripts/` | Multi-Model Review 指標分析 |

### Hook 與 Gate 2.5 的協作流程

```
Session Start
    │
    ▼
gate25-session-start.sh
    │  記錄 baseline: 哪些 .java 檔案已存在
    │
    ▼
  工作中...
    │
    ├─ Edit/Write Java 檔案
    │     │
    │     ├─ check-authority-consumers.py (SSOA 檢查)
    │     └─ audit-docs-hook.sh (文件審計)
    │
    ├─ /execute-uc (手動觸發 Gate 2.5)
    │     │
    │     └─ validate-generated-code.sh --aggregate xxx
    │
    ▼
Session Stop
    │
    ▼
gate25-stop-guard.sh
    │  比對 baseline → 找出新增/修改的 Java 檔案
    │  對這些檔案執行 Gate 2.5 rules
    │  有 CRITICAL → 警告（不強制阻擋，但顯示）
    ▼
  Done
```

---

## 總結

ezddd-java Skill 是一個高度工程化的 AI Coding 工具包，其核心特點：

1. **Pipeline 自動化** — execute-uc 從 JSON spec 到可測試程式碼，7 個 Phase 自動執行
2. **雙重品質閘門** — Gate 1 (測試) + Gate 2.5 (靜態分析) 確保輸出品質
3. **知識治理** — SSOA + AUTHORITY-REGISTRY 確保 37 個 topic 的一致性
4. **Pattern 驅動** — 16 個 pattern 檔案 (~13,000 行) 覆蓋 DDD 所有層次
5. **Hook 自動化** — 3 個觸發點 (SessionStart/PostToolUse/Stop) 自動執行品質檢查
6. **自包含可移植** — 只需 `project-config.json` + JSON specs 即可移植到新專案
