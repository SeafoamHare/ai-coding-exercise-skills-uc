# Code Review Checklist (ezddd-java)

## File Type Identification

| File Pattern | Category | Priority |
|-------------|----------|----------|
| `**/entity/{Aggregate}.java` | Aggregate Root | CRITICAL |
| `**/entity/*Events.java` | Domain Event | CRITICAL |
| `**/entity/*Id.java` | Value Object (ID) | HIGH |
| `**/entity/ReadOnly*.java` | Read-only Entity | HIGH |
| `**/entity/*.java` (class) | Entity (Internal) | MEDIUM |
| `**/entity/*.java` (record) | Value Object | MEDIUM |
| `**/entity/*State.java` (enum) | Enum | LOW (SKIP) |
| `**/usecase/service/*.java` | Use Case Service | HIGH |
| `**/usecase/port/in/*.java` | Use Case Interface | MEDIUM |
| `**/controller/*.java` | Controller | MEDIUM |
| `**/*Test.java` | Test | MEDIUM |
| `**/*ContractTest.java` | Contract Test | HIGH |
| `**/usecase/port/*Mapper.java` | Mapper | LOW |
| `**/*Data.java` | Data Class (PO) | MEDIUM |
| `**/*RepositoryConfig*.java` | Repository Config | LOW |
| `**/*OrmClient.java` | ORM Client | LOW |

---

## Level 1 - Architecture & Structure (CRITICAL)

### Package Location Check

- [ ] **UseCase interface** → `[aggregate]/usecase/port/in/`
- [ ] **Service implementation** → `[aggregate]/usecase/service/`
- [ ] **Entity/Aggregate** → `[aggregate]/entity/`
- [ ] **Controller** → `[aggregate]/adapter/in/rest/springboot/`
- [ ] **Repository** → `[aggregate]/adapter/out/repository/`
- [ ] **Mapper** → `[aggregate]/usecase/port/` (NOT `adapter/out/mapper/`)

### Clean Architecture Layers

- [ ] Domain layer has NO framework dependencies
- [ ] UseCase layer depends only on Domain
- [ ] Adapter layer depends on UseCase
- [ ] Package declaration matches actual path

---

## Level 2 - File Type Specific Checks (HIGH)

### Aggregate Root Checklist (CRITICAL)

**Golden Rule**: State can ONLY be set in `when()` method.

```java
// CORRECT - state in when()
private void when(SprintStarted event) {
    this.state = SprintState.STARTED;  // OK
}

// WRONG - state outside when()
public void start() {
    this.state = SprintState.STARTED;  // FORBIDDEN!
}
```

**Checklist:**

- [ ] Extends `EsAggregateRoot<ID, Events>`
- [ ] **No instance field initializer** (`= false`/`= new ArrayList<>()`/`= 0`/`= null` 全部禁止) — field initializer 在 `super(events)` 之後執行，會覆蓋 event replay 狀態（見 aggregate.md Rule 11）
- [ ] All fields initialized in `when(ConstructionEvent)` — 包括 primitives 和 collections
- [ ] Constructor does NOT set state fields directly
- [ ] Constructor calls `apply(event)` not just `addDomainEvent()`
- [ ] Event parameters use constructor params, not `this.xxx`
- [ ] ALL state assignments only in `when()` method
- [ ] Uses `DateProvider.now()` not `Instant.now()`
- [ ] Contract helpers use `_` prefix (PIT mutation testing support)
- [ ] Uses `Objects.equals()` for nullable comparison
- [ ] Has soft-delete support (uses **inherited** `isDeleted` from `EsAggregateRoot`, do NOT declare own field)
- [ ] Uses `if (ignore(...)) return;` for idempotency
- [ ] **MUST NOT declare `version` or `isDeleted` fields** — inherited from `EsAggregateRoot` (field shadowing causes silent failures: `isDeleted()` always returns `false`, `getVersion()` always returns `0`)
- [ ] **Read-only exposure (Rule 13)**: every getter returning a child entity / entity collection returns the `ReadOnly{Entity}` / an unmodifiable collection — NEVER the live internal object (`return goal;` / `return metrics;` is a defect)

