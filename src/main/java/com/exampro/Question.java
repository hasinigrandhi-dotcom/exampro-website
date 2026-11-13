package com.exampro;
import java.util.List;
public record Question(String text, List<String> options, int correctIndex) {
    public Question {
        if (text == null || text.isBlank() || options == null || options.size()!=4 || correctIndex<0 || correctIndex>3)
            throw new IllegalArgumentException("Enter a question, four options and a correct answer.");
        options = List.copyOf(options);
    }
}