package com.blueforge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clarifying_question")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClarifyingQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_version_id", nullable = false)
    private ProjectVersion projectVersion;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public ClarifyingQuestion(ProjectVersion projectVersion, String questionText, int orderIndex) {
        this.projectVersion = projectVersion;
        this.questionText = questionText;
        this.orderIndex = orderIndex;
    }
}
