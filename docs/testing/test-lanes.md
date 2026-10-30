# Test lanes

The 71 classes and 245 Jupiter tests under `src/integrationTest/java` are
selected into three independent Gradle lanes. The class and method patterns
below are the source-of-truth inventory; a test is deliberately not inferred
from its filename because four classes contain more than one kind of test.

| Lane | Tests | Services | Gradle task |
| --- | ---: | --- | --- |
| Fast | 161 | None | `./gradlew fastTest` |
| Derby | 78 | None; in-memory or temporary Derby | `./gradlew derbyTest` |
| External database | 6 | MySQL and PostgreSQL | `./gradlew externalDatabaseTest` |

The fast lane contains pure service, formatting, layout, and model tests. The
Derby lane contains persistence, migration, repository, backup, and local UI
fixture tests. External tests are limited to the MySQL/PostgreSQL migration,
terminal configuration, and signed SQL-import checks.

`build.gradle.kts` keeps the explicit class and method selection next to the
task definitions. This makes adding a test require an intentional lane
decision rather than silently coupling a fast test to Docker. Every lane has
separate JUnit XML and Gradle HTML reports under `build/test-results/<task>`
and `build/reports/tests/<task>`.

`./gradlew verifyTestLaneCoverage` scans the current `@Test` methods and fails
if a method is missing from, or assigned to more than one, lane. `ciCheck` and
each CI lane run this guard before executing tests.

Tests receive `pos.projectDir` and `pos.test.outputDir` from Gradle. Repository
fixtures and generated screenshots therefore do not depend on the caller's
working directory. External tasks also have a ten-minute task timeout and
report the lane, database engine, and connection URL when a database cannot be
reached.
