# MotorPH IAS 2 MVP

A minimal Spring Boot app implementing 4 controls from the IAS 1 design,
scoped small enough to actually finish and test: R-01 (SQLi hardening),
R-03 (RBAC), R-06 (session hardening), R-08 (audit logging).

## 1. VS Code setup

1. Install the **Extension Pack for Java** and **Spring Boot Extension Pack**
   (both from Microsoft/VMware, search in the Extensions panel).
2. Open this folder in VS Code (`File > Open Folder`).
3. VS Code should auto-detect the Maven project. If it asks to import, click Yes.

## 2. Database setup

You need MySQL running locally (MySQL Workbench, XAMPP, or the MySQL service — whatever your group already uses).

```sql
CREATE DATABASE motorph_security;
```

Then edit `src/main/resources/application.properties`:
- `spring.datasource.username` — your MySQL username
- `spring.datasource.password` — your MySQL password

`schema.sql` and `data.sql` run automatically on startup and create the
`app_user` table with two seed accounts:

| username     | password           | role     |
|--------------|--------------------|----------|
| admin_user   | AdminPass123!      | ADMIN    |
| emp_user     | EmployeePass123!   | EMPLOYEE |

## 3. Run it

In the VS Code terminal (you'll need Maven installed — `mvn -v` to check,
or install via your OS package manager / https://maven.apache.org):

```
mvn spring-boot:run
```

Alternatively, once the Java extension has indexed the project, you can
just open `MotorphMvpApplication.java` and click the "Run" button VS Code
shows above the `main` method.

Visit `http://localhost:8080` — you should see a plain text welcome message.

**Important:** `server.servlet.session.cookie.secure=true` in
`application.properties` means the session cookie only gets sent over
HTTPS. Over plain `http://localhost` your browser may just silently not
send it back, which will look like login isn't "sticking." For local
testing, temporarily set this to `false`, note that you did so, and put
it back to `true` before treating R-06 as validated — real deployment
must be HTTPS anyway, and testing a `secure=true` cookie under plain HTTP
would give you a false "it doesn't work" result.

## 4. Testing each control (maps directly to your audit worksheet)

**R-01 — SQL Injection**
Open `src/main/java/com/motorph/dao/UserDao.java` — this is your
evidence: PreparedStatement with a bound `?` parameter, never string
concatenation. Try logging in with a username like `' OR '1'='1` — it
will fail, because it's treated as a literal string value, not SQL.

**R-03 — RBAC**
Log in as `emp_user` and try visiting `http://localhost:8080/admin/dashboard`
directly — Spring Security should return 403 Forbidden. Log in as
`admin_user` and it should work. That 403 response is your evidence.

**R-06 — Session Management**
After logging in, open DevTools (F12) → Application → Cookies →
`localhost` and check the `JSESSIONID` cookie's flags (HttpOnly, SameSite).
Leave the app idle for 15+ minutes, then try visiting `/employee/dashboard`
again — you should get redirected to log in again.

**R-08 — Audit Logging**
After a login attempt (success or failure), check `logs/audit.log` in
the project root. Each line is a structured entry:
`action=LOGIN_ATTEMPT actor=admin_user result=USER_FOUND role=ADMIN timestamp=...`

## 5. Once this MVP works

This deliberately does NOT implement R-02, R-04, R-05, R-07 yet — expand
into those the same way: small, testable, one control at a time, rather
than building the full payroll system up front.
