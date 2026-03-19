package com.mapin.content.dto;

public record PerspectiveAnalysisResult(
        String category,
        String frame,
        String scope,
        String tone,
        String format,
        String perspectiveLevel,
        String perspectiveStakeholder
) {
}