**Semantics Compliance:**

| Semantics Tag | Check |
|---------------|-------|
| `value_immutable` | No setter, no mutating event, only set at creation |
| `collection_reference_immutable` | No `setXxx(List)`, modify via behavior methods |
| `soft_delete_flag` | Has DestructionEvent |
| `readonly_exposure` | Returned entity wrapped in `ReadOnly{Entity}`; `ReadOnly` class exists with throwing commands |

**Postcondition Check:**

<!-- @authority: postcondition_event_verification | source: patterns/domain/aggregate.md -->

- [ ] Postcondition helper verifies aggregate ID field (e.g., `_productIdMatches()`)
- [ ] Postcondition helper verifies **ALL** business fields changed by the command (e.g., `_nameMatches()`, `_stateMatches()`)
- [ ] Postcondition helper verifies computed fields
- [ ] **每個 command 方法都有 `_xxxEventGenerated()` helper** 驗證 domain event 正確產生（參見 `references/examples/Plan.java`）
- [ ] `ensure()` 使用 `format()` 提供具體錯誤訊息（e.g., `ensure(format("Name is '%s'", name), () -> _nameMatches(name))`）

---

<!-- @authority: domain_event_mapper_key | source: patterns/domain/domain-event.md -->

### Domain Event Checklist (CRITICAL)

**Location**: `[aggregate]/entity/[Aggregate]Events.java`

- [ ] `sealed interface` extends `InternalDomainEvent`
- [ ] Has `{aggregate}Id()` method at interface level
- [ ] Has `source()` default method at interface level, returning `{aggregate}Id().value()` (aggregate instance ID)
- [ ] **No per-record `source()` override** — all records inherit from interface (DRY)
- [ ] **No `aggregateId()` method** — removed from `InternalDomainEvent`
- [ ] Event records have `metadata` field (`Map<String, String>`)
- [ ] Metadata initialized with `new HashMap<>()` (NOT `Map.of()`)
- [ ] Uses `InternalDomainEvent.ConstructionEvent/DestructionEvent` (not custom)
- [ ] Uses `Objects.requireNonNull()` (NOT Contract)
- [ ] Has inline `static mapper()` with `MAPPING_TYPE_PREFIX` keys (authority: `domain-event.md`)
- [ ] Has `static mapper()` method (ADR-047 Auto-Registration)

**Example:**
```java
public sealed interface ProductEvents extends InternalDomainEvent {
    ProductId productId();

    @Override
    default String source() { return productId().value(); }

    String MAPPING_TYPE_PREFIX = "ProductEvents$";

    record ProductCreated(
        ProductId productId,
        ProductName name,
        Map<String, String> metadata,
        UUID id,
        Instant occurredOn
    ) implements ProductEvents, ConstructionEvent {
        public ProductCreated {
            Objects.requireNonNull(productId);
            Objects.requireNonNull(name);
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(id);
            Objects.requireNonNull(occurredOn);
        }
        // No source() override — inherits from interface
    }

    static DomainEventTypeMapper mapper() {
        DomainEventTypeMapper mapper = DomainEventTypeMapper.create();
        mapper.put(MAPPING_TYPE_PREFIX + "ProductCreated", ProductCreated.class);
        return mapper;
    }
}
```

---

### Entity (Internal) Checklist (MEDIUM)

- [ ] Implements `Entity<ID>` interface
- [ ] Uses `Objects.requireNonNull()` (NOT Contract)
- [ ] Has `equals/hashCode` based on ID
- [ ] Only accessible through Aggregate Root
- [ ] No direct repository access
- [ ] Mutation methods are package-private (NOT public)
- [ ] Collection getters return `Collections.unmodifiable*` (never a live internal list — Fowler)
- [ ] **If the aggregate root returns this entity:** a `ReadOnly{Entity}` exists; entity is
      non-final with a `protected` copy constructor (Read-only Entities pattern, entity.md Rule 11)

### Read-only Entity (`ReadOnly*`) Checklist (CRITICAL when present)

