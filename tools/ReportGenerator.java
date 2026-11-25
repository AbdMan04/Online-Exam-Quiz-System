import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Builds the course report (target/report/report.html) from the project's real data:
 * <ul>
 *   <li>docs/requirements.md                  - the 20 Gherkin requirements</li>
 *   <li>target/test-tables/test-cases.csv     - every test case row with actual output and Pass/Fail</li>
 *   <li>target/site/jacoco/jacoco.csv         - code coverage</li>
 *   <li>target/surefire-reports/*.txt         - test counts</li>
 *   <li>docs/defects.md, docs/junit-features.md, docs/report-info.properties</li>
 *   <li>src/test/java/com/quizapp/whitebox/*  - the "functions under test" of each requirement</li>
 * </ul>
 * Run from the project root after "mvnw clean test":  java tools/ReportGenerator.java
 * (tools/build-report.cmd does all steps and prints the PDF.)
 */
public class ReportGenerator {

    private static final Path OUTPUT = Path.of("target", "report", "report.html");
    private static final Path SCREENSHOTS = Path.of("docs", "report-screenshots");
    private static final String[] TABLE_CODES = {"EP", "BVA", "SC", "BC"};
    private static final String[] TABLE_TITLES = {
            "Equivalent partition test cases", "2-BVA test cases",
            "Statement coverage test cases", "Branch coverage test cases"};

    record Requirement(int number, String title, String gherkin) { }

    record Row(String requirement, String table, String id, String condition, String input,
               String actual, String expected, String result) { }

    record CoverageLine(String pkg, String name, long linesCovered, long linesTotal, long branchesCovered,
                        long branchesTotal) {
        String linePercent() {
            return percent(linesCovered, linesTotal);
        }

        String branchPercent() {
            return branchesTotal == 0 ? "n/a" : percent(branchesCovered, branchesTotal);
        }
    }

    record TestCounts(long run, long failures, long errors, long skipped, long blackBox, long whiteBox) { }

