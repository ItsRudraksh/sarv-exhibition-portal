# Secrets for the Windows host. Copy to C:\exhibition-portal\portal.env.ps1 and edit.
# Do not commit portal.env.ps1.

$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:SERVER_PORT = '80'
$env:DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3306/exhibition_portal?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8'
$env:DATASOURCE_USERNAME = 'exhibition'
$env:DATASOURCE_PASSWORD = 'change-me-db'
$env:EXHIBITION_STAFF_BOOTSTRAP_PASSWORD = 'change-me-staff'
$env:EXHIBITION_STORAGE_ROOT = 'C:\exhibition-portal\files'
