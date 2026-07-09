package com.blueforge.repository;

import com.blueforge.entity.ProjectVersion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectVersionRepository extends JpaRepository<ProjectVersion, Long> {

    @Query(
            """
            SELECT v FROM ProjectVersion v
            LEFT JOIN FETCH v.clarifyingQuestions cq
            LEFT JOIN FETCH cq.answer
            WHERE v.project.id = :projectId AND v.versionNumber = :versionNumber
            """)
    Optional<ProjectVersion> findByProjectIdAndVersionNumber(
            @Param("projectId") Long projectId, @Param("versionNumber") int versionNumber);
}
