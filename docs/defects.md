# Defects found by testing

| ID | Found by | Requirement | Defect | Fix |
|---|---|---|---|---|
| D-01 | TC-R01-BVA-04 (2-BVA, name length = 101) | R1 | Registering with a 101-character name crashed with a database error ("Value too long for column NAME") instead of showing a validation message. The spec had no maximum name length. | Rule added: name 1–100 characters. `AccountValidator.validateName` rejects longer names with "Name must be at most 100 characters". |
| D-02 | TC-R01-BVA-06 (2-BVA, email length = 101) | R1 | A 101-character email was accepted; there was no maximum length. | Rule added: email at most 100 characters. `AccountValidator.validateEmail` rejects longer emails with "Invalid email". |
| D-03 | TC-R10-BVA-04 (2-BVA, question text length = 501) | R10/R11 | Adding a question with 501-character text crashed with a database error ("value too long") instead of a validation message. The spec had no maximum question length. | Rule added: question text 1–500 characters. `QuestionValidator` rejects longer text with "Invalid question configuration". |
| D-04 | TC-R10-BVA-06 (2-BVA, option text length = 301) | R10/R11 | An answer option with 301 characters crashed with a database error. The spec had no maximum option length. | Rule added: option text 1–300 characters. `QuestionValidator` rejects longer options with "Invalid question configuration". |
