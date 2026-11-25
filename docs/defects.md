# Defects found by testing

| ID | Found by | Requirement | Defect | Fix |
|---|---|---|---|---|
| D-01 | TC-R01-BVA-04 (2-BVA, name length = 101) | R1 | Registering with a 101-character name crashed with a database error ("Value too long for column NAME") instead of showing a validation message. The spec had no maximum name length. | Rule added: name 1–100 characters. `AccountValidator.validateName` rejects longer names with "Name must be at most 100 characters". |
| D-02 | TC-R01-BVA-06 (2-BVA, email length = 101) | R1 | A 101-character email was accepted; there was no maximum length. | Rule added: email at most 100 characters. `AccountValidator.validateEmail` rejects longer emails with "Invalid email". |
