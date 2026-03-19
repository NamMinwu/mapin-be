package com.mapin.content.application;

import com.mapin.content.domain.Content;
import com.mapin.content.dto.RecommendationCandidate;
import com.mapin.content.dto.SelectedRecommendation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FinalRecommendationSelector {

    public List<SelectedRecommendation> select(List<RecommendationCandidate> candidates, int limit) {
        if (candidates == null || candidates.isEmpty() || limit <= 0) {
            return List.of();
        }

        List<RecommendationCandidate> remaining = new ArrayList<>(candidates);
        remaining.sort(Comparator.comparing(RecommendationCandidate::candidateScore).reversed());

        List<SelectedRecommendation> selected = new ArrayList<>();
        while (!remaining.isEmpty() && selected.size() < limit) {
            SelectedRecommendation best = null;
            int bestIndex = -1;
            for (int i = 0; i < remaining.size(); i++) {
                RecommendationCandidate candidate = remaining.get(i);
                double diversityScore = diversityFromSelected(candidate, selected);
                double selectionScore = 0.7 * candidate.candidateScore() + 0.3 * diversityScore;
                if (best == null || selectionScore > best.selectionScore()) {
                    best = new SelectedRecommendation(candidate, diversityScore, selectionScore);
                    bestIndex = i;
                }
            }
            if (best == null) {
                break;
            }
            selected.add(best);
            remaining.remove(bestIndex);
        }
        return selected;
    }

    private double diversityFromSelected(RecommendationCandidate candidate, List<SelectedRecommendation> selected) {
        if (selected.isEmpty()) {
            return 1.0;
        }

        double score = 1.0;
        String candidateFrame = candidate.content().getFrame();
        String candidateFormat = candidate.content().getFormat();
        String candidateTone = candidate.content().getTone();
        String candidateScope = candidate.content().getScope();
        String candidateChannel = candidate.content().getChannelTitle();
        String candidateChannelType = resolveChannelType(candidate.content());

        long sameFrameCount = selected.stream()
                .map(SelectedRecommendation::candidate)
                .filter(existing -> safeEquals(existing.content().getFrame(), candidateFrame))
                .count();
        if (sameFrameCount > 0) {
            score -= 0.45;
        }

        long sameToneCount = selected.stream()
                .map(SelectedRecommendation::candidate)
                .filter(existing -> safeEquals(existing.content().getTone(), candidateTone))
                .count();
        if (sameToneCount > 0) {
            score -= 0.35;
        }

        long sameFormatCount = selected.stream()
                .map(SelectedRecommendation::candidate)
                .filter(existing -> safeEquals(existing.content().getFormat(), candidateFormat))
                .count();
        if (sameFormatCount > 0) {
            score -= 0.35;
        }

        boolean sameChannelExists = selected.stream()
                .map(SelectedRecommendation::candidate)
                .anyMatch(existing -> safeEquals(existing.content().getChannelTitle(), candidateChannel));
        if (sameChannelExists) {
            score -= 0.4;
        }

        boolean sameChannelTypeExists = selected.stream()
                .map(SelectedRecommendation::candidate)
                .anyMatch(existing -> resolveChannelType(existing.content()).equals(candidateChannelType));
        if (sameChannelTypeExists) {
            score -= 0.15;
        }

        long scopeCount = selected.stream()
                .map(SelectedRecommendation::candidate)
                .filter(existing -> safeEquals(existing.content().getScope(), candidateScope))
                .count();
        if (scopeCount >= 2) {
            score -= 0.25;
        } else if (scopeCount == 1) {
            score -= 0.12;
        }

        return clamp(score);
    }

    private String resolveChannelType(Content content) {
        String title = content.getChannelTitle();
        if (title == null) {
            return "UNKNOWN";
        }
        String normalized = title.toLowerCase();
        if (normalized.contains("news") || normalized.contains("뉴스") || normalized.contains("tv") || normalized.contains("방송")) {
            return "MEDIA";
        }
        if (normalized.contains("공식") || normalized.contains("channel")) {
            return "OFFICIAL";
        }
        return "CREATOR";
    }

    private boolean safeEquals(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private double clamp(double score) {
        if (score < 0.0) {
            return 0.0;
        }
        if (score > 1.0) {
            return 1.0;
        }
        return score;
    }
}
