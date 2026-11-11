package com.example.volunteer.service;

import com.example.volunteer.vo.RecommendationVO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 在结果较多时优先保留冲刺、稳妥和保底的代表项，避免最低门槛院校挤满全部名额。
 */
public final class RecommendationResultSelector {

    private static final List<String> LEVEL_ORDER = List.of(
            "冲刺",
            "稳妥",
            "保底",
            "不建议"
    );

    private RecommendationResultSelector() {
    }

    public static List<RecommendationVO> select(
            List<RecommendationVO> candidates,
            int maxResults
    ) {
        if (maxResults <= 0 || candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Map<String, List<RecommendationVO>> grouped = new LinkedHashMap<>();
        for (String level : LEVEL_ORDER) {
            grouped.put(level, new ArrayList<>());
        }
        for (RecommendationVO candidate : candidates) {
            grouped.computeIfAbsent(candidate.matchLevel(), key -> new ArrayList<>())
                    .add(candidate);
        }
        grouped.values().forEach(items -> items.sort(scoreComparator()));

        List<String> availableRecommendedLevels = LEVEL_ORDER.subList(0, 3).stream()
                .filter(level -> !grouped.get(level).isEmpty())
                .toList();
        List<RecommendationVO> selected = new ArrayList<>();
        Set<RecommendationVO> selectedItems = new HashSet<>();

        if (!availableRecommendedLevels.isEmpty()) {
            int baseQuota = maxResults / availableRecommendedLevels.size();
            int remainder = maxResults % availableRecommendedLevels.size();
            for (int index = 0; index < availableRecommendedLevels.size(); index++) {
                String level = availableRecommendedLevels.get(index);
                int quota = baseQuota + (index < remainder ? 1 : 0);
                addFromLevel(grouped.get(level), quota, selected, selectedItems);
            }
        }

        // 某类数量不足时，先用其他推荐类补满，再考虑“不建议”。
        for (String level : LEVEL_ORDER) {
            if (selected.size() >= maxResults) break;
            addFromLevel(
                    grouped.get(level),
                    maxResults - selected.size(),
                    selected,
                    selectedItems
            );
        }

        selected.sort(
                Comparator.comparingInt((RecommendationVO item) -> levelIndex(item.matchLevel()))
                        .thenComparing(scoreComparator())
        );
        return selected.size() > maxResults
                ? List.copyOf(selected.subList(0, maxResults))
                : List.copyOf(selected);
    }

    private static void addFromLevel(
            List<RecommendationVO> levelItems,
            int quota,
            List<RecommendationVO> selected,
            Set<RecommendationVO> selectedItems
    ) {
        int added = 0;
        for (RecommendationVO item : levelItems) {
            if (added >= quota) break;
            if (selectedItems.add(item)) {
                selected.add(item);
                added++;
            }
        }
    }

    private static Comparator<RecommendationVO> scoreComparator() {
        return Comparator.comparing(
                        RecommendationVO::matchScore,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(
                        RecommendationVO::universityId,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
    }

    private static int levelIndex(String level) {
        int index = LEVEL_ORDER.indexOf(level);
        return index < 0 ? LEVEL_ORDER.size() : index;
    }
}
