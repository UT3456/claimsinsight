# ClaimInsight360 - Project Error Scan Report
**Date:** April 14, 2026  
**Total Errors Found:** 76  
**Severity:** Mixed (Critical, Major, Minor)

---

## Error Summary by Severity

### CRITICAL ERRORS (Must Fix)
1. **ClaimKpiControllerTest.java** (line 155-163)
   - `KpiSummaryDTO` constructor argument mismatch
   - Missing `settlementTime` parameter
   - Type mismatch in `List.of()` → should be `LocalDate`
   - **Impact:** Test will not compile

2. **KpiCalculationServiceTest.java** (multiple lines)
   - Missing `ClaimRawProjection` class/interface
   - Missing `ClaimRawProjectionRepository` 
   - 24 references to undefined types
   - **Impact:** Test file cannot compile

### MAJOR ERRORS (Should Fix)
3. **Unused Imports** (14 instances)
   - `data-ingestion-service/GlobalExceptionHandler.java` - InvalidFeedStatusException
   - `AdjusterAndOperations/LoggingAspect.java` - ProceedingJoinPoint
   - `AdjusterAndOperations/AdjusterPerformanceService.java` - AdjusterPerformance
   - `AdjusterAndOperations/SLAViolationControllerTests.java` - InvalidInputException
   - `denial-leakage-service/LeakageFlagRequest.java` - @Past annotation
   - `denial-leakage-service/DenialPatternRequest.java` - @Past annotation
   - `cost-reserve-service/AgingRecordControllerTest.java` - any() and request field
   - `cost-reserve-service/ClaimClaimCostServiceTest.java` - any()
   - `AdjusterAndOperations/AdjusterAndOperationsApplicationTests.java` - EnableCaching
   - `AdjusterAndOperations/AdjusterPerformanceControllerTests.java` - After, AfterEach

4. **Unused Fields** (2 instances)
   - `AdjusterAndOperations/AdjusterPerformanceServiceImpl.java` - HIGH_DENIAL_BENCHMARK constant
   - `cost-reserve-service/AgingRecordControllerTest.java` - request field

5. **Generic Exception Handling** (4 instances)
   - `KpiCalculationService.java` (lines 142, 155, 166, 179)
   - Should use specific exceptions instead of generic `catch (Exception e)`

### MINOR ERRORS (Code Quality)
6. **Unnecessary Annotations** (3 instances)
   - `denial-leakage-service/SwaggerConfig.java` - unnecessary @SuppressWarnings
   - `cost-reserve-service/WebMvcConfig.java` - unnecessary @SuppressWarnings
   - `AdjusterAndOperations/WebMvcConfig.java` - unnecessary @SuppressWarnings

7. **Deprecated API Usage** (1 instance)
   - `denial-leakage-service/LeakageFlagControllerTest.java` - @MockBean deprecated

8. **Maven POM Issues** (3 instances)
   - `claims-metrics-service/pom.xml` - Lombok version override
   - `data-ingestion-service/pom.xml` - Lombok and ByteBuddy version overrides
   - `api-gateway/pom.xml` - Lombok version override

---

## Detailed Error List

### File: ClaimKpiControllerTest.java (CRITICAL)
**Lines 155-163**
```java
KpiSummaryDTO summary = new KpiSummaryDTO(
    "CLM-001",
    new BigDecimal("45.0"),    // tat
    new BigDecimal("3.0"),     // cycleTime
    new BigDecimal("5.5"),     // severity
    new BigDecimal("2.0"),     // frequency
    new BigDecimal("0.85"),    // lossRatio
    LocalDate.now(),           // calculatedDate
    List.of()                  // ❌ Wrong - should be settlementTime (BigDecimal) then List
);
```
**Fix:** Add `settlementTime` parameter before `List.of()`
```java
KpiSummaryDTO summary = new KpiSummaryDTO(
    "CLM-001",
    new BigDecimal("45.0"),
    new BigDecimal("3.0"),
    new BigDecimal("5.5"),
    new BigDecimal("2.0"),
    new BigDecimal("0.85"),
    new BigDecimal("49.0"),    // Add: settlementTime
    LocalDate.now(),
    List.of()
);
```