    public static void main(String[] args) throws IOException {
        Properties info = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of("docs", "report-info.properties"), StandardCharsets.UTF_8)) {
            info.load(reader);
        }
        List<Requirement> requirements = parseRequirements(read("docs/requirements.md"));
        List<Row> rows = parseRows(read("target/test-tables/test-cases.csv"));
        List<CoverageLine> classes = parseCoverage(read("target/site/jacoco/jacoco.csv"));
        TestCounts counts = countTests(Path.of("target", "surefire-reports"));

        StringBuilder html = new StringBuilder();
        html.append(head(info.getProperty("title")));
        html.append(cover(info));
        html.append(contents(requirements));
        html.append(overview(requirements, rows, counts));
        html.append(coverageSection(classes));
        html.append(defects(read("docs/defects.md")));
        for (Requirement requirement : requirements) {
            html.append(requirementSection(requirement, rows));
        }
        html.append(appendix(read("docs/junit-features.md")));
        html.append("</body></html>\n");

        Files.createDirectories(OUTPUT.getParent());
        Files.writeString(OUTPUT, html.toString(), StandardCharsets.UTF_8);
        System.out.printf("Report written to %s (%d requirements, %d test case rows, %d tests)%n",
                OUTPUT, requirements.size(), rows.size(), counts.run());
    }

    // ------------------------------------------------------------------ parsing

    private static String read(String path) throws IOException {
        Path file = Path.of(path);
        if (!Files.exists(file)) {
            throw new IllegalStateException("Missing " + path + " - run 'mvnw clean test' first.");
        }
        return Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    static List<Requirement> parseRequirements(String markdown) {
        Matcher m = Pattern.compile("\\*\\*R(\\d+): ([^*]+)\\*\\*\\s*```gherkin\\s*\\n(.*?)```", Pattern.DOTALL)
                .matcher(markdown);
        List<Requirement> result = new ArrayList<>();
        while (m.find()) {
            result.add(new Requirement(Integer.parseInt(m.group(1)), m.group(2).trim(), m.group(3).strip()));
        }
        result.sort(Comparator.comparingInt(Requirement::number));
        return result;
    }

    static List<Row> parseRows(String csv) {
        List<Row> rows = new ArrayList<>();
        String[] lines = csv.split("\n");
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            List<String> c = splitCsvLine(lines[i]);
            rows.add(new Row(c.get(0), c.get(1), c.get(2), c.get(3), c.get(4), c.get(5), c.get(6), c.get(7)));
        }
        return rows;
    }

    /** Splits one CSV line; fields may be quoted, with "" for a quote inside a field. */
    static List<String> splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (ch == '"') {
                    quoted = false;
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(ch);
            }
        }
        fields.add(field.toString());
        return fields;
    }

    static List<CoverageLine> parseCoverage(String csv) {
        List<CoverageLine> result = new ArrayList<>();
        String[] lines = csv.split("\n");
        for (int i = 1; i < lines.length; i++) {
            String[] c = lines[i].split(",");
            if (c.length < 9) {
                continue;
            }
            long branchMissed = Long.parseLong(c[5]);
            long branchCovered = Long.parseLong(c[6]);
            long lineMissed = Long.parseLong(c[7]);
            long lineCovered = Long.parseLong(c[8]);
            result.add(new CoverageLine(c[1], c[1] + "." + c[2], lineCovered, lineMissed + lineCovered,
                    branchCovered, branchMissed + branchCovered));
        }
        result.sort(Comparator.comparing(CoverageLine::name));
        return result;
    }

    static TestCounts countTests(Path reports) throws IOException {
        Pattern p = Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)");
        long run = 0, failures = 0, errors = 0, skipped = 0, blackBox = 0, whiteBox = 0;
        if (!Files.isDirectory(reports)) {
            return new TestCounts(0, 0, 0, 0, 0, 0);
        }
        try (Stream<Path> files = Files.list(reports)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".txt")).toList()) {
                Matcher m = p.matcher(Files.readString(file));
                if (m.find()) {
                    long n = Long.parseLong(m.group(1));
                    run += n;
                    failures += Long.parseLong(m.group(2));
                    errors += Long.parseLong(m.group(3));
                    skipped += Long.parseLong(m.group(4));
                    String name = file.getFileName().toString();
                    blackBox += name.startsWith("com.quizapp.blackbox.") ? n : 0;
                    whiteBox += name.startsWith("com.quizapp.whitebox.") ? n : 0;
                }
            }
        }
        return new TestCounts(run, failures, errors, skipped, blackBox, whiteBox);
    }

    /** The class Javadoc of the requirement's white-box test: it names the functions under test. */
    static String functionsUnderTest(int number) throws IOException {
        Path dir = Path.of("src", "test", "java", "com", "quizapp", "whitebox");
        String prefix = String.format("R%02d", number);
        try (Stream<Path> files = Files.list(dir)) {
            Path file = files.filter(f -> f.getFileName().toString().startsWith(prefix)).findFirst().orElse(null);
            if (file == null) {
                return "";
            }
            String source = Files.readString(file, StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("/\\*\\*(.*?)\\*/\\s*@", Pattern.DOTALL).matcher(source);
            if (!m.find()) {
                return "";
            }
            String text = m.group(1).replaceAll("(?m)^\\s*\\*\\s?", " ")
                    .replaceAll("\\{@code ([^}]*)}", "`$1`")
                    .replaceAll("<p>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            return text.replaceFirst("^R\\d+ white-box tests\\.\\s*", "");
        }
    }

    // ------------------------------------------------------------------ HTML sections

    private static String head(String title) {
        return """
                <!DOCTYPE html>
                <html lang="en"><head><meta charset="UTF-8"><title>%s - Test Report</title>
                <style>
                  @page { size: A4; margin: 15mm 13mm; }
                  body { font-family: "Segoe UI", Arial, sans-serif; font-size: 10pt; color: #1f2430; line-height: 1.4; }
                  h1 { font-size: 17pt; color: #1d3a8a; margin: 0 0 8px; }
                  h2 { font-size: 13pt; color: #1d3a8a; border-bottom: 2px solid #2f5bd3; padding-bottom: 3px; margin-top: 18px; }
                  h3 { font-size: 11pt; margin: 14px 0 4px; }
                  .page { page-break-before: always; }
                  .cover { text-align: center; padding-top: 70mm; }
                  .cover h1 { font-size: 26pt; margin-bottom: 4px; }
                  .cover .subtitle { font-size: 14pt; color: #4a5368; }
                  .cover table { margin: 28px auto 0; width: 70%%; font-size: 10.5pt; }
                  .cover td { text-align: left; }
                  table { width: 100%%; border-collapse: collapse; margin: 4px 0 12px; font-size: 8.3pt; table-layout: fixed; }
                  th, td { border: 1px solid #b9c0cf; padding: 3px 5px; vertical-align: top; overflow-wrap: anywhere; }
                  th { background: #e8edf7; text-align: left; }
                  thead { display: table-header-group; }
                  tr { break-inside: avoid; }
                  .caption { font-weight: 600; font-style: italic; margin: 10px 0 2px; }
                  .gherkin { background: #f5f7fb; border-left: 3px solid #2f5bd3; padding: 7px 10px; margin: 6px 0 10px;
                             white-space: pre-wrap; font-family: Consolas, monospace; font-size: 9pt; }
                  .kw { color: #1d3a8a; font-weight: 700; }
                  .scope { font-size: 8.8pt; color: #4a5368; margin: 2px 0 6px; }
                  .pass { color: #1d6b3d; font-weight: 700; }
                  .fail { color: #a12626; font-weight: 700; }
                  .num { text-align: right; }
                  .muted { color: #667085; }
                  code { font-family: Consolas, monospace; font-size: 0.95em; background: #f1f3f8; padding: 0 2px; }
                  .stats { display: flex; gap: 10px; margin: 8px 0 12px; }
                  .stat { flex: 1; border: 1px solid #b9c0cf; border-radius: 6px; padding: 6px 8px; text-align: center; }
                  .stat b { display: block; font-size: 15pt; color: #1d3a8a; }
                  figure { margin: 10px 0; break-inside: avoid; } figure img { max-width: 100%%; border: 1px solid #b9c0cf; }
                  figcaption { font-style: italic; font-size: 9pt; }
                  ul.toc { columns: 2; font-size: 9.5pt; }
                </style></head><body>
                """.formatted(escape(title));
    }

    private static String cover(Properties info) {
        StringBuilder team = new StringBuilder();
        for (int i = 1; i <= 4; i++) {
            String member = info.getProperty("team." + i, "").trim();
            if (!member.isEmpty()) {
                team.append("<tr><td>").append(escape(member)).append("</td></tr>");
            }
        }
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH));
        return """
                <div class="cover">
                  <div class="subtitle">%s &middot; %s</div>
                  <h1>%s</h1>
                  <div class="subtitle">Requirements, test cases and coverage report</div>
                  <table><tr><th>Team members</th></tr>%s</table>
                  <table><tr><th>Source code and tests</th></tr><tr><td>%s</td></tr></table>
                  <p class="muted" style="margin-top:30px">Generated from the test results on %s</p>
                </div>
                """.formatted(escape(info.getProperty("course")), escape(info.getProperty("semester")),
                escape(info.getProperty("title")), team, escape(info.getProperty("github")), date);
    }

    private static String contents(List<Requirement> requirements) {
        StringBuilder html = new StringBuilder("<div class=\"page\"><h1>Contents</h1><ol>"
                + "<li><a href=\"#overview\">Overview</a></li><li><a href=\"#coverage\">Code coverage</a></li>"
                + "<li><a href=\"#defects\">Defects found by testing</a></li>"
                + "<li>Requirements and test cases<ul class=\"toc\">");
        for (Requirement r : requirements) {
            html.append("<li><a href=\"#req-").append(r.number()).append("\">Requirement #").append(r.number())
                    .append(": ").append(escape(r.title())).append("</a></li>");
        }
        return html.append("</ul></li><li><a href=\"#appendix\">Appendix: JUnit features used</a></li></ol></div>")
                .toString();
    }

    private static String overview(List<Requirement> requirements, List<Row> rows, TestCounts counts) {
        long passed = rows.stream().filter(r -> r.result().equals("Pass")).count();
        StringBuilder html = new StringBuilder("""
                <div class="page" id="overview"><h1>1. Overview</h1>
                <p>The <b>Online Exam/Quiz System</b> is a web application (Java 17, Spring Boot, Thymeleaf, MySQL) in which
                instructors create, publish and grade timed multiple-choice quizzes and students take them. Its behaviour is
                specified by 20 requirements written in Gherkin (Given / When / Then), shown in section 4.</p>
                <h3>How the test cases were produced</h3>
                <ul>
                  <li><b>Black-box (tables n-1 and n-2):</b> equivalence partitions and 2-value boundary analysis
                      (min&minus;1, min, max, max+1), executed through the application's public service layer on an
                      in-memory H2 database. Timer rules are tested at exact seconds with a test-controlled clock.</li>
                  <li><b>White-box (tables n-3 and n-4):</b> JUnit unit tests that call each function directly (Mockito for
                      the database) so that every statement runs and every decision &mdash; including each part of combined
                      conditions such as <code>a || b</code> &mdash; is both true and false.</li>
                  <li>Every table row is a JUnit test case. A custom JUnit extension records the <b>actual output produced by
                      the running code</b> and the Pass/Fail result, and this report is generated from those records.</li>
                </ul>
                """);
        html.append("<div class=\"stats\">")
                .append(stat(counts.run(), "JUnit tests run"))
                .append(stat(counts.failures() + counts.errors(), "failures"))
                .append(stat(rows.size(), "table rows (test cases)"))
                .append(stat(passed, "rows passed"))
                .append(stat(requirements.size() * 4L, "tables"))
                .append("</div>");
        html.append("<p class=\"muted\">").append(counts.skipped())
                .append(" test is intentionally disabled (a 30-minute real-time timer test, replaced by clock-controlled tests).</p>");

        html.append("<h3>Test cases per requirement</h3><table><thead><tr><th style=\"width:42%\">Requirement</th>"
                + "<th>EP</th><th>2-BVA</th><th>Statement</th><th>Branch</th><th>Total</th><th>Passed</th></tr></thead><tbody>");
        for (Requirement r : requirements) {
            String key = String.format("R%02d", r.number());
            List<Row> own = rows.stream().filter(row -> row.requirement().equals(key)).toList();
            html.append("<tr><td>#").append(r.number()).append(" ").append(escape(r.title())).append("</td>");
            for (String code : TABLE_CODES) {
                html.append("<td class=\"num\">").append(own.stream().filter(row -> row.table().equals(code)).count()).append("</td>");
            }
            long ownPassed = own.stream().filter(row -> row.result().equals("Pass")).count();
            html.append("<td class=\"num\"><b>").append(own.size()).append("</b></td><td class=\"num\">")
                    .append(ownPassed).append("</td></tr>");
        }
        return html.append("</tbody></table></div>").toString();
    }

    private static String stat(long value, String label) {
        return "<div class=\"stat\"><b>" + value + "</b>" + escape(label) + "</div>";
    }

    private static String coverageSection(List<CoverageLine> classes) throws IOException {
        long lc = classes.stream().mapToLong(CoverageLine::linesCovered).sum();
        long lt = classes.stream().mapToLong(CoverageLine::linesTotal).sum();
        long bc = classes.stream().mapToLong(CoverageLine::branchesCovered).sum();
        long bt = classes.stream().mapToLong(CoverageLine::branchesTotal).sum();

        Map<String, long[]> packages = new TreeMap<>();
        for (CoverageLine c : classes) {
            String pkg = c.pkg();
            long[] sums = packages.computeIfAbsent(pkg, k -> new long[4]);
            sums[0] += c.linesCovered();
            sums[1] += c.linesTotal();
            sums[2] += c.branchesCovered();
            sums[3] += c.branchesTotal();
        }

        StringBuilder html = new StringBuilder("<div class=\"page\" id=\"coverage\"><h1>2. Code coverage</h1>");
        html.append("<p>Measured with JaCoCo while running the complete JUnit test suite (statement coverage = line "
                + "coverage, branch coverage = every outcome of every decision).</p>");
        html.append("<div class=\"stats\">")
                .append("<div class=\"stat\"><b>").append(percent(lc, lt)).append("</b>statement (line) coverage<br>")
                .append(lc).append(" / ").append(lt).append(" lines</div>")
                .append("<div class=\"stat\"><b>").append(percent(bc, bt)).append("</b>branch coverage<br>")
                .append(bc).append(" / ").append(bt).append(" branches</div></div>");
        html.append("<p class=\"muted\">The only lines not executed by tests are the two lines of "
                + "<code>QuizSystemApplication.main()</code>, which starts the web server; tests start the application "
                + "through Spring's test support instead.</p>");

        html.append("<h3>Coverage per package</h3><table><thead><tr><th style=\"width:40%\">Package</th><th>Lines</th>"
                + "<th>Line coverage</th><th>Branches</th><th>Branch coverage</th></tr></thead><tbody>");
        packages.forEach((pkg, s) -> html.append(coverageRow(pkg, s[0], s[1], s[2], s[3])));
        html.append("</tbody></table>");

        html.append("<h3>Coverage per class</h3><table><thead><tr><th style=\"width:40%\">Class</th><th>Lines</th>"
                + "<th>Line coverage</th><th>Branches</th><th>Branch coverage</th></tr></thead><tbody>");
        for (CoverageLine c : classes) {
            html.append(coverageRow(c.name().replace("com.quizapp.", ""), c.linesCovered(), c.linesTotal(),
                    c.branchesCovered(), c.branchesTotal()));
        }
        html.append("</tbody></table>");
        html.append(screenshots());
        return html.append("</div>").toString();
    }

    private static String coverageRow(String name, long lc, long lt, long bc, long bt) {
        return "<tr><td>" + escape(name) + "</td><td class=\"num\">" + lc + " / " + lt + "</td><td class=\"num\">"
                + percent(lc, lt) + "</td><td class=\"num\">" + (bt == 0 ? "-" : bc + " / " + bt)
                + "</td><td class=\"num\">" + (bt == 0 ? "n/a" : percent(bc, bt)) + "</td></tr>";
    }

    /** IntelliJ / JaCoCo screenshots placed in docs/report-screenshots are embedded here. */
    private static String screenshots() throws IOException {
        if (!Files.isDirectory(SCREENSHOTS)) {
            return "";
        }
        List<Path> images;
        try (Stream<Path> files = Files.list(SCREENSHOTS)) {
            images = files.filter(f -> f.toString().toLowerCase().matches(".*\\.(png|jpg|jpeg)$")).sorted().toList();
        }
        StringBuilder html = new StringBuilder(images.isEmpty() ? "" : "<h3>Coverage screenshots (IntelliJ IDEA)</h3>");
        for (Path image : images) {
            String caption = image.getFileName().toString().replaceAll("\\.[^.]+$", "").replaceAll("^\\d+[-_ ]*", "")
                    .replace('-', ' ').replace('_', ' ');
            html.append("<figure><img src=\"").append(image.toAbsolutePath().toUri()).append("\"><figcaption>")
                    .append(escape(caption)).append("</figcaption></figure>");
        }
        return html.toString();
    }

    private static String defects(String markdown) {
        StringBuilder html = new StringBuilder("<div class=\"page\" id=\"defects\"><h1>3. Defects found by testing</h1>"
                + "<p>Boundary value analysis found four defects. Each was confirmed by a failing test case, fixed, "
                + "added to the specification, and the test case now passes.</p><table><thead><tr>"
                + "<th style=\"width:6%\">ID</th><th style=\"width:17%\">Found by</th><th style=\"width:9%\">Req.</th>"
                + "<th>Defect</th><th>Fix</th></tr></thead><tbody>");
        for (String line : markdown.split("\n")) {
            if (line.startsWith("| D-")) {
                List<String> cells = markdownCells(line);
                html.append("<tr>");
                cells.forEach(cell -> html.append("<td>").append(inline(cell)).append("</td>"));
                html.append("</tr>");
            }
        }
        return html.append("</tbody></table></div>").toString();
    }

    private static String requirementSection(Requirement requirement, List<Row> rows) throws IOException {
        String key = String.format("R%02d", requirement.number());
        int n = requirement.number();
        StringBuilder html = new StringBuilder("<div class=\"page\" id=\"req-").append(n).append("\"><h1>Requirement #").append(n).append(": ")
                .append(escape(requirement.title())).append("</h1>");
        html.append("<div class=\"gherkin\">").append(gherkin(requirement.gherkin())).append("</div>");

        for (int t = 0; t < TABLE_CODES.length; t++) {
            String code = TABLE_CODES[t];
            boolean whiteBox = t >= 2;
            List<Row> tableRows = rows.stream()
                    .filter(row -> row.requirement().equals(key) && row.table().equals(code))
                    .sorted(Comparator.comparing(Row::id)).toList();
            if (t == 2) {
                String scope = functionsUnderTest(n);
                if (!scope.isEmpty()) {
                    html.append("<p class=\"scope\"><b>White-box scope:</b> ").append(inline(scope)).append("</p>");
                }
            }
            html.append("<div class=\"caption\">Table ").append(n).append('-').append(t + 1).append(": ")
                    .append(TABLE_TITLES[t]).append("</div>");
            html.append("<table><thead><tr><th style=\"width:12%\">Test Case ID</th><th style=\"width:")
                    .append(whiteBox ? "24" : "26").append("%\">Input condition</th><th style=\"width:")
                    .append(whiteBox ? "22" : "24").append("%\">Input</th><th>Actual output</th><th>Expected output</th>")
                    .append(whiteBox ? "<th style=\"width:7%\">Pass/Fail</th>" : "").append("</tr></thead><tbody>");
            for (Row row : tableRows) {
                html.append("<tr><td>").append(escape(row.id())).append("</td><td>").append(escape(row.condition()))
                        .append("</td><td>").append(escape(row.input())).append("</td><td>").append(escape(row.actual()))
                        .append("</td><td>").append(escape(row.expected())).append("</td>");
                if (whiteBox) {
                    String css = row.result().equals("Pass") ? "pass" : "fail";
                    html.append("<td class=\"").append(css).append("\">").append(escape(row.result())).append("</td>");
                }
                html.append("</tr>");
            }
            html.append("</tbody></table>");
        }
        return html.append("</div>").toString();
    }

    private static String appendix(String markdown) {
        StringBuilder html = new StringBuilder("<div class=\"page\" id=\"appendix\"><h1>Appendix: JUnit features used</h1>");
        List<String> tableLines = new ArrayList<>();
        for (String line : (markdown + "\n").split("\n")) {
            if (line.startsWith("|")) {
                tableLines.add(line);
                continue;
            }
            if (!tableLines.isEmpty()) {
                html.append(markdownTable(tableLines));
                tableLines.clear();
            }
            if (line.startsWith("## ")) {
                html.append("<h3>").append(inline(line.substring(3))).append("</h3>");
            } else if (!line.isBlank() && !line.startsWith("# ")) {
                html.append("<p>").append(inline(line)).append("</p>");
            }
        }
        return html.append("</div>").toString();
    }

    private static String markdownTable(List<String> lines) {
        StringBuilder html = new StringBuilder("<table><thead><tr>");
        markdownCells(lines.get(0)).forEach(cell -> html.append("<th>").append(inline(cell)).append("</th>"));
        html.append("</tr></thead><tbody>");
        for (String line : lines.subList(2, lines.size())) {
            html.append("<tr>");
            markdownCells(line).forEach(cell -> html.append("<td>").append(inline(cell)).append("</td>"));
            html.append("</tr>");
        }
        return html.append("</tbody></table>").toString();
    }

    // ------------------------------------------------------------------ text helpers

    private static List<String> markdownCells(String line) {
        String trimmed = line.trim();
        String inner = trimmed.substring(1, trimmed.endsWith("|") ? trimmed.length() - 1 : trimmed.length());
        List<String> cells = new ArrayList<>();
        for (String cell : inner.split("(?<!\\\\)\\|")) {
            cells.add(cell.trim().replace("\\|", "|"));
        }
        return cells;
    }

    private static String gherkin(String text) {
        return escape(text).replaceAll("(?m)^(\\s*)(Given|When|Then|And|But)\\b", "$1<span class=\"kw\">$2</span>");
    }

    /** Escapes HTML, then renders `code` and **bold**. */
    private static String inline(String text) {
        return escape(text).replaceAll("`([^`]+)`", "<code>$1</code>").replaceAll("\\*\\*([^*]+)\\*\\*", "<b>$1</b>");
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    static String percent(long part, long total) {
        return total == 0 ? "100.0%" : String.format(Locale.ROOT, "%.1f%%", 100.0 * part / total);
    }

    private ReportGenerator() {
    }
}
