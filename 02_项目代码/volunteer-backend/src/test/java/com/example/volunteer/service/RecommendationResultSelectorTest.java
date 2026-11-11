package com.example.volunteer.service;

import com.example.volunteer.vo.RecommendationVO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendationResultSelectorTest {

    @Test
    void shouldKeepRepresentativeResultsFromEachRecommendedLevel() {
        List<RecommendationVO> candidates = new ArrayList<>();
        candidates.addAll(candidates("冲刺", 60));
        candidates.addAll(candidates("稳妥", 60));
        candidates.addAll(candidates("保底", 60));
        candidates.addAll(candidates("不建议", 60));

        List<RecommendationVO> selected =
                RecommendationResultSelector.select(candidates, 50);

        Map<String, Long> counts = selected.stream()
                .collect(Collectors.groupingBy(RecommendationVO::matchLevel, Collectors.counting()));
        assertEquals(50, selected.size());
        assertEquals(17L, counts.get("冲刺"));
        assertEquals(17L, counts.get("稳妥"));
        assertEquals(16L, counts.get("保底"));
        assertEquals(null, counts.get("不建议"));
        assertEquals(List.of("冲刺", "稳妥", "保底"), selected.stream()
                .map(RecommendationVO::matchLevel)
                .distinct()
                .toList());
    }

    @Test
    void shouldSortScoresDescendingInsideEachLevel() {
        List<RecommendationVO> selected = RecommendationResultSelector.select(
                candidates("稳妥", 5),
                3
        );

        assertEquals(List.of(5, 4, 3), selected.stream()
                .map(RecommendationVO::matchScore)
                .toList());
    }

    @Test
    void shouldFillRemainingSlotsFromNotRecommendedWhenNeeded() {
        List<RecommendationVO> candidates = new ArrayList<>();
        candidates.addAll(candidates("稳妥", 2));
        candidates.addAll(candidates("保底", 2));
        candidates.addAll(candidates("不建议", 10));

        List<RecommendationVO> selected =
                RecommendationResultSelector.select(candidates, 10);

        Map<String, Long> counts = selected.stream()
                .collect(Collectors.groupingBy(RecommendationVO::matchLevel, Collectors.counting()));
        assertEquals(10, selected.size());
        assertEquals(2L, counts.get("稳妥"));
        assertEquals(2L, counts.get("保底"));
        assertEquals(6L, counts.get("不建议"));
    }

    @Test
    void shouldReturnEmptyWhenLimitIsZero() {
        assertEquals(List.of(), RecommendationResultSelector.select(candidates("冲刺", 3), 0));
    }

    private List<RecommendationVO> candidates(String level, int count) {
        List<RecommendationVO> results = new ArrayList<>();
        for (int index = count; index >= 1; index--) {
            results.add(recommendation(level, index));
        }
        return results;
    }

    private RecommendationVO recommendation(String level, int score) {
        return new RecommendationVO(
                (long) score,
                "演示院校" + score,
                "山西",
                "太原",
                "双一流",
                1L,
                "软件工程",
                "物理+不限",
                2025,
                550,
                10000,
                score,
                level,
                "演示原因",
                "演示数据（非官方）"
        );
    }
}
