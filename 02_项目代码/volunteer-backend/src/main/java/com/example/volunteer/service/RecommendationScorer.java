package com.example.volunteer.service;

import java.util.Locale;

/**
 * 将考生位次和分数与历史录取数据进行比较，生成可解释的辅助匹配结果。
 * 位次权重高于分数，避免不同年份试题难度变化对结果造成过大影响。
 */
public final class RecommendationScorer {

    private RecommendationScorer() {
    }

    public record Result(int matchScore, String matchLevel, String reason) {
    }

    public static Result score(int userScore, int userRank, int averageMinScore, int averageMinRank) {
        if (userScore < 0 || userRank <= 0 || averageMinScore <= 0 || averageMinRank <= 0) {
            throw new IllegalArgumentException("成绩、位次和历史录取数据必须为正数");
        }

        // 相对位次差为正表示考生位次优于历史最低位次，为负表示落后。
        double rankGap = (averageMinRank - userRank) / (double) averageMinRank;
        double scoreGap = (userScore - averageMinScore) / (double) averageMinScore;
        double rankComponent = clamp(50 + rankGap * 100, 0, 100);
        double scoreComponent = clamp(50 + scoreGap * 100, 0, 100);
        // 位次占 70%，分数占 30%，两项都限制在 0 到 100 分之间。
        int matchScore = (int) Math.round(rankComponent * 0.7 + scoreComponent * 0.3);

        // 等级只是资料整理标签，不代表系统对录取概率作出承诺。
        String matchLevel;
        if (matchScore >= 70) {
            matchLevel = "保底";
        } else if (matchScore >= 57) {
            matchLevel = "稳妥";
        } else if (matchScore >= 42) {
            matchLevel = "冲刺";
        } else {
            matchLevel = "不建议";
        }

        int rankDifference = averageMinRank - userRank;
        String rankDescription = rankDifference >= 0
                ? "你的位次高于近三年平均最低位次 " + rankDifference + " 名"
                : "你的位次低于近三年平均最低位次 " + Math.abs(rankDifference) + " 名";
        String reason = String.format(
                Locale.ROOT,
                "%s，分数与平均最低分相差 %+d 分。筛选时主要看位次，分数作为补充。",
                rankDescription,
                userScore - averageMinScore
        );
        return new Result(matchScore, matchLevel, reason);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
