
package com.exampro;

public record ExamTest(
        String id,
        String title,
        int durationMinutes
) {
    public ExamTest {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("Test ID is required.");

        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Test title is required.");

        if (durationMinutes < 1)
            throw new IllegalArgumentException("Duration must be at least 1 minute.");
    }
}

