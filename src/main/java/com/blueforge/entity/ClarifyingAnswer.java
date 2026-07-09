package com.blueforge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "clarifying_answer")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClarifyingAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clarifying_question_id", nullable = false, unique = true)
    private ClarifyingQuestion clarifyingQuestion;

    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @CreationTimestamp
    @Column(name = "answered_at", nullable = false, updatable = false)
    private Instant answeredAt;

    public ClarifyingAnswer(ClarifyingQuestion clarifyingQuestion, String answerText) {
        this.clarifyingQuestion = clarifyingQuestion;
        this.answerText = answerText;
    }
}
