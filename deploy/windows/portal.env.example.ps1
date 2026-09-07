# Secrets for the Windows host. Copy to C:\exhibition-portal\portal.env.ps1 and edit.
# Do not commit portal.env.ps1.

$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:SERVER_PORT = '80'
$env:DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3306/exhibition_portal?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8'
$env:DATASOURCE_USERNAME = 'exhibition'
$env:DATASOURCE_PASSWORD = 'change-me-db'
# Required in prod. Must NOT be poc-staff or change-me-staff — app refuses to start.
$env:EXHIBITION_STAFF_BOOTSTRAP_PASSWORD = 'change-me-staff'
$env:EXHIBITION_STORAGE_ROOT = 'C:\exhibition-portal\files'
# Optional until live CRM/vendor APIs are chosen (local file outbox stubs):
# $env:EXHIBITION_OUTBOX_MARKETING = 'local-mailbox'
# $env:EXHIBITION_OUTBOX_VENDOR = 'local-vendor-stub'
# Buyer finished goods — weekly DB sync from pharma-erp (manual sync via Staff first):
# $env:EXHIBITION_PHARMA_ERP_ENABLED = 'true'
# $env:EXHIBITION_PHARMA_ERP_JDBC_URL = 'jdbc:mysql://127.0.0.1:3306/pharmadb?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8'
# $env:EXHIBITION_PHARMA_ERP_USERNAME = 'root'
# $env:EXHIBITION_PHARMA_ERP_PASSWORD = 'your-pharma-db-password'
# $env:EXHIBITION_PHARMA_ERP_SCHEDULE = 'false'   # set true later for weekly cron
