# Online Exam/Quiz System: Requirements Specification (frozen)

This file is the **single source of truth** for the code, the JUnit tests and the final PDF report.
If a rule or message changes, change it here **and** in `ValidationRules` / `Messages` in the same commit.

## 1. Validation rules (used for EP and BVA)

| Item | Valid range | BVA values to test |
|---|---|---|
| Password length | 8–20, at least 1 letter **and** 1 digit | 7, 8, 9, 19, 20, 21 |
| Quiz title length | 3–100 characters (after trimming) | 2, 3, 4, 99, 100, 101 |
| Quiz duration | 5–180 minutes | 4, 5, 6, 179, 180, 181 |
| Passing score | 0–100 % | -1, 0, 1, 99, 100, 101 |
| Max attempts | 1–5 | 0, 1, 2, 4, 5, 6 |
| Options per question | 2–5, exactly 1 correct | 1, 2, 3, 4, 5, 6 options; 0 / 1 / 2 correct |
| Question points | 1–10 | 0, 1, 2, 9, 10, 11 |
| Failed logins before lock | 3 | 2nd failure (still open), 3rd failure (locked) |

## 2. Message catalogue (exact strings; tests assert on these)

| Code | Message | Used by |
|---|---|---|
| REGISTER_OK | `Registration successful` | R1 |
| NAME_REQUIRED | `Name is required` | R1 |
| INVALID_EMAIL | `Invalid email` | R1 |
| INVALID_PASSWORD | `Password must be 8-20 characters with a letter and a digit` | R2 |
| EMAIL_TAKEN | `Email already registered` | R3 |
| BAD_CREDENTIALS | `Invalid email or password` | R4 |
| ACCOUNT_LOCKED | `Account locked` | R5 |
| INVALID_TITLE | `Invalid title length` | R7 |
| INVALID_DURATION | `Duration must be 5-180 minutes` | R8 |
| INVALID_ATTEMPTS | `Max attempts must be 1-5` | R9 |
| INVALID_PASSING_SCORE | `Passing score must be 0-100` | R9 |
| INVALID_QUESTION | `Invalid question configuration` | R11 |
| QUIZ_NEEDS_QUESTION | `Quiz needs at least 1 question` | R12 |
| QUIZ_LOCKED | `Quiz cannot be modified after attempts exist` | R13 |
| NO_ATTEMPTS_LEFT | `No attempts remaining` | R15 |
| QUIZ_NOT_AVAILABLE | `Quiz not available` | R16 |
| TIME_EXPIRED | `Time is up - attempt already submitted` | R17 |
| QUIZ_ALREADY_PUBLISHED | `Quiz is already published` | R12 |
| QUIZ_NOT_FOUND | `Quiz not found` | R6–R13 (missing or another instructor's quiz) |
| QUESTION_NOT_FOUND | `Question not found` | R10–R13 |
| ATTEMPT_NOT_FOUND | `Attempt not found` | R14–R17 (missing or another student's attempt) |
| INVALID_ANSWER | `Invalid answer` | R17 (question/option not part of this quiz) |

## 3. Clarifications (decisions that make the requirements testable)

- **R1**: Name must not be blank. Email must match `local@domain.tld`. Emails are compared case-insensitively and stored in lower case. New accounts always get role `STUDENT`. Instructors are seeded.
- **R2**: Length is checked first, then the letter and digit rule. `12345678` (no letter) and `abcdefgh` (no digit) are both invalid. A `null` password is invalid.
- **R3**: The check runs after R1/R2 validation succeeds.
- **R4**: A wrong email or wrong password gives the same message (`Invalid email or password`). A successful login resets the failed-login counter to 0. STUDENT → student dashboard, INSTRUCTOR → instructor dashboard.
- **R5**: The 3rd consecutive wrong password locks the account. A locked account rejects even the correct password with `Account locked`.
- **R6–R9**: Validation order is title → duration → passing score → max attempts. The first failure is reported. A missing number counts as out of range. A new quiz always starts as `DRAFT`. An instructor can only see and change their own quizzes (otherwise `Quiz not found`).
- **R10/R11**: A question is valid when its text is not blank, points are 1–10, it has 2–5 options with non-blank text, and exactly 1 option is correct. Anything else gives `Invalid question configuration`.
- **R12**: Publishing an already-PUBLISHED quiz is rejected with `Quiz is already published`. A PUBLISHED quiz cannot lose its last question (`Quiz needs at least 1 question`).
- **R13**: Questions can be added, edited or deleted (DRAFT or PUBLISHED) until the first attempt exists. After that every change is rejected with `Quiz cannot be modified after attempts exist`. This check runs before question validation.
- **R14**: Allowed when `attemptsUsed < maxAttempts`. `attemptsUsed` counts all of the student's attempts on that quiz (in progress and submitted). The attempt stores `startTime` from the system clock. If the student already has an unexpired attempt in progress, "start" resumes it instead of creating a new one.
- **R15/R16**: Check order is quiz published (R16), then resume an open attempt, then the attempt limit (R15). A quiz that does not exist is also `Quiz not available`.
- **R17**: An attempt is expired when `now >= startTime + duration` (exactly at the limit counts as expired). Each answer is saved as soon as it is chosen. Expiry is checked whenever the attempt is used (opened, answered, submitted, or the quiz is started again). An expired attempt is auto-submitted with the answers saved so far, and its submit time is set to the deadline. Answers to a submitted attempt are rejected with `Time is up - attempt already submitted`. A question or option that does not belong to the attempt's quiz gives `Invalid answer`. A student can only use their own attempts (otherwise `Attempt not found`).
- **R18**: `score = earnedPoints / totalPoints × 100`, rounded half-up to 2 decimals (decimal arithmetic, no floating point). A question earns its points only when the selected option is the correct one. Unanswered questions earn 0. The score is calculated from the saved answers of a submitted attempt. Invalid input (total ≤ 0, earned < 0 or earned > total) is a programming error (`IllegalArgumentException`).
- **R19**: `PASS` if `score >= passingScore` (the quiz's own passing score), else `FAIL`. Letter grade from the rounded score: A ≥ 90, B ≥ 80, C ≥ 70, D ≥ 60, otherwise F. Scores outside 0–100 are rejected (`IllegalArgumentException`). BVA: 59.99 / 60, 69.99 / 70, 79.99 / 80, 89.99 / 90, plus 0 and 100.
- **R20**: Only submitted attempts count. Opening the report first auto-submits attempts whose time is up (R17). A student's reported score is their highest attempt. Students with no submitted attempt are not listed. Class average = mean of the listed students' best scores, rounded half-up to 2 decimals. With no students the average is 0.00.

## 4. Requirements (Gherkin)

### Account and access

**R1: Register**
```gherkin
Given a user provides name "Alice Smith", email "alice@uni.edu", and password "Secure123"
When the registration request is submitted
Then the system creates an account with role "STUDENT"
And the message "Registration successful" is shown
```

**R2: Password validation**
```gherkin
Given a user provides a password of length 7
When the registration request is submitted
Then the system rejects it with "Password must be 8-20 characters with a letter and a digit"
```

**R3: Duplicate email**
```gherkin
Given an account with email "alice@uni.edu" already exists
When a new user registers with the same email
Then the system rejects it with "Email already registered"
```

**R4: Login**
```gherkin
Given a registered user with correct credentials
When the user logs in
Then the system opens the dashboard for the user's role
```

**R5: Account lock**
```gherkin
Given a user has entered a wrong password 2 times
When the user enters a wrong password a 3rd time
Then the account is locked and "Account locked" is shown
```

### Quiz creation (instructor)

**R6: Create quiz**
```gherkin
Given an instructor enters title "Java Basics", duration 30, passing score 60, and max attempts 2
When the quiz is saved
Then the quiz is created with status "DRAFT"
```

**R7: Title validation**
```gherkin
Given an instructor enters a title shorter than 3 or longer than 100 characters
When the quiz is saved
Then the system rejects it with "Invalid title length"
```

**R8: Duration validation**
```gherkin
Given an instructor enters a duration outside 5-180 minutes
When the quiz is saved
Then the system rejects it with "Duration must be 5-180 minutes"
```

**R9: Attempts and passing score validation**
```gherkin
Given an instructor enters max attempts outside 1-5 or passing score outside 0-100
When the quiz is saved
Then the system rejects it with "Max attempts must be 1-5" or "Passing score must be 0-100" respectively
```

**R10: Add question**
```gherkin
Given a DRAFT quiz and a question with 4 options, 1 correct answer, and 5 points
When the instructor adds the question
Then the question is added to the quiz
```

**R11: Question validation**
```gherkin
Given a question has fewer than 2 or more than 5 options, no correct answer, more than one correct answer, or points outside 1-10
When the instructor adds it
Then the system rejects it with "Invalid question configuration"
```

**R12: Publish quiz**
```gherkin
Given a DRAFT quiz with at least 1 question
When the instructor publishes it
Then the status becomes "PUBLISHED"
And a quiz with 0 questions is rejected with "Quiz needs at least 1 question"
```

**R13: Lock editing**
```gherkin
Given a student has already attempted a quiz
When the instructor tries to edit its questions
Then the system rejects it with "Quiz cannot be modified after attempts exist"
```

### Taking a quiz (student)

**R14: Start attempt**
```gherkin
Given a PUBLISHED quiz and a student with attempts used less than max attempts
When the student starts the quiz
Then a new attempt is created and the timer starts
```

**R15: Attempt limit**
```gherkin
Given a student has used all allowed attempts
When the student tries to start again
Then the system rejects it with "No attempts remaining"
```

**R16: Unpublished quiz**
```gherkin
Given a quiz with status "DRAFT"
When a student tries to start it
Then the system rejects it with "Quiz not available"
```

**R17: Timer expiry**
```gherkin
Given an attempt has been running for the full duration
When the time expires
Then the attempt is auto-submitted with the answers saved so far
And any later answer submission is rejected with "Time is up - attempt already submitted"
```

### Grading and results

**R18: Score calculation**
```gherkin
Given a student answered questions worth 15 of 20 total points correctly
When the attempt is submitted
Then the score is 75 %
```

**R19: Pass/fail and letter grade**
```gherkin
Given a score and the quiz passing score of 60
When the result is generated
Then the result is "PASS" if score >= 60, otherwise "FAIL"
And the letter grade is A (>=90), B (80-89), C (70-79), D (60-69), or F (<60)
```

**R20: Best attempt and statistics**
```gherkin
Given a student has multiple attempts on a quiz
When the instructor views the report
Then the student's highest score is reported
And the class average is calculated from all students' best scores
```

## 5. Requirement → function map

| Req | Function(s) under test |
|---|---|
| R1–R3 | `AuthService.register`, `PasswordValidator.validate` |
| R4–R5 | `AuthService.login` |
| R6–R9 | `QuizService.createQuiz`, `QuizValidator.validate` |
| R10–R11 | `QuizService.addQuestion`, `QuestionValidator.validate` |
| R12–R13 | `QuizService.publish`, `QuizService.editQuestion`, `QuizService.deleteQuestion` |
| R14–R17 | `AttemptService.start`, `AttemptService.submitAnswer`, `AttemptService.submit`, `AttemptService.checkExpiry` |
| R18–R19 | `GradingService.calculateScore`, `GradingService.passFail`, `GradingService.letterGrade`, `Attempt.getEarnedPoints` |
| R20 | `ReportService.bestScore`, `ReportService.classAverage`, `ReportService.quizReport` |
