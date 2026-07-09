package com.blueforge.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClarifyingQuestionTest {

    @Test
    void settingAnswerWiresBothSidesOfTheBidirectionalAssociation() {
        Project project = new Project("Test Project");
        ProjectVersion version = new ProjectVersion(project, 1, "idea", ProjectVersionStatus.AWAITING_ANSWERS);
        ClarifyingQuestion question = new ClarifyingQuestion(version, "What is the primary user type?", 0);

        ClarifyingAnswer answer = new ClarifyingAnswer(question, "End consumers");
        question.setAnswer(answer);

        assertThat(question.getAnswer()).isSameAs(answer);
        assertThat(answer.getClarifyingQuestion()).isSameAs(question);
    }
}
