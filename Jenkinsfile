# Jenkins: same agent pattern as pharma-erp (Java17 + Maven3 tools).
# Windows Server at http://43.225.195.200/ — native PostgreSQL, no Docker, no ZK credentials.
# Node 22+ must be on the agent PATH (npm ci / npm run build). JDK must be 17 (server has 17.0.18).

pipeline {

    agent any

    tools {
        jdk 'Java17'
        maven 'Maven3'
    }

    parameters {
        booleanParam(name: 'SKIP_RUN_VALIDATE', defaultValue: false,
            description: 'Skip Run (Smoke) and Validate stages (Unix only).')
        booleanParam(name: 'SKIP_MAVEN_BUILD', defaultValue: false,
            description: 'false (default): npm + mvn on this agent; Package produces backend/target/exhibition-portal.jar then deploy uses it. true: skip build (agent must have a JAR under backend/target/ or set JAR_SOURCE).')
        string(name: 'JAR_SOURCE', defaultValue: '',
            description: 'Optional: absolute path to JAR on the agent. If empty, deploy uses exactly one backend/target/*.jar from this job workspace (not *.original).')
    }

    environment {
        APP_DIR = 'backend'
        SERVICE_NAME = 'exhibition-portal'
        STAGING_SERVICE = 'exhibition-portal-staging'
        APP_PORT = '80'
        SMOKE_PORT = '18080'
        STAGING_DIR = 'C:\\exhibition-portal-staging'
        PROD_DIR = 'C:\\exhibition-portal'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Frontend') {
            when { expression { return !params.SKIP_MAVEN_BUILD } }
            steps {
                dir('frontend') {
                    script {
                        if (isUnix()) {
                            sh 'npm ci'
                            sh 'npm run build'
                        } else {
                            bat 'npm ci'
                            bat 'npm run build'
                        }
                    }
                }
            }
        }

        stage('Maven (compile, test, package)') {
            when { expression { return !params.SKIP_MAVEN_BUILD } }
            environment {
                MAVEN_OPTS = '-XX:+TieredCompilation -XX:TieredStopAtLevel=1'
            }
            steps {
                dir("${APP_DIR}") {
                    script {
                        if (isUnix()) {
                            sh 'mvn -B clean compile'
                            sh 'mvn -B test'
                            sh 'mvn -B package -DskipTests'
                        } else {
                            bat 'mvn -B clean compile'
                            bat 'mvn -B test'
                            bat 'mvn -B package -DskipTests'
                        }
                    }
                }
            }
        }

        stage('Run (Smoke)') {
            when { expression { return !params.SKIP_MAVEN_BUILD && !params.SKIP_RUN_VALIDATE && isUnix() } }
            environment {
                MAVEN_OPTS = '-XX:+TieredCompilation -XX:TieredStopAtLevel=1'
            }
            steps {
                dir("${APP_DIR}") {
                    sh '''#!/usr/bin/env bash
                        set -eu
                        rm -f smoke-run.log
                        nohup java -Xms256m -Xmx512m -Dserver.port=''' + env.SMOKE_PORT + ''' -XX:+TieredCompilation -XX:TieredStopAtLevel=1 \\
                            -jar target/exhibition-portal.jar --spring.profiles.active=ci >> smoke-run.log 2>&1 &
                        echo $! > app.pid
                        sleep 3
                        tail -40 smoke-run.log || true
                    '''
                    sh """
                        for i in \$(seq 1 60); do
                            code=\$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${env.SMOKE_PORT}/actuator/health" || true)
                            if curl -sf "http://localhost:${env.SMOKE_PORT}/actuator/health" > /dev/null 2>&1; then
                                echo "App is up after \${i}s"
                                exit 0
                            fi
                            echo "wait attempt \$i: health HTTP \$code"
                            sleep 2
                        done
                        echo "App did not become ready in time — last smoke-run.log:"
                        tail -120 smoke-run.log || true
                        exit 1
                    """
                }
            }
        }

        stage('Validate') {
            when { expression { return !params.SKIP_MAVEN_BUILD && !params.SKIP_RUN_VALIDATE && isUnix() } }
            steps {
                sh """
                    echo "=== Actuator health ==="
                    curl -sS http://localhost:${env.SMOKE_PORT}/actuator/health | head -20
                    echo ""
                    echo "=== Visitor shell (HTTP code) ==="
                    curl -sS -o /dev/null -w "%{http_code}\\n" http://localhost:${env.SMOKE_PORT}/ || true
                """
            }
        }

        stage('Deploy to Staging') {
            when { branch 'dev' }
            steps {
                script {
                    if (isUnix()) {
                        echo 'Skipping staging deployment on Linux'
                    } else {
                        powershell """
                \$jarSource = "${params.JAR_SOURCE}".Trim()
                \$appTarget = "${APP_DIR}\\target"
                \$destJar = "${STAGING_DIR}\\exhibition-portal.jar"
                \$service = "${STAGING_SERVICE}"

                Write-Host "Stopping service if running..."
                \$svc = Get-Service -Name \$service -ErrorAction SilentlyContinue
                if (\$svc -and \$svc.Status -eq 'Running') {
                    net stop \$service
                } elseif (-not \$svc) {
                    Write-Error "Windows service \$service is not installed. Run deploy\\windows\\install-service.ps1 once."
                    exit 1
                }

                Write-Host "Resolving JAR to deploy (SKIP_MAVEN_BUILD=${params.SKIP_MAVEN_BUILD})..."
                \$src = \$null
                if ([string]::IsNullOrEmpty(\$jarSource)) {
                    \$matches = @(Get-ChildItem -Path "\$appTarget" -File -ErrorAction SilentlyContinue |
                        Where-Object { \$_.Name -like 'exhibition-portal*.jar' -and \$_.Name -notlike '*.original' })
                    if (\$matches.Count -eq 0) {
                        Write-Error ("No JAR under " + \$appTarget + ". Options: (1) SKIP_MAVEN_BUILD=false, (2) Copy exhibition-portal.jar into " + \$appTarget + "\\, (3) Set JAR_SOURCE.")
                        exit 1
                    }
                    if (\$matches.Count -gt 1) {
                        Write-Error ("Multiple JAR files under " + \$appTarget + "; keep one or set JAR_SOURCE.")
                        exit 1
                    }
                    \$src = \$matches[0].FullName
                    Write-Host "Using workspace target JAR: \$src"
                } else {
                    if (-not (Test-Path -LiteralPath \$jarSource -PathType Leaf)) {
                        Write-Error "JAR_SOURCE path not found or not a file: \$jarSource"
                        exit 1
                    }
                    \$src = \$jarSource
                    Write-Host "Using JAR_SOURCE: \$src"
                }
                \$info = Get-Item -LiteralPath \$src
                Write-Host ("Copying JAR - LastWriteTime: {0}, Length: {1} bytes" -f \$info.LastWriteTime, \$info.Length)
                New-Item -ItemType Directory -Force -Path "${STAGING_DIR}" | Out-Null
                Copy-Item -LiteralPath \$src -Destination \$destJar -Force

                Write-Host "Starting service..."
                net start \$service

                Write-Host "Waiting for startup..."
                Start-Sleep -Seconds 20
            """
                    }
                }
            }
        }

        stage('Deploy to Production') {
            when { branch 'main' }
            steps {
                script {
                    if (isUnix()) {
                        echo 'Production for this app is Windows Server. Skipping Unix deploy.'
                    } else {
                        powershell """
                \$jarSource = "${params.JAR_SOURCE}".Trim()
                \$appTarget = "${APP_DIR}\\target"
                \$destJar = "${PROD_DIR}\\exhibition-portal.jar"
                \$service = "${SERVICE_NAME}"

                Write-Host "Stopping service if running..."
                \$svc = Get-Service -Name \$service -ErrorAction SilentlyContinue
                if (\$svc -and \$svc.Status -eq 'Running') {
                    net stop \$service
                } elseif (-not \$svc) {
                    Write-Error "Windows service \$service is not installed. Run deploy\\windows\\install-service.ps1 once."
                    exit 1
                }

                Write-Host "Resolving JAR to deploy (SKIP_MAVEN_BUILD=${params.SKIP_MAVEN_BUILD})..."
                \$src = \$null
                if ([string]::IsNullOrEmpty(\$jarSource)) {
                    \$matches = @(Get-ChildItem -Path "\$appTarget" -File -ErrorAction SilentlyContinue |
                        Where-Object { \$_.Name -like 'exhibition-portal*.jar' -and \$_.Name -notlike '*.original' })
                    if (\$matches.Count -eq 0) {
                        Write-Error ("No JAR under " + \$appTarget + ". Options: (1) SKIP_MAVEN_BUILD=false, (2) Copy exhibition-portal.jar into " + \$appTarget + "\\, (3) Set JAR_SOURCE.")
                        exit 1
                    }
                    if (\$matches.Count -gt 1) {
                        Write-Error ("Multiple JAR files under " + \$appTarget + "; keep one or set JAR_SOURCE.")
                        exit 1
                    }
                    \$src = \$matches[0].FullName
                    Write-Host "Using workspace target JAR: \$src"
                } else {
                    if (-not (Test-Path -LiteralPath \$jarSource -PathType Leaf)) {
                        Write-Error "JAR_SOURCE path not found or not a file: \$jarSource"
                        exit 1
                    }
                    \$src = \$jarSource
                    Write-Host "Using JAR_SOURCE: \$src"
                }
                \$info = Get-Item -LiteralPath \$src
                Write-Host ("Copying JAR - LastWriteTime: {0}, Length: {1} bytes" -f \$info.LastWriteTime, \$info.Length)
                New-Item -ItemType Directory -Force -Path "${PROD_DIR}" | Out-Null
                Copy-Item -LiteralPath \$src -Destination \$destJar -Force

                Write-Host "Starting service..."
                net start \$service

                Write-Host "Waiting for startup..."
                Start-Sleep -Seconds 20
            """
                    }
                }
            }
        }

        stage('Health Check') {
            when { branch 'main' }
            steps {
                script {
                    if (isUnix()) {
                        echo 'Health check is Windows (public host). Skipping Unix.'
                    } else {
                        powershell """
                \$ok = \$false
                for (\$i = 1; \$i -le 48; \$i++) {
                    try {
                        \$r = Invoke-WebRequest -Uri "http://127.0.0.1/actuator/health" -UseBasicParsing -TimeoutSec 5
                        if (\$r.StatusCode -eq 200) {
                            Write-Host "Production actuator HTTP 200 after attempt \$i"
                            Write-Host \$r.Content
                            \$ok = \$true
                            break
                        }
                        Write-Host "health wait attempt \$i: HTTP \$(\$r.StatusCode)"
                    } catch {
                        Write-Host "health wait attempt \$i: \$(\$_.Exception.Message)"
                    }
                    Start-Sleep -Seconds 5
                }
                if (-not \$ok) {
                    Write-Error "Production health check failed. Confirm service ${SERVICE_NAME} and C:\\exhibition-portal\\portal.env.ps1."
                    exit 1
                }
            """
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                if (isUnix()) {
                    dir("${APP_DIR}") {
                        sh """
                            if [ -f app.pid ]; then
                                PID=\$(cat app.pid 2>/dev/null)
                                kill \$PID 2>/dev/null || true
                                sleep 2
                                kill -9 \$PID 2>/dev/null || true
                                rm -f app.pid
                            fi
                        """
                    }
                }
            }
        }
        success {
            echo 'Pipeline executed successfully'
        }
        failure {
            echo 'Pipeline failed'
        }
    }
}
