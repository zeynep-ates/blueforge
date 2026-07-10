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
@Table(name = "architecture_recommendation")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchitectureRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_version_id", nullable = false)
    private ProjectVersion projectVersion;

    @Column(nullable = false)
    private String component;

    @Column(nullable = false)
    private String recommendation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reasoning;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String tradeoffs;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public ArchitectureRecommendation(
            ProjectVersion projectVersion,
            String component,
            String recommendation,
            String reasoning,
            String tradeoffs,
            int orderIndex) {
        this.projectVersion = projectVersion;
        this.component = component;
        this.recommendation = recommendation;
        this.reasoning = reasoning;
        this.tradeoffs = tradeoffs;
        this.orderIndex = orderIndex;
    }
}
