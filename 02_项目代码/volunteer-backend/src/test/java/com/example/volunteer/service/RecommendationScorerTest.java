package com.example.volunteer.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationScorerTest {

    @Test
    void strongerRankShouldReceiveHigherMatchScore() {
        RecommendationScorer.Result stronger = RecommendationScorer.score(560, 9000, 550, 10000);
        RecommendationScorer.Result weaker = RecommendationScorer.score(560, 11000, 550, 10000);

        assertTrue(stronger.matchScore() > weaker.matchScore());
        assertEquals("稳妥", stronger.matchLevel());
        assertEquals("冲刺", weaker.matchLevel());
    }

    @Test
    void clearlyAheadShouldBeSafeChoice() {
        RecommendationScorer.Result result = RecommendationScorer.score(590, 7000, 550, 12000);

        assertEquals("保底", result.matchLevel());
        assertTrue(result.matchScore() >= 70);
    }

    @Test
    void clearlyBehindShouldNotBeRecommended() {
        RecommendationScorer.Result result = RecommendationScorer.score(500, 20000, 560, 10000);

        assertEquals("不建议", result.matchLevel());
        assertTrue(result.matchScore() < 42);
    }

    @Test
    void invalidDataShouldBeRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RecommendationScorer.score(560, 0, 550, 10000)
        );
    }

    @Test
    void shouldSupportRankOnlyFiltering() {
        RecommendationScorer.Result result =
                RecommendationScorer.score(null, 10000, 550, 10000);

        assertEquals("冲刺", result.matchLevel());
        assertTrue(result.reason().contains("本次只按位次筛选"));
    }

    @Test
    void shouldSupportScoreOnlyFiltering() {
        RecommendationScorer.Result result =
                RecommendationScorer.score(550, null, 550, 10000);

        assertEquals("冲刺", result.matchLevel());
        assertTrue(result.reason().contains("本次只按分数筛选"));
    }

    @Test
    void shouldRejectRequestWithoutScoreAndRank() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RecommendationScorer.score(null, null, 550, 10000)
        );
    }

    @Test
    void scoreOnlyShouldUseScoreSpecificLevelRanges() {
        RecommendationScorer.Result difficult =
                RecommendationScorer.score(560, null, 580, 10000);
        RecommendationScorer.Result different =
                RecommendationScorer.score(560, null, 540, 10000);
        RecommendationScorer.Result safe =
                RecommendationScorer.score(560, null, 510, 10000);

        assertEquals("不建议", difficult.matchLevel());
        assertEquals("稳妥", different.matchLevel());
        assertEquals("保底", safe.matchLevel());
    }
}
