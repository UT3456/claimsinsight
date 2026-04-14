# IntelliJ IDEA Setup — Fix ExceptionInInitializerError

## Root Cause
`java.lang.ExceptionInInitializerError` during build in IntelliJ is caused by
**Lombok annotation processing not being enabled**. Lombok generates getters,
setters, and constructors at compile time — without annotation processing,
IntelliJ cannot compile any class that uses `@Getter`, `@Setter`,
`@RequiredArgsConstructor`, etc.

---

## Fix — Enable Annotation Processing (do this once)

1. Open **File → Settings** (Windows/Linux) or **IntelliJ IDEA → Preferences** (Mac)
2. Navigate to **Build, Execution, Deployment → Compiler → Annotation Processors**
3. Check ✅ **Enable annotation processing**
4. Click **OK**
5. **Rebuild the project**: **Build → Rebuild Project**

---

## Fix — Install the Lombok Plugin (if not already installed)

1. Open **File → Settings → Plugins**
2. Search for **Lombok**
3. Click **Install** (by Michail Plushnikov)
4. Restart IntelliJ
5. Rebuild the project

---

## Verify It Worked

After enabling annotation processing and rebuilding, the project should compile
without errors. You can verify Lombok is working by checking that IntelliJ
shows getter/setter methods in the Structure panel (View → Tool Windows → Structure)
for any class annotated with `@Getter` or `@Data`.

---

## Running the Application

```bash
# 1. Create the database
mysql -u root -p -e "CREATE DATABASE claiminsight_db;"

# 2. Update src/main/resources/application.properties with your MySQL password

# 3. Run
mvn spring-boot:run

# 4. Open Swagger UI
# http://localhost:8082/swagger-ui.html
```
