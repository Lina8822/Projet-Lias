# GitHub publication review â€” 24 October 2026

Status: prepared for user review; no new commit or push. Destination: https://github.com/Lina8822/Projet-Lias (public, confirmed by GitHub REST API).

## Scope and preserved behavior

No application business code, servlet, DAO, database connection implementation, schema migration, JSP, or dependency was changed for this preparation. Existing MySQL/MariaDB and XAMPP defaults remain intact. All source files will be added on top of the existing `main` commit `29e21d841aa5ba92c00e9ea10a5edfb8ce812b3b`; no history rewrite or force push is planned.

## Changes to review

- `tools/CreateTestAccounts.java` and `tools/SeedIsolated.java`: require `LIAS_TEST_PASSWORD` and `LIAS_TEST_DIRECTOR_PASSWORD`, minimum 12 characters, instead of built-in fixture passwords. Existing guards, transactions and refusal to reset accounts are retained.
- `tools/test_environment.py`: shared Python password reader; missing or short passwords fail with the variable name, without printing the value.
- `tools/final_http.py`, `tools/final_security.py`, `tools/adhesion_http.py`, `tools/http_test.py`, `tools/http_mutations.py`: read fixture passwords from that environment helper; scenarios and assertions remain intact.
- `tools/verify-launch.ps1`: reads and validates `LIAS_TEST_PASSWORD`.
- `tools/VerifyRealAuthentication.java`: uses its existing environment-provided password for the negative isolated-account check instead of a built-in password.
- `src/test/java/com/lias/IntegrationTest.java`: persistent fixture and candidate passwords come from `LIAS_TEST_PASSWORD`; business assertions remain intact. Fixed strings used only for password-algorithm assertions are synthetic test inputs, not deployed credentials.
- `docs/authentification-base-reelle.md` and `tools/update_verification_report.py`: remove old plaintext fixture passwords, including text that could otherwise regenerate them in documentation.
- `.env.example` and `README.md`: document local MySQL/XAMPP settings, test variables and private interactive password entry. The example has no actual password or JWT key. Java/Tomcat do not automatically load `.env`; configure the server process environment.
- `.gitignore`: excludes local environment variants (while allowing `.env.example`), local PowerShell configuration, generated account files, private key files and the original archive.
- `docs/source-original.zip`: retained locally but removed from the proposed upload because it embeds an old hardcoded JWT signing key. Current application sources are all included.

## Credential and history audit

GitHub advertises only `main`, pointing to the original README-only commit. All reachable local/remote commits were inspected: no credentials were committed in that history. The rejected earlier push never created a project commit. No history rewrite or GitHub-related credential rotation is required for this repository based on the checked history.

The staged upload was scanned for the removed fixture values, the old archive JWT key, private keys and common GitHub/OpenAI/AWS token formats, with manual review of password/secret references. No sensitive credential match remains in the upload. This is a scoped audit, not a guarantee of detecting every possible unknown secret format.

Old test credentials did exist in working files, including a documented test director account used in a development database. Change those passwords if those accounts still exist, especially before using the database beyond a private local environment. No existing account was reset during this work. Replace the archived JWT key and invalidate tokens if any deployment ever used it; the current application already uses an environment-provided or freshly generated key. The archive database password is empty, not a nonempty secret.

Ignored local files still include the original archive, test data and generated private fixture credentials. Previously staged blobs can also remain as unreachable local Git objects; they were never committed and are not part of the history or proposed push. No unrelated files from the parent user-directory Git repository are included.

## Verification

- Maven clean/test/package: PASS, WAR generated; 23 JUnit tests, zero failures/errors/skips, including all eight JDBC tests.
- Existing Java integration runner: PASS, 36 assertions.
- All Java maintenance tools: compiled successfully.
- Python tools: syntax checked; missing/short test-password rejection checked.
- Application JDBC: unchanged `DBConnection.getConnection()` connected successfully and executed read-only SQL against XAMPP MariaDB 10.4.32 on port 33307.
- HTTP verification: PASS, all 19 scenario scripts (664 passing checks), covering permissions, lifecycle, applications, publications, communications, materials, governance, history, collaboration, PDF contents, security and profiles. Logs: `build/publish-review/`.

Two fresh schemas, `lias_test_publish_review` and `lias_test_publish_integration`, were created from the isolated test schema without copying application data. Tests used fresh random private fixture passwords. The real `lias_data_base` was not modified. Its normal port 3306 refused connections during review, so production/Eclipse database connectivity is not claimed.

Tomcat 11.0.22 and Java 26.0.1 were used for HTTP verification. This machine's default NIO connector failed during loopback initialization; the temporary server used Tomcat's NIO2 connector. This change exists only in ignored test infrastructure and is not an application/deployment change. The PDF verifier used the bundled Python runtime because the system Python lacks `pypdf`.

Approval is required before creating the publication commit and pushing `main`.
