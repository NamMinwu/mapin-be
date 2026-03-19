package com.mapin.content.dto;

public record ContentPerspectiveResponse(
        Long contentId,
        String category,
        String frame,
        String scope,
        String tone,
        String format,
        String perspectiveLevel,
        String perspectiveStakeholder
) {
}
