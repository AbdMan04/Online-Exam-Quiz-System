package com.quizapp.junitfeatures;

import com.quizapp.model.Role;
import com.quizapp.web.RoleInterceptor;
import com.quizapp.web.SessionUser;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R4: every role lands on its own dashboard and can only open its own pages.
 *
 * <p>JUnit features: {@code @EnumSource} (with and without {@code mode = EXCLUDE}) and
 * {@code @DisplayNameGeneration} (method names with underscores become readable display names).
 */
@Tag("parameterized")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RoleAccessTest {

    private static boolean allowed(Role pageRole, Role userRole) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUser.SESSION_KEY, new SessionUser(1L, "User", userRole));
        request.setSession(session);
        return new RoleInterceptor(pageRole).preHandle(request, new MockHttpServletResponse(), new Object());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void every_role_has_its_own_dashboard(Role role) {
        String expected = "/" + role.name().toLowerCase() + "/dashboard";
        assertEquals(expected, new SessionUser(1L, "User", role).dashboardPath());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void every_role_may_open_its_own_pages(Role role) throws Exception {
        assertTrue(allowed(role, role));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "STUDENT", mode = EnumSource.Mode.EXCLUDE)
    void a_student_cannot_open_the_pages_of_any_other_role(Role otherRole) throws Exception {
        assertFalse(allowed(otherRole, Role.STUDENT));
    }
}
