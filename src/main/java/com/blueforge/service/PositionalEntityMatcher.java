package com.blueforge.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

// Aligns entities by list position. The lists are always pre-sorted by orderIndex (@OrderBy on the entity),
// so index position already reflects orderIndex without needing to read the field.
@Component
public class PositionalEntityMatcher implements VersionEntityMatcher {

    @Override
    public <T> List<MatchedPair<T>> match(List<T> before, List<T> after) {
        int size = Math.max(before.size(), after.size());
        List<MatchedPair<T>> pairs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            T beforeItem = i < before.size() ? before.get(i) : null;
            T afterItem = i < after.size() ? after.get(i) : null;
            pairs.add(new MatchedPair<>(beforeItem, afterItem));
        }
        return pairs;
    }
}
