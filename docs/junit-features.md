# JUnit features used in this project

Every feature below is used in a test that checks real behaviour of the Online Quiz System.
Paths are under `src/test/java/com/quizapp/`.

## Writing and organizing tests

| Feature | Where | What it is used for |
|---|---|---|
| `@Test`, `@DisplayName` | everywhere | readable test names in IntelliJ and the reports |
| `@Nested` | `blackbox/*`, `whitebox/*` | one inner class per report table (x-1 … x-4) |
| `@DisplayNameGeneration(ReplaceUnderscores)` | `junitfeatures/RoleAccessTest` | `every_role_has_its_own_dashboard` → "every role has its own dashboard" |
| `@Tag` | `blackbox`, `whitebox`, `integration`, `performance`, `mysql`, … | run groups: `mvnw test -Dgroups=whitebox` |
| `@Disabled` | `junitfeatures/PerformanceTest` | a documented real-time (30 min) timer test, replaced by clock-controlled tests |
| `@TestMethodOrder` + `@Order` | `junitfeatures/AccountLockLifecycleTest` | wrong → wrong → wrong → correct password, in that order |
| `@TestInstance(PER_CLASS)` | `junitfeatures/AccountLockLifecycleTest` | one account shared by all test methods |
| `@Suite`, `@SelectPackages`, `@IncludeTags`, `@SuiteDisplayName` | `suites/BlackBoxSuite`, `WhiteBoxSuite`, `ReportTablesSuite` | one click runs all black-box, all white-box, or exactly the 80 report tables |

## Lifecycle

| Feature | Where |
|---|---|
| `@BeforeEach` / `@AfterEach` | `whitebox/*Support`, `blackbox/QuizBlackBoxSupport`, `service/AuthFlowIntegrationTest` |
| `@BeforeAll` / `@AfterAll` (non-static with PER_CLASS) | `junitfeatures/AccountLockLifecycleTest` (the `@AfterAll` checks that every failure was saved) |

## Assertions and assumptions

| Feature | Where |
|---|---|
| `assertEquals`, `assertTrue/False`, `assertSame`, `assertNotEquals` | everywhere |
| `assertThrows`, `assertDoesNotThrow`, `assertInstanceOf` | `service/*Test`, `support/TestTableWriterTest` |
| `assertAll` (grouped assertions) | `service/AuthServiceTest`, `junitfeatures/MySqlSchemaTest` |
| `assertIterableEquals` | `support/TestTableWriterTest` (sort order of report rows) |
| `assertLinesMatch` (regex lines and `>> n >>` fast-forward) | `support/TestTableWriterTest` (generated Markdown) |
| `assertTimeout`, `assertTimeoutPreemptively`, `@Timeout` | `junitfeatures/PerformanceTest` |
| `assumeTrue`, `assumingThat` | `junitfeatures/MySqlSchemaTest` |
| `@EnabledIf` (conditional execution) | `junitfeatures/MySqlSchemaTest` (runs only where MySQL is listening) |

## Repeated, dynamic and parameterized tests

| Feature | Where |
|---|---|
| `@RepeatedTest` + `RepetitionInfo` | `junitfeatures/AccountLockLifecycleTest` (wrong password 1 of 2, 2 of 2) |
| `@TestFactory`, `DynamicTest`, `DynamicContainer` | `junitfeatures/LetterGradeExhaustiveTest` (202 generated tests, grouped by grade) |
| `@ParameterizedTest` + `@CsvSource` (text blocks, `nullValues`, `emptyValue`, `delimiter`) | all 80 report tables (`blackbox/*`, `whitebox/*`) |
| `@ValueSource`, `@NullAndEmptySource` | `validation/PasswordValidatorTest` |
| `@EnumSource` (incl. `mode = EXCLUDE`) | `junitfeatures/RoleAccessTest` |
| `@MethodSource` + `Named` | `junitfeatures/QuizInputSourcesTest` |
| `@CsvFileSource` | `junitfeatures/ScoreRoundingFileTest` (cases in `src/test/resources/test-data/score-rounding.csv`) |
| `@ArgumentsSource` (custom `ArgumentsProvider`) | `junitfeatures/QuizInputSourcesTest` (all option/correct-answer combinations) |
| `@AggregateWith` (custom `ArgumentsAggregator`) | `junitfeatures/QuizInputSourcesTest` (CSV columns → `QuizInput`) |
| `@ConvertWith` (custom `ArgumentConverter`) | `junitfeatures/QuizInputSourcesTest` ("LEN:101" → 101-character text) |
| `@ParameterizedClass` + `@Parameter` | `junitfeatures/AttemptLimitPerSettingTest` (whole class runs for max attempts 1, 3, 5) |

## Extensions and injected parameters

| Feature | Where |
|---|---|
| Custom extension with `@ExtendWith` (`ParameterResolver`, `AfterTestExecutionCallback`, `ExtensionContext.Store`, auto-closed store values) | `support/TestCaseRecorder` (turns test runs into the report tables) |
| `@RegisterExtension` (programmatic, configured extension; `Before/AfterTestExecutionCallback`, report entries) | `junitfeatures/TimingExtension` in `PerformanceTest` |
| Composed annotations | `support/BlackBoxTest`, `support/WhiteBoxTest` |
| `@TempDir` | `support/TestTableWriterTest` |
| `@AutoClose` | `junitfeatures/MySqlSchemaTest` (JDBC connection) |
| `TestInfo`, `TestReporter` injection | `junitfeatures/AccountLockLifecycleTest` |
| Third-party extensions | `MockitoExtension` (`@Mock`), Spring's `SpringExtension` (via `@SpringBootTest`) |

## Running

| Goal | Command / action |
|---|---|
| Everything + coverage + report tables | `mvnw clean test` |
| Only one group | `mvnw test -Dgroups=blackbox` (or `whitebox`, `performance`, `mysql`, …) |
| In IntelliJ | right-click a suite in `suites/` → Run (or Run with Coverage) |
