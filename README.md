# ExamPro — Smart Online Examination and Evaluation System

Java 21 + JavaFX + Maven desktop application. Uses local UTF-8 text files; no database or web server.

## Run
1. Install JDK 21 and Maven 3.9+.
2. Open this folder (the folder containing `pom.xml`) in PowerShell / VS Code.
3. Run: `mvn clean javafx:run`

## Roles
- Admin: select Admin on login; demo password `admin123`.
- Student: select Student, enter roll number and full name. Email and course are optional. First login registers the student; later logins with that roll number load the saved record.
- Admin Dashboard includes all registered student records, question bank management, and exam history/results.
- Students take a 10-minute MCQ exam; answers can be navigated and submitted, or auto-submitted when time expires.
- Pass threshold is 40%.

## Data files
Created automatically in `data/`:
- `students.txt` — all registered student details
- `questions.txt` — admin question bank
- `exam_history.txt` — saved exam results

Back up the `data` folder to preserve records. This is a local/demo authentication flow, not secure production authentication. Change the demo admin password before any real deployment.
