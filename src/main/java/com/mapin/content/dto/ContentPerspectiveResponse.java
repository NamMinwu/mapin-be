package com.mapin.content.dto;

public record ContentPerspectiveResponse(
        Long contentId,
        String category,
        String perspectiveLevel,
        String perspectiveStakeholder
) {
}
