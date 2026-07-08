package com.blueforge.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_version")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "idea_snapshot", nullable = false, columnDefinition = "TEXT")
    private String ideaSnapshot;

    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectVersionStatus status;

    @OneToMany(mappedBy = "projectVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ClarifyingQuestion> clarifyingQuestions = new ArrayList<>();

    public ProjectVersion(Project project, int versionNumber, String ideaSnapshot, ProjectVersionStatus status) {
        this.project = project;
        this.versionNumber = versionNumber;
        this.ideaSnapshot = ideaSnapshot;
        this.status = status;
    }
}
