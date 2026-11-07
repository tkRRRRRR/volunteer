package com.example.volunteer.service;

import java.util.Locale;

public final class RecommendationScorer {

    private RecommendationScorer() {
    }

    public record Result(int matchScore, String matchLevel, String reason) {
    }

    public static Result score(int userScore, int userRank, int averageMinScore, int averageMinRank) {
        if (userScore < 0 || userRank <= 0 || averageMinScore <= 0 || averageMinRank <= 0) {
            throw new IllegalArgumentException("成绩、位次和历史录取数据必须为正数");
        }

        double rankGap = (averageMinRank - userRank) / (double) averageMinRank;
        double scoreGap = (userScore - averageMinScore) / (double) averageMinScore;
        double rankComponent = clamp(50 + rankGap * 100, 0, 100);
        double scoreComponent = clamp(50 + scoreGap * 100, 0, 100);
        int matchScore = (int) Math.round(rankComponent * 0.7 + scoreComponent * 0.3);

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
                "%s，分数与平均最低分相差 %+d 分；按位次为主、分数为辅计算匹配分。",
                rankDescription,
                userScore - averageMinScore
        );
        return new Result(matchScore, matchLevel, reason);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
