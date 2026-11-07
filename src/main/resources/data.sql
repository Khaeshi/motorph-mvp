-- Passwords are BCrypt hashes, never plaintext.
-- Login as: admin_user / AdminPass123!
--           emp_user   / EmployeePass123!
-- Sample payroll data (replace names/amounts with your own if you like).

INSERT INTO employee (id, full_name, job_title, basic_salary, allowance) VALUES (10001, 'Admin User', 'HR Administrator', 45000.00, 3000.00);
INSERT INTO employee (id, full_name, job_title, basic_salary, allowance) VALUES (10002, 'Employee User', 'Payroll Analyst', 32000.00, 2000.00);
INSERT INTO employee (id, full_name, job_title, basic_salary, allowance) VALUES (10003, 'XSS Demo', 'Test Account', 28000.00, 1500.00);
INSERT INTO employee (id, full_name, job_title, basic_salary, allowance) VALUES (10004, 'Juan Dela Cruz', 'Sales Associate', 25000.00, 1500.00);

INSERT INTO app_user (username, password_hash, role, employee_id)
VALUES ('admin_user', '$2b$10$49OgP0fwR.VkFjgeganDEup2qczTclDmJPiWdtdfbX.Lj9qn9Hs.O', 'ADMIN', 10001)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);

INSERT INTO app_user (username, password_hash, role, employee_id)
VALUES ('emp_user', '$2b$10$0AYWXR3lNOh3RfAyRCIQye/qJbjOKJuZwejkgUKPOt2Si/Ioc6LpS', 'EMPLOYEE', 10002)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);

-- R-07 demo: username contains HTML markup to show output encoding.
INSERT INTO app_user (username, password_hash, role, employee_id)
VALUES ('xss<b>demo</b>', '$2b$10$0AYWXR3lNOh3RfAyRCIQye/qJbjOKJuZwejkgUKPOt2Si/Ioc6LpS', 'EMPLOYEE', 10003)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);
