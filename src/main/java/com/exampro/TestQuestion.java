
package com.exampro;

import java.util.List;

public record TestQuestion(
        String text,
        List<String> options,
        int correctIndex
) {
    public TestQuestion {
        text = text == null ? "" : text.trim();
        options = options == null ? List.of() : List.copyOf(options);

        if (text.isBlank())
            throw new IllegalArgumentException("Question is required.");

        if (options.size() != 4)
            throw new IllegalArgumentException("Exactly 4 options are required.");

        if (options.stream().anyMatch(o -> o == null || o.isBlank()))
            throw new IllegalArgumentException("All 4 options are required.");

        if (correctIndex < 0 || correctIndex > 3)
            throw new IllegalArgumentException("Select the correct answer.");
    }
}
