
## Registration Debugging

For synthetic customer registration:

1. Generate a unique username using the current run ID
   and a random suffix.
2. Record the exact username in TEST_REPORT.md.
3. Never record passwords, SSNs or authentication tokens.
4. Before submission, verify all required fields are filled
   and the password matches its confirmation.
5. Submit the form and inspect every validation message.
6. If "This username already exists" appears, verify that
   the username differs from all previous attempts.
7. Record the exact rejected username and validation message.
8. If the submission was unambiguously rejected, one retry
   with a different username is permitted.
9. Do not claim the username is unique without verifying
   its generation.
   For generated registration usernames:
- Use only ASCII letters and digits (A-Z, a-z, 0-9).
- Keep the username between 12 and 20 characters.
- Do not use underscores, hyphens, spaces or special characters.
- Include a short random alphanumeric suffix for uniqueness.
- Record the attempted username and the exact response.