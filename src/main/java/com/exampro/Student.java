
package com.exampro;

public record Student(
        String rollNo,
        String name,
        String email,
        String course
) {

    public Student {
        rollNo = rollNo == null ? "" : rollNo.trim();
        name = name == null ? "" : name.trim();
        email = email == null ? "" : email.trim();
        course = course == null ? "" : course.trim();

        if (rollNo.isBlank()) {
            throw new IllegalArgumentException("Roll number is required.");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("Student name is required.");
        }

        if (!email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }

        if (course.isBlank()) {
            throw new IllegalArgumentException("Course is required.");
        }
    }
}
