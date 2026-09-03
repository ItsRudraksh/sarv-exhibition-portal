-- Native MySQL 8 on the Windows Server (no Docker). Same engine as pharma-erp.
-- `mysql` is often not on PATH. Use the full client path from an elevated PowerShell.
-- Do not run this from C:\exhibition-portal-staging unless init-mysql.sql was copied there.
-- Repo checkout:
--   Get-Content deploy\windows\init-mysql.sql | & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' -u root -p
-- Install dir (after Jenkins copies this file):
--   Get-Content C:\exhibition-portal-staging\init-mysql.sql | & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' -u root -p
-- Initial password matches local application.yml (`exhibition`). Change it on the public host:
--   ALTER USER 'exhibition'@'localhost' IDENTIFIED BY 'your-db-password';
--   ALTER USER 'exhibition'@'127.0.0.1' IDENTIFIED BY 'your-db-password';

CREATE DATABASE IF NOT EXISTS exhibition_portal
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'exhibition'@'localhost' IDENTIFIED BY 'exhibition';
CREATE USER IF NOT EXISTS 'exhibition'@'127.0.0.1' IDENTIFIED BY 'exhibition';

GRANT ALL PRIVILEGES ON exhibition_portal.* TO 'exhibition'@'localhost';
GRANT ALL PRIVILEGES ON exhibition_portal.* TO 'exhibition'@'127.0.0.1';
FLUSH PRIVILEGES;
