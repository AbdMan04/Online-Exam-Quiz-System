package com.quizapp.repository;

import com.quizapp.model.Attempt;
import com.quizapp.model.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    boolean existsByQuizId(Long quizId);

    long countByQuizIdAndStudentId(Long quizId, Long studentId);

    Optional<Attempt> findFirstByQuizIdAndStudentIdAndStatus(Long quizId, Long studentId, AttemptStatus status);

    List<Attempt> findByQuizIdOrderByStartedAtAsc(Long quizId);
}
