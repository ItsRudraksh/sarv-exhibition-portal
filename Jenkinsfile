// Jenkins: same agent pattern as pharma-erp (Java17 + Maven3 tools).
// Groovy Pipeline script — comments must be // or /* */, not # (that is parsed as a shebang).
// Windows Server at http://43.225.195.200/ — native MySQL 8 on 3306, no Docker, no ZK credentials.
// Node 22: Jenkins Windows service PATH does not include an interactive user's Node.
// Frontend prepends C:\Program Files\nodejs and NODE_HOME. Restart Jenkins after installing Node.
// Staging listen port is 8082. 8081 is pharma-erp-staging on this host. Production is port 80.

def windowsInstallExhibition(String installDir, String serviceName, String kind, String workspace, String jarSource, String appDir, String stagingPort) {
    powershell """
                \$installDir = '${installDir}'
                \$service = '${serviceName}'
                \$kind = '${kind}'
                \$workspace = '${workspace}'
                \$jarSource = '${jarSource}'.Trim()
                \$stagingPort = '${stagingPort}'
                \$appTarget = '${appDir}\\target'
                \$destJar = Join-Path \$installDir 'exhibition-portal.jar'

                New-Item -ItemType Directory -Force -Path \$installDir | Out-Null
                New-Item -ItemType Directory -Force -Path (Join-Path \$installDir 'files') | Out-Null

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
                Copy-Item -LiteralPath \$src -Destination \$destJar -Force

                \$startSrc = Join-Path \$workspace 'deploy\\windows\\start-portal.ps1'
                if (Test-Path -LiteralPath \$startSrc) {
                    Copy-Item -LiteralPath \$startSrc -Destination (Join-Path \$installDir 'start-portal.ps1') -Force
                }

                \$envTarget = Join-Path \$installDir 'portal.env.ps1'
                \$envExample = Join-Path \$workspace 'deploy\\windows\\portal.env.example.ps1'
                if (-not (Test-Path -LiteralPath \$envTarget) -and (Test-Path -LiteralPath \$envExample)) {
                    Copy-Item -LiteralPath \$envExample -Destination \$envTarget
                    if (\$kind -eq 'staging') {
                        \$txt = Get-Content -LiteralPath \$envTarget -Raw
                        \$txt = \$txt.Replace('C:\\exhibition-portal\\files', 'C:\\exhibition-portal-staging\\files')
                        Set-Content -LiteralPath \$envTarget -Value \$txt -NoNewline
                    }
                    Write-Host "Created \$envTarget - edit DATASOURCE_PASSWORD and EXHIBITION_STAFF_BOOTSTRAP_PASSWORD before start."
                }
                if (\$kind -eq 'staging' -and (Test-Path -LiteralPath \$envTarget)) {
                    \$txt = Get-Content -LiteralPath \$envTarget -Raw
                    \$txt = \$txt.Replace('\$env:SERVER_PORT = ''80''', '\$env:SERVER_PORT = ''${stagingPort}''')
                    \$txt = \$txt.Replace('\$env:SERVER_PORT = ''8081''', '\$env:SERVER_PORT = ''${stagingPort}''')
                    Set-Content -LiteralPath \$envTarget -Value \$txt -NoNewline
                    Write-Host "Pinned SERVER_PORT=\$stagingPort in \$envTarget (8081 is pharma-erp-staging; 80 is production)."
                }

                Write-Host "Installed exhibition-portal.jar under \$installDir"

                \$svc = Get-Service -Name \$service -ErrorAction SilentlyContinue
                if (-not \$svc) {
                    \$installScript = Join-Path \$workspace 'deploy\\windows\\install-service.ps1'
                    if (-not (Test-Path -LiteralPath \$installScript)) {
                        Write-Error ("Missing " + \$installScript)
                        exit 1
                    }
                    Write-Host "Windows service \$service is not installed. Creating it (Jenkins LocalSystem)..."
                    if (\$kind -eq 'staging') {
                        & \$installScript -Staging
                    } else {
                        & \$installScript
                    }
                    if (\$LASTEXITCODE -and \$LASTEXITCODE -ne 0) { exit \$LASTEXITCODE }
                    \$svc = Get-Service -Name \$service -ErrorAction SilentlyContinue
                    if (-not \$svc) {
                        Write-Error ("Failed to install Windows service " + \$service + ". Elevated, from the repo: .\\deploy\\windows\\install-service.ps1" + \$(if (\$kind -eq 'staging') { ' -Staging' } else { '' }))
                        exit 1
                    }
                }

                \$envText = ''
                if (Test-Path -LiteralPath \$envTarget) {
                    \$envText = Get-Content -LiteralPath \$envTarget -Raw
                }
                if (\$envText -match 'change-me-db' -or \$envText -match 'change-me-staff') {
                    Write-Error ("Service " + \$service + " is installed and " + \$installDir + " has the JAR. Edit " + \$envTarget + " (DATASOURCE_PASSWORD and EXHIBITION_STAFF_BOOTSTRAP_PASSWORD), create MySQL DB/user with deploy\\windows\\init-mysql.sql, then rebuild. start-portal.ps1 will not start with the example passwords.")
                    exit 1
                }

                if (\$svc.Status -eq 'Running') {
                    Write-Host "Stopping service \$service..."
                    net stop \$service
                }
                Write-Host "Starting service \$service..."
                net start \$service
                if (\$LASTEXITCODE -and \$LASTEXITCODE -ne 0) {
                    Write-Error ("net start " + \$service + " failed. Check " + \$envTarget + " (MySQL password, staff bootstrap) and the Windows Event Log.")
                    exit 1
                }
                Write-Host "Waiting for startup..."
                Start-Sleep -Seconds 20
            """
}

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
        STAGING_PORT = '8082'
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
                            // Jenkins Windows service (SYSTEM) does not inherit an interactive user's PATH.
                            // Java17/Maven3 are Jenkins tools; Node is not. Prefer C:\\Program Files\\nodejs.
                            powershell '''
                                $dirs = @($env:NODE_HOME, 'C:\\Program Files\\nodejs', 'C:\\Program Files (x86)\\nodejs', 'C:\\nodejs')
                                foreach ($dir in $dirs) {
                                    if ($dir -and (Test-Path -LiteralPath (Join-Path $dir 'npm.cmd'))) {
                                        $env:Path = "$dir;$env:Path"
                                        Write-Host "Using npm from $dir"
                                        break
                                    }
                                }
                                if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
                                    Write-Error 'npm not found for the Jenkins service account. Install Node 22 to C:\\Program Files\\nodejs (all users), set NODE_HOME to that folder if it lives elsewhere, then restart the Jenkins Windows service. An interactive user PATH is ignored.'
                                    exit 1
                                }
                                npm ci
                                if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
                                npm run build
                                if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
                            '''
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
            when {
                anyOf {
                    branch 'dev'
                    branch 'poc'
                    expression {
                        return (env.BRANCH_NAME == 'poc') || (env.GIT_BRANCH == 'origin/poc') || (env.GIT_BRANCH == 'poc') ||
                               (env.BRANCH_NAME == 'dev') || (env.GIT_BRANCH == 'origin/dev') || (env.GIT_BRANCH == 'dev')
                    }
                }
            }
            steps {
                script {
                    if (isUnix()) {
                        echo 'Skipping staging deployment on Linux'
                    } else {
                        windowsInstallExhibition(
                                env.STAGING_DIR,
                                env.STAGING_SERVICE,
                                'staging',
                                env.WORKSPACE,
                                params.JAR_SOURCE ?: '',
                                env.APP_DIR,
                                env.STAGING_PORT)
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
                        windowsInstallExhibition(
                                env.PROD_DIR,
                                env.SERVICE_NAME,
                                'prod',
                                env.WORKSPACE,
                                params.JAR_SOURCE ?: '',
                                env.APP_DIR,
                                env.STAGING_PORT)
                    }
                }
            }
        }

        stage('Health Check') {
            when {
                anyOf {
                    branch 'main'
                    branch 'dev'
                    branch 'poc'
                    expression {
                        return (env.BRANCH_NAME == 'main') || (env.GIT_BRANCH == 'origin/main') || (env.GIT_BRANCH == 'main') ||
                               (env.BRANCH_NAME == 'poc') || (env.GIT_BRANCH == 'origin/poc') || (env.GIT_BRANCH == 'poc') ||
                               (env.BRANCH_NAME == 'dev') || (env.GIT_BRANCH == 'origin/dev') || (env.GIT_BRANCH == 'dev')
                    }
                }
            }
            steps {
                script {
                    if (isUnix()) {
                        echo 'Health check is Windows (public host). Skipping Unix.'
                    } else {
                        def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: ''
                        def isMain = (branch == 'main' || branch.endsWith('/main'))
                        def healthUrl = isMain ? 'http://127.0.0.1/actuator/health' : "http://127.0.0.1:${env.STAGING_PORT}/actuator/health"
                        def hint = isMain ? "service ${SERVICE_NAME} and C:\\\\exhibition-portal\\\\portal.env.ps1" : "service ${STAGING_SERVICE} and C:\\\\exhibition-portal-staging\\\\portal.env.ps1 (port ${env.STAGING_PORT}; 8081 is pharma-erp-staging)"
                        powershell """
                \$ok = \$false
                \$healthUrl = '${healthUrl}'
                for (\$i = 1; \$i -le 48; \$i++) {
                    try {
                        \$r = Invoke-WebRequest -Uri \$healthUrl -UseBasicParsing -TimeoutSec 5
                        if (\$r.StatusCode -eq 200) {
                            Write-Host "Actuator HTTP 200 at \$healthUrl after attempt \$i"
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
                    Write-Error "Health check failed for \$healthUrl. Confirm ${hint}."
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
