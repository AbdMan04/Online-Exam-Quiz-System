package com.quizapp.junitfeatures;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assumptions.assumingThat;

/**
 * Checks the REAL MySQL database used by the running application (read-only queries).
 * All other tests use the in-memory H2 database, so this is the only check of the production schema.
 * It runs only on machines where MySQL is listening and local credentials exist; elsewhere it is skipped.
 *
 * <p>JUnit features: {@code @EnabledIf} (conditional execution), {@code assumeTrue} / {@code assumingThat}
 * (assumptions) and {@code @AutoClose} (the JDBC connection is closed automatically after each test).
 */
@Tag("mysql")
@EnabledIf("mysqlIsReachable")
@DisplayName("Real MySQL database (only where MySQL runs locally)")
class MySqlSchemaTest {

    private static final String URL = "jdbc:mysql://localhost:3306/quizdb";
    private static final List<String> TABLES =
            List.of("users", "quizzes", "questions", "answer_options", "attempts", "attempt_answers");

    @AutoClose
    private Connection connection;

    static boolean mysqlIsReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 3306), 300);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeEach
    void connectWithLocalCredentials() throws IOException, SQLException {
        Properties local = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application-local.properties")) {
            assumeTrue(in != null, "no application-local.properties with MySQL credentials on this machine");
            local.load(in);
        }
        connection = DriverManager.getConnection(URL,
                local.getProperty("spring.datasource.username", "root"),
                local.getProperty("spring.datasource.password", ""));
    }

    @Test
    @DisplayName("all six application tables exist in quizdb")
    void allTablesExist() throws SQLException {
        Set<String> existing = new HashSet<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'quizdb'");
             ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                existing.add(rows.getString(1).toLowerCase());
            }
        }
        assertAll(TABLES.stream().map(table -> () -> assertTrue(existing.contains(table), "missing table " + table)));
    }

    @Test
    @DisplayName("once the app has started, the instructor account exists with role INSTRUCTOR")
    void seededInstructor() throws SQLException {
        String role = null;
        try (PreparedStatement query = connection.prepareStatement("SELECT role FROM users WHERE email = ?")) {
            query.setString(1, "instructor@uni.edu");
            try (ResultSet rows = query.executeQuery()) {
                if (rows.next()) {
                    role = rows.getString(1);
                }
            }
        }
        String found = role;
        assumingThat(found != null, () -> assertEquals("INSTRUCTOR", found));
    }
}
