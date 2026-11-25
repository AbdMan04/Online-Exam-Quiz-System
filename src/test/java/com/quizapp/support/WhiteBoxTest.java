package com.quizapp.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composed annotation for white-box tests (tables x-3 statement coverage and x-4 branch coverage):
 * plain unit tests where every function is called directly, collaborators are Mockito mocks,
 * tagged "whitebox", and every test case recorded as a report row.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("whitebox")
@ExtendWith(MockitoExtension.class)
@ExtendWith(TestCaseRecorder.class)
public @interface WhiteBoxTest {
}