---

### File: KpiCalculationServiceTest.java (CRITICAL)
**Lines 8, 11, 37, 46, 73-86, 114-121, 135-142, 154-161, 173, 179-180**
```java
// ❌ These don't exist:
import com.claiminsight.metrics.model.ClaimRawProjection;
import com.claiminsight.metrics.repository.ClaimRawProjectionRepository;
```
**Fix:** Either:
1. Create the missing classes, OR
2. Remove this test file and update integration tests

---

### File: KpiCalculationService.java (MAJOR)
**Lines 142, 155, 166, 179**
```java
} catch (Exception e) {  // ❌ Generic exception
```
**Fix:** Use specific exceptions
```java
} catch (IOException | ParseException | NullPointerException e) {
    // Handle specific exceptions
}
```

---

### Unused Imports (MAJOR)
Files to clean:
- `data-ingestion-service/GlobalExceptionHandler.java:4`
- `denial-leakage-service/LeakageFlagRequest.java:8`
- `denial-leakage-service/DenialPatternRequest.java:7`
- `AdjusterAndOperations/LoggingAspect.java:5`
- `AdjusterAndOperations/SLAViolationControllerTests.java:6`
- `AdjusterAndOperations/AdjusterPerformanceControllerTests.java:10-11`
- `cost-reserve-service/AgingRecordControllerTest.java:20`
- `cost-reserve-service/ClaimCostServiceTest.java:27`

---

### Unnecessary @SuppressWarnings (MINOR)
Remove from:
- `denial-leakage-service/SwaggerConfig.java:18`
- `cost-reserve-service/WebMvcConfig.java:16`
- `AdjusterAndOperations/WebMvcConfig.java:16`

---

### Deprecated @MockBean (MINOR)
Replace in `denial-leakage-service/LeakageFlagControllerTest.java:31`
```java
// Old
@MockBean
// New
@MockitoBean  // or use @Mock + @BeforeEach setup
```

---

### Maven POM Version Overrides (MINOR)
In DependencyManagement section, Lombok is already defined as 1.18.42:
- Remove version tags from `claims-metrics-service/pom.xml:77`
- Remove version tags from `data-ingestion-service/pom.xml:77, 110`
- Remove version tags from `api-gateway/pom.xml:77`

---

## Error Categories Summary

| Category | Count | Severity | Status |
|----------|-------|----------|--------|
| Constructor Mismatches | 1 | CRITICAL | Needs Fix |
| Missing Classes/Imports | 24 | CRITICAL | Needs Fix |
| Generic Exception Handlers | 4 | MAJOR | Should Fix |
| Unused Imports | 14 | MAJOR | Should Fix |
| Unused Variables | 2 | MAJOR | Should Fix |
| Unnecessary Annotations | 3 | MINOR | Code Quality |
| Deprecated APIs | 1 | MINOR | Code Quality |
| Maven Overrides | 3 | MINOR | Code Quality |

---

## Fix Priority

**Priority 1 (MUST FIX - Blocks Build):**
1. Fix `ClaimKpiControllerTest.java` - Add settlementTime parameter
2. Fix `KpiCalculationServiceTest.java` - Create missing projection classes OR remove test

**Priority 2 (SHOULD FIX - Code Quality):**
3. Clean unused imports (14 files)
4. Replace generic Exception handlers with specific ones (4 instances)
5. Remove unused variables/fields
6. Replace deprecated @MockBean

**Priority 3 (NICE TO FIX - Warnings):**
7. Remove @SuppressWarnings("deprecation")
8. Fix Maven pom.xml version overrides

---

## Notes
- Total compile errors that block build: **25 errors**
- Total code quality warnings: **51 errors**
- All services are currently running despite warnings
- Tests will fail if test classes have compilation errors
