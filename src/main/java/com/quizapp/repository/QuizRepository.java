package com.quizapp.repository;

import com.quizapp.model.Quiz;
import com.quizapp.model.QuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByInstructorIdOrderByIdDesc(Long instructorId);

    List<Quiz> findByStatusOrderByIdDesc(QuizStatus status);
}
