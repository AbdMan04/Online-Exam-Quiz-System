# Online Exam/Quiz System (SE430 Software Testing Project)

A web-based quiz system (Spring Boot + Thymeleaf + MySQL) with 20 BDD requirements, each tested with
black-box techniques (equivalence partitions and BVA) and white-box techniques (statement and branch coverage) in JUnit 5.

- Requirements, rules and exact messages: [docs/requirements.md](docs/requirements.md)

## Prerequisites
- JDK 17+
- MySQL 8 running on `localhost:3306` (only needed to **run** the app; tests use in-memory H2)
- No Maven install is needed: use the wrapper (`mvnw` / `mvnw.cmd`)

## Database credentials
The app uses the `quizdb` database (it is created automatically). The default user is `root` with an empty password. To override it, either:
- set the environment variables `DB_USERNAME` and `DB_PASSWORD`, or
- create `src/main/resources/application-local.properties` (git-ignored):
  ```properties
  spring.datasource.username=root
  spring.datasource.password=yourPassword
  ```

## Commands
| Task | Command |
|---|---|
| Run all tests + coverage report | `mvnw clean test` → open `target/site/jacoco/index.html` |
| Run only black-box tests | `mvnw test -Dgroups=blackbox` |
| Run only white-box tests | `mvnw test -Dgroups=whitebox` |
| Start the app | `mvnw spring-boot:run` → http://localhost:8080 |

In IntelliJ: right-click `src/test/java` → **Run 'All Tests' with Coverage** for the coverage screenshots.

## Project layout
```
com.quizapp
 ├─ model        JPA entities (User, Quiz, Question, Attempt, ...)
 ├─ repository   Spring Data JPA repositories
 ├─ validation   ValidationRules + pure validators (no Spring - easy to unit test)
 ├─ service      business logic, one service per feature area
 ├─ exception    BusinessException + Messages (exact user-facing strings)
 └─ web          controllers + Thymeleaf templates
```
Tests mirror this layout. Each test class uses `@Nested` groups named after the report tables
(x-1 EP, x-2 BVA, x-3/x-4 statement/branch) and is tagged `blackbox` or `whitebox`.
