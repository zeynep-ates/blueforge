package com.blueforge.service;

import java.util.List;

// Pairs entities from two ProjectVersions; ids are never comparable across versions, so matching needs a strategy.
public interface VersionEntityMatcher {

    <T> List<MatchedPair<T>> match(List<T> before, List<T> after);
}
