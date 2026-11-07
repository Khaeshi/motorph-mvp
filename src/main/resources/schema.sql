-- Recreated on every startup (spring.sql.init.mode=always), so salary edits made
-- through the demo reset when the app restarts. app_user is dropped first because
-- it references employee.
DROP TABLE IF EXISTS app_user;
DROP TABLE IF EXISTS employee;

CREATE TABLE employee (
    id           BIGINT        PRIMARY KEY,
    full_name    VARCHAR(100)  NOT NULL,
    job_title    VARCHAR(100)  NOT NULL,
    basic_salary DECIMAL(12,2) NOT NULL,
    allowance    DECIMAL(12,2) NOT NULL DEFAULT 0
);

CREATE TABLE app_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    employee_id   BIGINT       NULL,
    CONSTRAINT fk_app_user_employee FOREIGN KEY (employee_id) REFERENCES employee (id)
);
