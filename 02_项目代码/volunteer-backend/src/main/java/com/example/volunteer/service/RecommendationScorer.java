package com.example.volunteer.service;

import java.util.Locale;

/**
 * 支持按位次或按分数单一条件比较历史录取数据，生成冲稳保参考结果。
 */
public final class RecommendationScorer {

    private RecommendationScorer() {
    }

    public record Result(int matchScore, String matchLevel, String reason) {
    }

    public static Result score(
            Integer userScore,
            Integer userRank,
            int averageMinScore,
            int averageMinRank
    ) {
        if ((userScore == null && userRank == null)
                || (userScore != null && (userScore < 0 || userScore > 750))
                || (userRank != null && userRank <= 0)
                || averageMinScore <= 0
                || averageMinRank <= 0) {
            throw new IllegalArgumentException("分数、位次和历史录取数据不正确");
        }

        // 相对位次差为正表示考生位次优于历史最低位次，为负表示落后。
        double rankComponent = userRank == null
                ? 0
                : clamp(50 + (averageMinRank - userRank) / (double) averageMinRank * 100, 0, 100);
        double scoreComponent = userScore == null
                ? 0
                : clamp(
                        50 + (userScore - averageMinScore) / (double) averageMinScore
                                * (userRank == null ? 500 : 100),
                        0,
                        100
                );
        int matchScore;
        if (userRank == null) {
            matchScore = (int) Math.round(scoreComponent);
        } else if (userScore == null) {
            matchScore = (int) Math.round(rankComponent);
        } else {
            // 两项都填写时保持原有权重：位次 70%，分数 30%。
            matchScore = (int) Math.round(rankComponent * 0.7 + scoreComponent * 0.3);
        }

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

        String reason;
        if (userRank == null) {
            reason = String.format(
                    Locale.ROOT,
                    "你的分数与近三年平均最低分相差 %+d 分。本次只按分数筛选。",
                    userScore - averageMinScore
            );
        } else {
            int rankDifference = averageMinRank - userRank;
            String rankDescription = rankDifference >= 0
                    ? "你的位次高于近三年平均最低位次 " + rankDifference + " 名"
                    : "你的位次低于近三年平均最低位次 " + Math.abs(rankDifference) + " 名";
            reason = userScore == null
                    ? rankDescription + "。本次只按位次筛选。"
                    : String.format(
                            Locale.ROOT,
                            "%s，分数与平均最低分相差 %+d 分。筛选时主要看位次，分数作为补充。",
                            rankDescription,
                            userScore - averageMinScore
                    );
        }
        return new Result(matchScore, matchLevel, reason);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