- [ ] `ReadOnly{Entity} extends {Entity}` (type preserved — Ubiquitous Language)
- [ ] Every command method overridden to `throw new UnsupportedOperationException(...)`
- [ ] Query methods returning entities return read-only variants; collections are unmodifiable
- [ ] Constructor delegates to the base copy constructor (`super(source)`)
- [ ] ⚠️ The throwing commands are a **deliberate LSP trade-off** — do NOT flag as a bug

---

### Value Object Checklist (MEDIUM)

- [ ] Uses `record` or immutable `final class`
- [ ] Implements `ValueObject` interface (**enum is EXEMPT**)
- [ ] Uses `Objects.requireNonNull()` for validation
- [ ] **`*Id` records (ID 型別)**: Has 3 factory methods — `valueOf(String)`, `valueOf(UUID)`, `create()`
- [ ] **Non-ID records (非 ID 型別)**: Has 2 factory methods — `valueOf(String)`, `valueOf(UUID)` (no `create()`)
- [ ] NO setter methods (immutable)
- [ ] Has `value()` method for primitive wrapper types
- [ ] **`*Id` records**: `toString()` overridden to return raw `value` (NOT default `TypeName[value=xxx]`) ← F1
- [ ] **Record VOs with `is*()`/`get*()`/`has*()` non-constructor methods**: ALL annotated with `@JsonIgnore` ← F4

**Example:**
```java
// ID Value Object — 3 factory methods: valueOf(String), valueOf(UUID), create()
public record ProductId(String value) implements ValueObject {
    public ProductId {
        Objects.requireNonNull(value, "ProductId value cannot be null");
    }

    public static ProductId create() {
        return new ProductId(UUID.randomUUID().toString());
    }

    public static ProductId valueOf(String value) {
        return new ProductId(value);
    }

    public static ProductId valueOf(UUID value) {
        return new ProductId(value.toString());
    }

    @Override
    public String toString() { return value; }
}

// Non-ID Value Object — 2 factory methods: valueOf(String), valueOf(UUID)
public record ProductName(String value) implements ValueObject {
    public ProductName {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException("Name cannot be blank");
    }

    public static ProductName valueOf(String value) {
        return new ProductName(value);
    }

    public static ProductName valueOf(UUID value) {
        return new ProductName(value.toString());
    }
}

// Jackson Safety: @JsonIgnore on derived methods
public record WipLimit(int value) implements ValueObject {
    @JsonIgnore
    public boolean isUnlimited() { return value == -1; }
}
```

---

### Use Case Service Checklist (HIGH)

- [ ] NO `@Component` or `@Service` annotation (use `@Bean`)
- [ ] `Input/Output` are inner classes of UseCase interface
- [ ] Uses `requireNotNull()` for preconditions (via `import static Contract.*`)
- [ ] Has `error_mapping` checks (from spec)
- [ ] Returns `CqrsOutput` for commands
- [ ] Repository uses only 3 methods (`findById`, `save`, `delete`)
- [ ] Uses blanket catch pattern (`catch (Exception e) { throw new UseCaseFailureException(e); }`)
- [ ] Business errors return `CqrsOutput` with `ExitCode.FAILURE` + message (not throw IAE)
- [ ] Uses `findById().orElse(null)` + null check for Update/Delete commands
- [ ] Contracts (`requireNotNull`) are OUTSIDE try block

**Example:**
```java
public class CreateProductService implements CreateProductUseCase {
    @Override
    public CqrsOutput<?> execute(CreateProductInput input) {
        requireNotNull("Input", input);
        requireNotNull("Product name", input.name);  // Outside try — field access, not method

        try {
            ProductId productId = ProductId.valueOf(input.id);
            Product product = new Product(productId, input.name);
            repository.save(product);
            return CqrsOutput.create()
                .setId(input.id)
                .succeed();
        } catch (Exception e) {
            throw new UseCaseFailureException(e);
        }
    }
}
```

---

### Controller Checklist (MEDIUM)

