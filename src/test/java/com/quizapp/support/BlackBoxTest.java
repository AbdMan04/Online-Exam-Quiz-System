package com.quizapp.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composed annotation for black-box requirement tests (tables x-1 EP and x-2 BVA):
 * the real application on the in-memory H2 database, every test rolled back afterwards,
 * tagged "blackbox", and every test case recorded as a report row.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpringBootTest
@Transactional
@Tag("blackbox")
@ExtendWith(TestCaseRecorder.class)
public @interface BlackBoxTest {
}
