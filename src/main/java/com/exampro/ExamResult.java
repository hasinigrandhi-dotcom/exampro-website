
package com.exampro;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ExamResult(
        String rollNo,
        String studentName,
        String email,
        String course,
        int total,
        int attempted,
        int correct,
        double percentage,
        boolean passed,
        String submittedAt
) {

    public static ExamResult create(
            Student s,
            int total,
            int attempted,
            int correct
    ) {

        double pct = total == 0
                ? 0
                : correct * 100.0 / total;

        String date = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
        );

        return new ExamResult(
                s.rollNo(),
                s.name(),
                s.email(),
                s.course(),
                total,
                attempted,
                correct,
                pct,
                pct >= 40,
                date
        );
    }
}
