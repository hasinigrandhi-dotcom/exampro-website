
package com.exampro;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;

public class TestStore {

    private final Path folder = Path.of("data");
    private final Path testsFile = folder.resolve("tests.txt");
    private final Path historyFile = folder.resolve("test_history.txt");

    public TestStore() {
        try {
            Files.createDirectories(folder);
            if (!Files.exists(testsFile)) Files.createFile(testsFile);
            if (!Files.exists(historyFile)) Files.createFile(historyFile);
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize test storage", e);
        }
    }

    private String enc(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String dec(String value) {
        return new String(
                Base64.getDecoder().decode(value),
                StandardCharsets.UTF_8
        );
    }

    private void append(Path file, String line) {
        try {
            Files.writeString(
                    file,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new RuntimeException("Could not save data", e);
        }
    }

    public List<ExamTest> getTests() {
        List<ExamTest> result = new ArrayList<>();

        try {
            for (String line : Files.readAllLines(testsFile)) {
                String[] p = line.split("\\|", -1);
                if (p.length != 3) continue;

                try {
                    result.add(new ExamTest(
                            dec(p[0]),
                            dec(p[1]),
                            Integer.parseInt(p[2])
                    ));
                } catch (Exception ignored) {
                    // Ignore invalid records.
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public void createTest(String title, int durationMinutes) {
        String id = UUID.randomUUID().toString();

        ExamTest test = new ExamTest(id, title, durationMinutes);

        if (getTests().stream().anyMatch(t ->
                t.title().equalsIgnoreCase(test.title()))) {
            throw new IllegalArgumentException(
                    "A test with this title already exists."
            );
        }

        append(testsFile,
                enc(test.id()) + "|" +
                enc(test.title()) + "|" +
                test.durationMinutes()
        );

        try {
            Path questionFile = questionFile(test.id());
            Files.createFile(questionFile);
        } catch (IOException e) {
            throw new RuntimeException("Could not create test question file", e);
        }
    }

    private Path questionFile(String testId) {
        return folder.resolve("questions_" + testId + ".txt");
    }

    public List<TestQuestion> getQuestions(String testId) {
        List<TestQuestion> result = new ArrayList<>();
        Path file = questionFile(testId);

        if (!Files.exists(file)) return result;

        try {
            for (String line : Files.readAllLines(file)) {
                String[] p = line.split("\\|", -1);
                if (p.length != 6) continue;

                try {
                    result.add(new TestQuestion(
                            dec(p[0]),
                            List.of(dec(p[1]), dec(p[2]),
                                    dec(p[3]), dec(p[4])),
                            Integer.parseInt(p[5])
                    ));
                } catch (Exception ignored) {
                    // Ignore invalid records.
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public void addQuestion(String testId, TestQuestion question) {
        if (getTests().stream().noneMatch(t -> t.id().equals(testId))) {
            throw new IllegalArgumentException("Test does not exist.");
        }

        append(questionFile(testId),
                enc(question.text()) + "|" +
                enc(question.options().get(0)) + "|" +
                enc(question.options().get(1)) + "|" +
                enc(question.options().get(2)) + "|" +
                enc(question.options().get(3)) + "|" +
                question.correctIndex()
        );
    }

    public boolean hasCompleted(String rollNo, String testId) {
        return getAttempts().stream().anyMatch(a ->
                a.rollNo().equalsIgnoreCase(rollNo)
                && a.testId().equals(testId)
        );
    }

    public void saveAttempt(
            Student student,
            ExamTest test,
            int total,
            int attempted,
            int correct
    ) {
        if (hasCompleted(student.rollNo(), test.id())) {
            throw new IllegalStateException(
                    "You have already completed this test."
            );
        }

        double percentage = total == 0
                ? 0 : correct * 100.0 / total;

        String line = String.join("|",
                enc(test.id()),
                enc(test.title()),
                enc(student.rollNo()),
                enc(student.name()),
                enc(student.email()),
                enc(student.course()),
                String.valueOf(total),
                String.valueOf(attempted),
                String.valueOf(correct),
                String.valueOf(percentage),
                String.valueOf(percentage >= 40),
                enc(LocalDateTime.now().toString())
        );

        append(historyFile, line);
    }

    public record TestAttempt(
            String testId,
            String testTitle,
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
    ) {}

    public List<TestAttempt> getAttempts() {
        List<TestAttempt> result = new ArrayList<>();

        try {
            for (String line : Files.readAllLines(historyFile)) {
                String[] p = line.split("\\|", -1);
                if (p.length != 12) continue;

                try {
                    result.add(new TestAttempt(
                            dec(p[0]), dec(p[1]), dec(p[2]),
                            dec(p[3]), dec(p[4]), dec(p[5]),
                            Integer.parseInt(p[6]),
                            Integer.parseInt(p[7]),
                            Integer.parseInt(p[8]),
                            Double.parseDouble(p[9]),
                            Boolean.parseBoolean(p[10]),
                            dec(p[11])
                    ));
                } catch (Exception ignored) {
                    // Ignore invalid records.
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public List<TestAttempt> getStudentAttempts(String rollNo) {
        return getAttempts().stream()
                .filter(a -> a.rollNo().equalsIgnoreCase(rollNo))
                .toList();
    }
}
