
package com.exampro;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class FileStore {

    private final Path dir = Path.of("data");
    private final Path students = dir.resolve("students.txt");
    private final Path questions = dir.resolve("questions.txt");
    private final Path history = dir.resolve("exam_history.txt");

    public FileStore() {
        try {
            Files.createDirectories(dir);

            for (Path p : List.of(students, questions, history)) {
                if (!Files.exists(p)) {
                    Files.createFile(p);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize data files", e);
        }
    }

    // ENCODING
    private String enc(String s) {
        return Base64.getEncoder().encodeToString(
                Objects.toString(s, "").getBytes(StandardCharsets.UTF_8)
        );
    }

    private String dec(String s) {
        return new String(
                Base64.getDecoder().decode(s),
                StandardCharsets.UTF_8
        );
    }

    // READ FILE
    private List<String> lines(Path p) {
        try {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // APPEND DATA
    private void append(Path p, String line) {
        try {
            Files.writeString(
                    p,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // STUDENT MANAGEMENT
    public List<Student> getStudents() {

        return lines(students).stream()
                .filter(x -> !x.isBlank())
                .map(x -> {
                    try {
                        String[] a = x.split("\\|", -1);

                        return new Student(
                                dec(a[0]),
                                dec(a[1]),
                                dec(a[2]),
                                dec(a[3])
                        );

                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void saveStudent(Student s) {

        if (getStudents().stream().anyMatch(
                x -> x.rollNo().equalsIgnoreCase(s.rollNo())
        )) {
            throw new IllegalArgumentException(
                    "Roll number already registered."
            );
        }

        append(
                students,
                String.join("|",
                        enc(s.rollNo()),
                        enc(s.name()),
                        enc(s.email()),
                        enc(s.course())
                )
        );
    }

    // SAMPLE STUDENTS
    public void addSampleStudents() {

        List<Student> samples = List.of(
                new Student("EX001", "Rahul Kumar", "rahul@example.com", "CSE"),
                new Student("EX002", "Priya Sharma", "priya@example.com", "ECE"),
                new Student("EX003", "Kiran Reddy", "kiran@example.com", "IT"),
                new Student("EX004", "Sneha Patel", "sneha@example.com", "CSE"),
                new Student("EX005", "Arjun Kumar", "arjun@example.com", "EEE")
        );

        for (Student sample : samples) {
            boolean exists = getStudents().stream()
                    .anyMatch(s -> s.rollNo().equalsIgnoreCase(sample.rollNo()));

            if (!exists) {
                saveStudent(sample);
            }
        }
    }

    // QUESTION MANAGEMENT
    public List<Question> getQuestions() {

        return lines(questions).stream()
                .filter(x -> !x.isBlank())
                .map(x -> {
                    try {
                        String[] a = x.split("\\|", -1);

                        return new Question(
                                dec(a[0]),
                                List.of(
                                        dec(a[1]),
                                        dec(a[2]),
                                        dec(a[3]),
                                        dec(a[4])
                                ),
                                Integer.parseInt(a[5])
                        );

                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void addQuestion(Question q) {

        append(
                questions,
                String.join("|",
                        enc(q.text()),
                        enc(q.options().get(0)),
                        enc(q.options().get(1)),
                        enc(q.options().get(2)),
                        enc(q.options().get(3)),
                        String.valueOf(q.correctIndex())
                )
        );
    }

    public void deleteQuestion(int index) {

        List<Question> qs = new ArrayList<>(getQuestions());

        if (index < 0 || index >= qs.size()) {
            return;
        }

        qs.remove(index);

        try {
            Files.write(
                    questions,
                    qs.stream()
                            .map(q -> String.join("|",
                                    enc(q.text()),
                                    enc(q.options().get(0)),
                                    enc(q.options().get(1)),
                                    enc(q.options().get(2)),
                                    enc(q.options().get(3)),
                                    String.valueOf(q.correctIndex())
                            ))
                            .toList(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // SAVE EXAMINATION RESULT
    public void saveResult(ExamResult r) {

        append(
                history,
                String.join("|",
                        enc(r.rollNo()),
                        enc(r.studentName()),
                        enc(r.email()),
                        enc(r.course()),
                        String.valueOf(r.total()),
                        String.valueOf(r.attempted()),
                        String.valueOf(r.correct()),
                        String.format(Locale.US, "%.2f", r.percentage()),
                        String.valueOf(r.passed()),
                        enc(r.submittedAt())
                )
        );
    }

    // READ ALL EXAMINATION RESULTS
    // Supports both old and new history formats.
    public List<ExamResult> getResults() {

        List<ExamResult> results = new ArrayList<>();

        for (String line : lines(history)) {

            if (line.isBlank()) {
                continue;
            }

            try {

                String[] a = line.split("\\|", -1);

                if (a.length >= 10) {

                    results.add(new ExamResult(
                            dec(a[0]),
                            dec(a[1]),
                            dec(a[2]),
                            dec(a[3]),
                            Integer.parseInt(a[4]),
                            Integer.parseInt(a[5]),
                            Integer.parseInt(a[6]),
                            Double.parseDouble(a[7]),
                            Boolean.parseBoolean(a[8]),
                            dec(a[9])
                    ));

                } else if (a.length >= 8) {

                    // Older saved results
                    results.add(new ExamResult(
                            dec(a[0]),
                            dec(a[1]),
                            "",
                            "",
                            Integer.parseInt(a[2]),
                            Integer.parseInt(a[3]),
                            Integer.parseInt(a[4]),
                            Double.parseDouble(a[5]),
                            Boolean.parseBoolean(a[6]),
                            dec(a[7])
                    ));
                }

            } catch (Exception e) {
                System.out.println(
                        "Could not read an examination history record: " + e.getMessage()
                );
            }
        }

        return results;
    }

// CHECK WHETHER STUDENT HAS ALREADY COMPLETED THE EXAM
public boolean hasStudentCompletedExam(String rollNo) {

    return getResults().stream()
            .anyMatch(result ->
                    result.rollNo().equalsIgnoreCase(rollNo)
            );
}

    // OLD HISTORY METHOD - KEPT FOR COMPATIBILITY
    public List<String> getHistory() {
        return lines(history);
    }
}
