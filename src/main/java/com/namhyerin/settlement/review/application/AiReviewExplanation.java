package com.namhyerin.settlement.review.application;

import java.util.List;

public record AiReviewExplanation(
        String summary,
        List<String> checkPoints,
        SuggestedAction suggestedAction,
        boolean available
) {
}
