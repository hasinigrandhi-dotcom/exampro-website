
package com.exampro;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class ExamProApp extends Application {

    private final FileStore studentsStore = new FileStore();
    private final TestStore testStore = new TestStore();

    private Stage stage;
    private Student currentStudent;
    private ExamTest currentTest;
    private List<TestQuestion> examQuestions = new ArrayList<>();
    private final Map<Integer, Integer> answers = new HashMap<>();

    private int questionIndex;
    private int seconds;
    private boolean submitted;
    private Timeline timer;

    private Label label(String text) {
        return new Label(text);
    }

    private Button button(String text) {
        return new Button(text);
    }

    private VBox page(String title) {
        VBox box = new VBox(15);
        box.setPadding(new Insets(22));
        box.getStyleClass().add("page");

        Label heading = label(title);
        heading.getStyleClass().add("page-title");

        box.getChildren().add(heading);
        return box;
    }

    private void show(Parent root, String title) {
        Scene scene = new Scene(root, 1150, 780);

        var css = getClass().getResource("/css/style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("ExamPro - " + title);
        stage.setScene(scene);
        stage.show();
    }

    private void alert(String message) {
        new Alert(Alert.AlertType.INFORMATION,
                message, ButtonType.OK).showAndWait();
    }

    private void error(String message) {
        new Alert(Alert.AlertType.ERROR,
                message, ButtonType.OK).showAndWait();
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        showLogin();
    }

    // ================= LOGIN =================

    private void showLogin() {
        VBox root = page("EXAMPRO");
        root.setAlignment(Pos.CENTER);

        ComboBox<String> role = new ComboBox<>();
        role.getItems().addAll("Student", "Admin");
        role.setValue("Student");

        TextField roll = new TextField();
        roll.setPromptText("Student Roll Number");

        TextField name = new TextField();
        name.setPromptText("Student Name");

        TextField email = new TextField();
        email.setPromptText("Email");

        TextField course = new TextField();
        course.setPromptText("Course");

        PasswordField password = new PasswordField();
        password.setPromptText("Admin Password");
        password.setManaged(false);
        password.setVisible(false);

        Runnable updateRole = () -> {
            boolean admin = "Admin".equals(role.getValue());

            for (Control c : List.of(roll, name, email, course)) {
                c.setVisible(!admin);
                c.setManaged(!admin);
            }

            password.setVisible(admin);
            password.setManaged(admin);
        };

        role.setOnAction(e -> updateRole.run());

        Button login = button("LOGIN");

        login.setOnAction(e -> {
            try {
                if ("Admin".equals(role.getValue())) {
                    if (!"admin123".equals(password.getText())) {
                        throw new IllegalArgumentException(
                                "Incorrect admin password."
                        );
                    }

                    showAdmin();
                    return;
                }

                String r = roll.getText().trim();
                String n = name.getText().trim();
                String em = email.getText().trim();
                String c = course.getText().trim();

                if (r.isBlank() || n.isBlank()
                        || em.isBlank() || c.isBlank()) {
                    throw new IllegalArgumentException(
                            "Enter all student details."
                    );
                }

                Student student = studentsStore.getStudents().stream()
                        .filter(s -> s.rollNo().equalsIgnoreCase(r))
                        .findFirst()
                        .orElse(null);

                if (student == null) {
                    student = new Student(r, n, em, c);
                    studentsStore.saveStudent(student);
                } else if (!student.email().equalsIgnoreCase(em)) {
                    throw new IllegalArgumentException(
                            "Email does not match this roll number."
                    );
                }

                currentStudent = student;
                showStudentDashboard();

            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });

        root.getChildren().addAll(
                label("Smart Online Examination System"),
                role, roll, name, email, course,
                password, login,
                label("Admin password: admin123")
        );

        updateRole.run();
        show(root, "Login");
    }

    // ================= ADMIN DASHBOARD =================

    private void showAdmin() {
        VBox root = page("ADMIN DASHBOARD");

        TabPane tabs = new TabPane();

        tabs.getTabs().addAll(
                new Tab("Overview", adminOverview()),
                new Tab("Manage Tests", manageTestsTab()),
                new Tab("Students", studentsTab()),
                new Tab("Exam Results", adminResultsTab()),
                new Tab("Student Analysis", adminStudentAnalysis())
        );

        tabs.getTabs().forEach(t -> t.setClosable(false));

        Button logout = button("Logout");
        logout.setOnAction(e -> showLogin());

        root.getChildren().addAll(
                label("Create tests and monitor student performance"),
                tabs,
                logout
        );

        VBox.setVgrow(tabs, Priority.ALWAYS);
        show(root, "Admin Dashboard");
    }

    private VBox adminOverview() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(15));

        List<ExamTest> tests = testStore.getTests();
        List<Student> students = studentsStore.getStudents();
        List<TestStore.TestAttempt> attempts = testStore.getAttempts();

        long passed = attempts.stream()
                .filter(TestStore.TestAttempt::passed).count();

        double average = attempts.stream()
                .mapToDouble(TestStore.TestAttempt::percentage)
                .average().orElse(0);

        HBox cards = new HBox(12);
        cards.getChildren().addAll(
                statCard("Total Tests", tests.size()),
                statCard("Total Students", students.size()),
                statCard("Submissions", attempts.size()),
                statCard("Pass Count", passed),
                statCard("Average", String.format("%.1f%%", average))
        );

        box.getChildren().addAll(
                cards,
                label("All tests created: " + tests.size()),
                label("All student submissions: " + attempts.size())
        );

        return box;
    }

    private VBox statCard(String title, Object value) {
        VBox card = new VBox(7);
        card.setPadding(new Insets(15));
        card.getStyleClass().add("stat-card");

        Label heading = label(title);
        heading.getStyleClass().add("stat-heading");

        Label number = label(String.valueOf(value));
        number.getStyleClass().add("stat-number");

        card.getChildren().addAll(heading, number);
        return card;
    }

    // ================= ADMIN TEST MANAGEMENT =================

    private VBox manageTestsTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        TextField title = new TextField();
        title.setPromptText("New test name");

        TextField duration = new TextField("10");
        duration.setPromptText("Duration in minutes");

        ListView<ExamTest> testList = new ListView<>();
        testList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ExamTest test, boolean empty) {
                super.updateItem(test, empty);
                setText(empty || test == null ? null :
                        test.title() + " (" + test.durationMinutes() + " min)");
            }
        });

        Runnable refresh = () ->
                testList.getItems().setAll(testStore.getTests());

        Button create = button("CREATE NEW TEST");

        create.setOnAction(e -> {
            try {
                int minutes = Integer.parseInt(duration.getText().trim());
                testStore.createTest(title.getText(), minutes);
                title.clear();
                refresh.run();
                alert("New test created. Select it to add questions.");
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });

        TextField question = new TextField();
        question.setPromptText("Enter question");

        TextField[] options = new TextField[4];
        VBox optionBox = new VBox(7);

        for (int i = 0; i < 4; i++) {
            options[i] = new TextField();
            options[i].setPromptText("Option " + (char) ('A' + i));
            optionBox.getChildren().add(options[i]);
        }

        ComboBox<String> correct = new ComboBox<>();
        correct.getItems().addAll("A", "B", "C", "D");
        correct.setValue("A");

        Button addQuestion = button("ADD QUESTION TO SELECTED TEST");

        addQuestion.setOnAction(e -> {
            try {
                ExamTest selected = testList.getSelectionModel().getSelectedItem();

                if (selected == null) {
                    throw new IllegalArgumentException("Select a test first.");
                }

                testStore.addQuestion(
                        selected.id(),
                        new TestQuestion(
                                question.getText(),
                                Arrays.stream(options)
                                        .map(TextField::getText)
                                        .toList(),
                                correct.getSelectionModel().getSelectedIndex()
                        )
                );

                question.clear();
                Arrays.stream(options).forEach(TextField::clear);

                alert("Question added to " + selected.title());

            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });

        Button viewQuestions = button("VIEW QUESTION COUNT");

        viewQuestions.setOnAction(e -> {
            ExamTest selected = testList.getSelectionModel().getSelectedItem();

            if (selected != null) {
                alert("Questions in " + selected.title() + ": "
                        + testStore.getQuestions(selected.id()).size());
            }
        });

        refresh.run();

        box.getChildren().addAll(
                label("Create New Examination"),
                title,
                duration,
                create,
                new Separator(),
                label("Created Tests - Select a test before adding questions"),
                testList,
                new Separator(),
                label("Add Questions to Selected Test"),
                question,
                optionBox,
                new HBox(10, label("Correct Answer"), correct),
                addQuestion,
                viewQuestions
        );

        VBox.setVgrow(testList, Priority.ALWAYS);
        return box;
    }

    // ================= STUDENTS =================

    private VBox studentsTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        TableView<Student> table = new TableView<>();

        table.getColumns().addAll(
                studentColumn("Roll No", Student::rollNo),
                studentColumn("Name", Student::name),
                studentColumn("Email", Student::email),
                studentColumn("Course", Student::course)
        );

        table.getItems().setAll(studentsStore.getStudents());

        Button refresh = button("Refresh");
        refresh.setOnAction(e ->
                table.getItems().setAll(studentsStore.getStudents()));

        box.getChildren().addAll(
                label("Registered Students"),
                refresh,
                table
        );

        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    // ================= ADMIN RESULTS =================

    private VBox adminResultsTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        TableView<TestStore.TestAttempt> table = new TableView<>();

        table.getColumns().addAll(
                attemptColumn("Test", TestStore.TestAttempt::testTitle),
                attemptColumn("Student", TestStore.TestAttempt::studentName),
                attemptColumn("Roll No", TestStore.TestAttempt::rollNo),
                attemptColumn("Email", TestStore.TestAttempt::email),
                attemptColumn("Course", TestStore.TestAttempt::course),
                attemptColumn("Marks", a -> a.correct() + "/" + a.total()),
                attemptColumn("Percentage",
                        a -> String.format("%.2f%%", a.percentage())),
                attemptColumn("Status", a -> a.passed() ? "PASS" : "FAIL"),
                attemptColumn("Submitted", TestStore.TestAttempt::submittedAt)
        );

        table.getItems().setAll(testStore.getAttempts());

        Button refresh = button("Refresh Results");
        refresh.setOnAction(e ->
                table.getItems().setAll(testStore.getAttempts()));

        box.getChildren().addAll(
                label("All Test Submissions"),
                refresh,
                table
        );

        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    // ================= ADMIN INDIVIDUAL ANALYSIS =================

    private VBox adminStudentAnalysis() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        ComboBox<Student> selector = new ComboBox<>();
        selector.getItems().setAll(studentsStore.getStudents());
        selector.setPromptText("Select student");

        selector.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Student s) {
                return s == null ? "" : s.name() + " (" + s.rollNo() + ")";
            }

            @Override
            public Student fromString(String text) {
                return null;
            }
        });

        Label summary = label("Select a student to see analysis.");

        TableView<TestStore.TestAttempt> table = new TableView<>();
        table.getColumns().addAll(
                attemptColumn("Test", TestStore.TestAttempt::testTitle),
                attemptColumn("Marks", a -> a.correct() + "/" + a.total()),
                attemptColumn("Percentage",
                        a -> String.format("%.2f%%", a.percentage())),
                attemptColumn("Status", a -> a.passed() ? "PASS" : "FAIL"),
                attemptColumn("Date", TestStore.TestAttempt::submittedAt)
        );

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 100, 10);

        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.setTitle("Individual Student Performance");

        Runnable update = () -> {
            Student student = selector.getValue();
            if (student == null) return;

            List<TestStore.TestAttempt> attempts =
                    testStore.getStudentAttempts(student.rollNo());

            double average = attempts.stream()
                    .mapToDouble(TestStore.TestAttempt::percentage)
                    .average().orElse(0);

            double highest = attempts.stream()
                    .mapToDouble(TestStore.TestAttempt::percentage)
                    .max().orElse(0);

            long passed = attempts.stream()
                    .filter(TestStore.TestAttempt::passed).count();

            summary.setText(
                    "Student: " + student.name()
                    + " | Roll: " + student.rollNo()
                    + " | Tests completed: " + attempts.size()
                    + " | Average: " + String.format("%.2f%%", average)
                    + " | Highest: " + String.format("%.2f%%", highest)
                    + " | Passed: " + passed
                    + " | Failed: " + (attempts.size() - passed)
            );

            table.getItems().setAll(attempts);
            chart.getData().clear();

            XYChart.Series<String, Number> series = new XYChart.Series<>();

            for (int i = 0; i < attempts.size(); i++) {
                series.getData().add(new XYChart.Data<>(
                        attempts.get(i).testTitle(),
                        attempts.get(i).percentage()
                ));
            }

            chart.getData().add(series);
        };

        selector.setOnAction(e -> update.run());

        box.getChildren().addAll(
                label("Individual Student Analysis"),
                selector,
                summary,
                chart,
                label("Student Test History"),
                table
        );

        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    // ================= STUDENT DASHBOARD =================

    private void showStudentDashboard() {
        VBox root = page("STUDENT DASHBOARD");

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                new Tab("Available Tests", availableTestsTab()),
                new Tab("My Analysis", studentAnalysisTab()),
                new Tab("My Profile", profileTab())
        );

        tabs.getTabs().forEach(t -> t.setClosable(false));

        Button logout = button("Logout");
        logout.setOnAction(e -> {
            if (timer != null) timer.stop();
            currentStudent = null;
            showLogin();
        });

        root.getChildren().addAll(
                label("Welcome, " + currentStudent.name()),
                tabs,
                logout
        );

        VBox.setVgrow(tabs, Priority.ALWAYS);
        show(root, "Student Dashboard");
    }

    private VBox availableTestsTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        TableView<ExamTest> table = new TableView<>();

        table.getColumns().addAll(
                testColumn("Test Name", ExamTest::title),
                testColumn("Duration", t -> t.durationMinutes() + " minutes"),
                testColumn("Questions", t ->
                        String.valueOf(testStore.getQuestions(t.id()).size())),
                testColumn("Status", t ->
                        testStore.hasCompleted(currentStudent.rollNo(), t.id())
                                ? "COMPLETED" : "AVAILABLE")
        );

        Runnable refresh = () ->
                table.getItems().setAll(testStore.getTests());

        Button start = button("START SELECTED TEST");

        start.setOnAction(e -> {
            ExamTest selected = table.getSelectionModel().getSelectedItem();

            if (selected == null) {
                alert("Please select a test.");
                return;
            }

            if (testStore.hasCompleted(currentStudent.rollNo(), selected.id())) {
                alert("You have already completed this test.");
                return;
            }

            beginTest(selected);
        });

        refresh.run();

        box.getChildren().addAll(
                label("Each test can be attempted only once."),
                table,
                start
        );

        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    // ================= STUDENT PERSONAL ANALYSIS =================

    private VBox studentAnalysisTab() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(15));

        List<TestStore.TestAttempt> attempts =
                testStore.getStudentAttempts(currentStudent.rollNo());

        long passed = attempts.stream()
                .filter(TestStore.TestAttempt::passed).count();

        double average = attempts.stream()
                .mapToDouble(TestStore.TestAttempt::percentage)
                .average().orElse(0);

        double highest = attempts.stream()
                .mapToDouble(TestStore.TestAttempt::percentage)
                .max().orElse(0);

        HBox cards = new HBox(12);
        cards.getChildren().addAll(
                statCard("Completed Tests", attempts.size()),
                statCard("Average Score", String.format("%.1f%%", average)),
                statCard("Highest Score", String.format("%.1f%%", highest)),
                statCard("Passed", passed),
                statCard("Failed", attempts.size() - passed)
        );

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 100, 10);

        LineChart<String, Number> chart = new LineChart<>(x, y);
        chart.setTitle("My Performance");

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (TestStore.TestAttempt attempt : attempts) {
            series.getData().add(new XYChart.Data<>(
                    attempt.testTitle(),
                    attempt.percentage()
            ));
        }

        chart.getData().add(series);

        TableView<TestStore.TestAttempt> table = new TableView<>();
        table.getColumns().addAll(
                attemptColumn("Test", TestStore.TestAttempt::testTitle),
                attemptColumn("Marks", a -> a.correct() + "/" + a.total()),
                attemptColumn("Percentage",
                        a -> String.format("%.2f%%", a.percentage())),
                attemptColumn("Status", a -> a.passed() ? "PASS" : "FAIL"),
                attemptColumn("Date", TestStore.TestAttempt::submittedAt)
        );

        table.getItems().setAll(attempts);

        box.getChildren().addAll(
                label("Your Personal Performance Analysis"),
                cards,
                chart,
                label("My Test History"),
                table
        );

        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private VBox profileTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(18));

        box.getChildren().addAll(
                label("MY PROFILE"),
                label("Name: " + currentStudent.name()),
                label("Roll Number: " + currentStudent.rollNo()),
                label("Email: " + currentStudent.email()),
                label("Course: " + currentStudent.course())
        );

        return box;
    }

    // ================= EXAM ENGINE =================

    private void beginTest(ExamTest test) {
        currentTest = test;
        examQuestions = testStore.getQuestions(test.id());

        if (examQuestions.isEmpty()) {
            alert("Admin has not added questions to this test.");
            return;
        }

        if (testStore.hasCompleted(currentStudent.rollNo(), test.id())) {
            alert("You have already completed this test.");
            return;
        }

        questionIndex = 0;
        seconds = test.durationMinutes() * 60;
        submitted = false;
        answers.clear();

        renderQuestion();
    }

    private void renderQuestion() {
        if (timer != null) timer.stop();

        VBox root = page(currentTest.title());

        Label clock = label("");
        clock.getStyleClass().add("timer-label");
        updateClock(clock);

        Label progress = label(
                "Question " + (questionIndex + 1)
                + " of " + examQuestions.size()
        );

        ProgressBar bar = new ProgressBar(
                (questionIndex + 1.0) / examQuestions.size()
        );
        bar.setMaxWidth(Double.MAX_VALUE);

        TestQuestion q = examQuestions.get(questionIndex);

        Label question = label(
                "Q" + (questionIndex + 1) + ". " + q.text()
        );
        question.setWrapText(true);
        question.getStyleClass().add("exam-question");

        ToggleGroup group = new ToggleGroup();
        VBox options = new VBox(12);

        for (int i = 0; i < 4; i++) {
            final int selected = i;

            RadioButton radio = new RadioButton(
                    (char) ('A' + i) + ". " + q.options().get(i)
            );

            radio.setToggleGroup(group);
            radio.setWrapText(true);

            if (Objects.equals(answers.get(questionIndex), i)) {
                radio.setSelected(true);
            }

            radio.setOnAction(e -> answers.put(questionIndex, selected));
            options.getChildren().add(radio);
        }

        FlowPane palette = new FlowPane(8, 8);

        for (int i = 0; i < examQuestions.size(); i++) {
            final int target = i;

            Button number = button(String.valueOf(i + 1));
            if (answers.containsKey(i)) {
                number.setStyle(
                        "-fx-background-color: #16a34a; -fx-text-fill: white;"
                );
            }

            number.setOnAction(e -> {
                questionIndex = target;
                renderQuestion();
            });

            palette.getChildren().add(number);
        }

        Button previous = button("Previous");
        previous.setDisable(questionIndex == 0);
        previous.setOnAction(e -> {
            questionIndex--;
            renderQuestion();
        });

        Button next = button(
                questionIndex == examQuestions.size() - 1
                        ? "Submit Test" : "Next"
        );

        next.setOnAction(e -> {
            if (questionIndex == examQuestions.size() - 1) {
                confirmSubmit();
            } else {
                questionIndex++;
                renderQuestion();
            }
        });

        root.getChildren().addAll(
                clock, progress, bar, question, options,
                label("Question Navigation"), palette,
                new HBox(10, previous, next)
        );

        show(root, "Taking Test");

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds--;
            updateClock(clock);

            if (seconds <= 0) {
                submitTest(true);
            }
        }));

        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void updateClock(Label clock) {
        clock.setText(String.format(
                "TIME REMAINING: %02d:%02d",
                seconds / 60, seconds % 60
        ));
    }

    private void confirmSubmit() {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Are you sure you want to submit this test?",
                ButtonType.YES,
                ButtonType.NO
        );

        confirm.showAndWait().ifPresent(choice -> {
            if (choice == ButtonType.YES) submitTest(false);
        });
    }

    private void submitTest(boolean automatic) {
        if (submitted) return;
        submitted = true;

        if (timer != null) timer.stop();

        int correct = 0;

        for (var entry : answers.entrySet()) {
            if (examQuestions.get(entry.getKey()).correctIndex()
                    == entry.getValue()) {
                correct++;
            }
        }

        try {
            testStore.saveAttempt(
                    currentStudent,
                    currentTest,
                    examQuestions.size(),
                    answers.size(),
                    correct
            );
        } catch (Exception ex) {
            submitted = false;
            error(ex.getMessage());
            return;
        }

        double percentage =
                examQuestions.isEmpty() ? 0 :
                        correct * 100.0 / examQuestions.size();

        VBox root = page("TEST RESULT");

        Label status = label(percentage >= 40 ? "PASS" : "FAIL");
        status.getStyleClass().add(
                percentage >= 40 ? "pass-status" : "fail-status"
        );

        root.getChildren().addAll(
                status,
                label("Test: " + currentTest.title()),
                label("Student: " + currentStudent.name()),
                label("Marks: " + correct + "/" + examQuestions.size()),
                label("Attempted: " + answers.size()),
                label(String.format("Percentage: %.2f%%", percentage)),
                label(automatic ? "Automatically submitted - time expired"
                        : "Test submitted successfully.")
        );

        Button dashboard = button("Back to Dashboard");
        dashboard.setOnAction(e -> showStudentDashboard());

        root.getChildren().add(dashboard);
        show(root, "Test Result");
    }

    // ================= TABLE HELPERS =================

    private TableColumn<Student, String> studentColumn(
            String title,
            java.util.function.Function<Student, String> getter) {

        TableColumn<Student, String> column = new TableColumn<>(title);

        column.setCellValueFactory(data ->
                new SimpleStringProperty(getter.apply(data.getValue())));

        return column;
    }

    private TableColumn<ExamTest, String> testColumn(
            String title,
            java.util.function.Function<ExamTest, String> getter) {

        TableColumn<ExamTest, String> column = new TableColumn<>(title);

        column.setCellValueFactory(data ->
                new SimpleStringProperty(getter.apply(data.getValue())));

        return column;
    }

    private TableColumn<TestStore.TestAttempt, String> attemptColumn(
            String title,
            java.util.function.Function<TestStore.TestAttempt, String> getter) {

        TableColumn<TestStore.TestAttempt, String> column =
                new TableColumn<>(title);

        column.setCellValueFactory(data ->
                new SimpleStringProperty(getter.apply(data.getValue())));

        return column;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
