-- Passwords are BCrypt hashes, never plaintext (contrast this with the
-- legacy CSV app, which stored the password in cleartext).
-- Login as: admin_user / AdminPass123!
--           emp_user   / EmployeePass123!

INSERT INTO app_user (username, password_hash, role)
VALUES ('admin_user', '$2b$10$49OgP0fwR.VkFjgeganDEup2qczTclDmJPiWdtdfbX.Lj9qn9Hs.O', 'ADMIN')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);

INSERT INTO app_user (username, password_hash, role)
VALUES ('emp_user', '$2b$10$0AYWXR3lNOh3RfAyRCIQye/qJbjOKJuZwejkgUKPOt2Si/Ioc6LpS', 'EMPLOYEE')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);
