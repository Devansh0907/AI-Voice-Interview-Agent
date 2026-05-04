package com.interview.repository;

import com.interview.model.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByUserIdOrderByStartedAtDesc(Long userId);
    List<Interview> findByUserIdAndStatus(Long userId, String status);
}