- [ ] Uses `@RestController`
- [ ] Proper `@RequestMapping` path design
- [ ] Nested paths for creation: `POST /v1/api/products/{productId}/pbis`
- [ ] Flat paths for resources: `GET /v1/api/pbis/{pbiId}`
- [ ] Appropriate HTTP status codes
- [ ] Handles exceptions properly

---

### Test Checklist (MEDIUM)

- [ ] Uses ezSpec BDD (`@EzScenario`)
- [ ] Ends with `.Execute()` (not `.run()`)
- [ ] Given/When only use Use Case (not direct Aggregate)
- [ ] Test IDs use UUID
- [ ] Spring DI for Repository (`@Autowired`)
- [ ] NO `@ActiveProfiles` annotation (violates ADR-021)
- [ ] Extends `BaseUseCaseTest`
- [ ] Has `@DirtiesContext(AFTER_EACH_TEST_METHOD)`
- [ ] MUST manually call `setUpEventCapture()` in `@BeforeEach` and `tearDownEventCapture()` in `@AfterEach`

**⚠️ Cross-Reference: TestSuite Profile 驗證 (CRITICAL)**

> 每次 review test 檔案時，**必須同時驗證** TestSuite 配置是否完整。
> 這是跨檔案關注點，不會自動被單一檔案 review 觸發。

- [ ] **CROSS-REF**: 該 aggregate 的 package 已加入 `InMemoryTestSuite.java` 的 `@SelectPackages`
- [ ] **CROSS-REF**: 該 aggregate 的 package 已加入 `OutboxTestSuite.java` 的 `@SelectPackages`
- [ ] **CROSS-REF**: TestSuite 使用 `ProfileSetter` 內部類別（非 `@ActiveProfiles`）

---

### Dual Profile TestSuite Checklist (when `dualProfileSupport = true`) ⭐⭐⭐

- [ ] Has `{UseCase}ServiceTest.java`
- [ ] Global `InMemoryTestSuite.java` exists (全專案一個，不是 per-use-case)
- [ ] Global `OutboxTestSuite.java` exists (全專案一個，不是 per-use-case)
- [ ] `@SelectClasses` contains only `ProfileSetter.class`
- [ ] `@SelectPackages` includes **ALL** existing aggregate packages（掃描 `src/main/java` 下所有 aggregate 目錄）
- [ ] `@ExcludeClassNamePatterns(".*ControllerTest")` present
- [ ] NO per-use-case TestSuite (如 ~~`InMemoryCreateProductTestSuite`~~)
- [ ] NO `@ActiveProfiles` (violates ADR-021)
- [ ] `ProfileSetter` 是 `public static class`，在 `static {}` 區塊設定 `spring.profiles.active`
- [ ] InMemory ProfileSetter 排除 JDBC/JPA/Flyway autoconfiguration
- [ ] Outbox ProfileSetter 設定 PostgreSQL 連線和 `ddl-auto=update`

**⚠️ 常見遺漏**：新增 aggregate 後忘記將其 package 加入 `@SelectPackages`。
這會導致該 aggregate 的測試「看似通過」（`mvn test -Dtest=ClassName` 可跑），
但實際上 **不會被 TestSuite 收錄**，dual-profile 測試形同虛設。

---

### Contract Test Checklist (HIGH)

**Location**: `[aggregate]/entity/{Aggregate}ContractTest.java`

- [ ] Pure JUnit 5 (NO Spring annotations)
- [ ] Uses `@Nested` class per command method
- [ ] Uses `assertThrows(PreconditionViolationException.class, ...)`
- [ ] Has `create{Aggregate}WithState()` helper method
- [ ] In `entity/` package (NOT `usecase/service/`)

---

### Mapper Checklist (LOW)

**Location**: `[aggregate]/usecase/port/`

