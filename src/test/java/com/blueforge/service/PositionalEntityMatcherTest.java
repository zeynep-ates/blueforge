package com.blueforge.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PositionalEntityMatcherTest {

    private final PositionalEntityMatcher matcher = new PositionalEntityMatcher();

    @Test
    void matchesEqualLengthListsPositionally() {
        List<MatchedPair<String>> pairs = matcher.match(List.of("a", "b"), List.of("x", "y"));

        assertThat(pairs).containsExactly(new MatchedPair<>("a", "x"), new MatchedPair<>("b", "y"));
    }

    @Test
    void treatsExtraItemsInAfterAsAdded() {
        List<MatchedPair<String>> pairs = matcher.match(List.of("a"), List.of("a", "b"));

        assertThat(pairs).containsExactly(new MatchedPair<>("a", "a"), new MatchedPair<>(null, "b"));
    }

    @Test
    void treatsExtraItemsInBeforeAsRemoved() {
        List<MatchedPair<String>> pairs = matcher.match(List.of("a", "b"), List.of("a"));

        assertThat(pairs).containsExactly(new MatchedPair<>("a", "a"), new MatchedPair<>("b", null));
    }

    @Test
    void returnsEmptyWhenBothListsEmpty() {
        assertThat(matcher.match(List.of(), List.of())).isEmpty();
    }

    @Test
    void treatsAllItemsAsAddedWhenBeforeIsEmpty() {
        List<MatchedPair<String>> pairs = matcher.match(List.of(), List.of("a", "b"));

        assertThat(pairs).containsExactly(new MatchedPair<>(null, "a"), new MatchedPair<>(null, "b"));
    }

    @Test
    void treatsAllItemsAsRemovedWhenAfterIsEmpty() {
        List<MatchedPair<String>> pairs = matcher.match(List.of("a", "b"), List.of());

        assertThat(pairs).containsExactly(new MatchedPair<>("a", null), new MatchedPair<>("b", null));
    }
}