- [ ] In `usecase/port/` package (NOT `adapter/out/mapper/`)
- [ ] All methods are `public static`
- [ ] NO `@Component/@Service`
- [ ] `ObjectMapper` has `JavaTimeModule` registered
- [ ] `toData/toDomain` are symmetric (no missing field conversion)
- [ ] **Child entities properly serialized in `toData()`** (NOT hardcoded `"[]"`) ← F3
- [ ] **`toDomain()` child entity reconstruction uses correct strategy**: command methods (flat collections) OR direct population (recursive tree) ← F3
- [ ] **`toDomain()` does NOT call domain methods for tree reconstruction** (avoids phantom events + precondition failures) ← F3

---

### Data Class (PO) Checklist (MEDIUM)

- [ ] NO `@Enumerated` on String fields
- [ ] NO enum type fields (use String)
- [ ] Outbox fields have `@Transient`
- [ ] Implements `OutboxData<String>` for Outbox pattern
- [ ] Column names use snake_case

---

### Repository Config Checklist (LOW)

- [ ] Uses generic `Repository<T, ID>` interface
- [ ] NO custom Repository interface
- [ ] NO empty inheritance class
- [ ] Uses `@Bean` (NOT `@Component`)
- [ ] Only 3 methods: `findById`, `save`, `delete`

### OrmClient Checklist (LOW)

- [ ] Extends `SpringJpaClient<Data, String>` (NOT raw `JpaRepository`)
- [ ] Located in `[aggregate]/io/springboot/config/orm/` package
- [ ] Annotated with `@Repository`
- [ ] Interface name follows `{Aggregate}OrmClient` convention

---

## Level 3 - Business Logic (MEDIUM)

- [ ] Spec requirements implemented correctly
- [ ] Contract validation complete
- [ ] Error handling appropriate
- [ ] Edge cases covered

---

## Level 4 - Security Review (MEDIUM)

<!-- @authority: no_hardcoded_secrets | source: rules/security-patterns.md -->
<!-- @authority: cors_centralized_config | source: rules/security-patterns.md -->
<!-- @authority: request_dto_validation | source: patterns/adapter/controller.md -->

### Secret Management
- [ ] No hardcoded passwords/secrets in Java source (`security-patterns.md` Rule 1)
- [ ] Properties files use `${PLACEHOLDER}` for credentials (`security-patterns.md` Rule 3)
- [ ] `.gitignore` includes `.env`, `*.key`, `*.pem`, `credentials.json` (`security-patterns.md` Rule 2)

### Input Validation (Controllers)
- [ ] All `@RequestBody` parameters have `@Valid` (Gate 2.5: R-15)
- [ ] String fields: `@NotBlank` (required) + `@Size(max=N)` (free text) (`security-patterns.md` Rule 4)
- [ ] Path variables validated for null/blank/"null" (`controller.md` Rule 7)
- [ ] Collection fields have `@Size(max=N)` (prevent payload bombs)

### CORS/CSRF
- [ ] CORS via centralized `CorsConfig.java` (NOT `@CrossOrigin`) (Gate 2.5: F-30)
- [ ] No wildcard `*` in allowed origins (`security-config.md` Rule 1)
- [ ] CORS scoped to `/v1/api/**` only (`security-config.md` Rule 2)

---

## Rating Criteria

| Rating | Criteria |
|--------|----------|
| 5 stars | No issues found |
| 4 stars | Minor style issues only |
| 3 stars | Some SHOULD FIX issues |
| 2 stars | Multiple MUST FIX issues |
| 1 star | CRITICAL issues (Event Sourcing violation, etc.) |

---

## Report Template

```markdown
## Code Review: {FileName}

### File Type: {Type}

### Checklist Results

| Check Item | Result | Location | Issue |
|------------|--------|----------|-------|
| Item 1 | PASS/FAIL | Line # | Description |
| Item 2 | PASS/FAIL | Line # | Description |

### Summary

- **Critical Issues**: X
- **Must Fix**: Y
- **Should Fix**: Z
- **Rating**: X/5 stars

### Key Issues

1. {Issue 1}
2. {Issue 2}

### Recommendations

1. {Recommendation 1}
2. {Recommendation 2}
```

---

## Strict Mode Triggers

Automatic strict mode for:
- Aggregate Root files
- Domain Event files

Manual trigger keywords:
- `--strict`
- `嚴格模式`
- `完整 review`
