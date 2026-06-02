@Library("jenkins-pipeline-library")

import fr.creative.jenkins.BuildContextHolder
import fr.creative.jenkins.config.ConfigUtils
import fr.creative.jenkins.exception.TechnicalException
import fr.creative.jenkins.model.DockerRegistry
import fr.creative.jenkins.model.Module
import fr.creative.jenkins.model.Sonar
import fr.creative.jenkins.service.*
import fr.creative.jenkins.utils.*
import fr.creative.jenkins.config.ConfigUtils

node() {
properties([
    parameters([
        choice(name:'deployTo', choices: ['','int','valid','demo'], defaultValue: '', description: 'Environnement sur lequel deployer, laisser vide pour ne pas déployer.'),
        booleanParam(name:'skipBuild', defaultValue: false),
        booleanParam(name:'skipLicense', defaultValue: false),
        booleanParam(name:'skipSonarqube', defaultValue: false),
        booleanParam(name:'skipVulnerabilityCheck', defaultValue: false),
        booleanParam(name:'skipTrivy', defaultValue: false),
        booleanParam(name:'skipPackage', defaultValue: false),
        booleanParam(name:'skipDeploy', defaultValue: false),
        booleanParam(name:'skipAPISecurity', defaultValue: false),
        booleanParam(name:'skipTU', defaultValue: false),
        booleanParam(name:'skipTI', defaultValue: false),
        booleanParam(name:'skipSonarQualityCheck', defaultValue: false)
    ])
])
    // Nom du Projet Gitlab
    String projectName = "minds-rgpd-api"
    // Nom de la branche
    String projectBranch = env.BRANCH_NAME
    // Version de la branche
    String version
    // set sonar critical behaviour
    boolean isSonarqubeCritical = !params.skipSonarQualityCheck

    // URL Channel Teams
    // TODO
    String webhookUrl = "https://creativecorebusiness.webhook.office.com/webhookb2/3757b4c6-ab34-424d-8289-0ea5d29282bc@07cdf6c2-b866-4ffc-a7cf-eaeb75545f95/JenkinsCI/8a1c571231184f73b10b5f85519a528d/41524f31-8514-413e-8217-a9a6bad73477"

    boolean isMergeRequest = false

    try {
        BuildContextHolder.init(this)

        GitlabService.instance().updatePipelineStatusToRunning()

       // Ne pas deployer s'il s'agit d'une pipeline déclenché par gitlab
        if(GitlabService.instance().isTriggeredByGitlab()){
            params.skipDeploy = true
            params.skipVulnerabilityCheck = true
            params.isSonarqubeCritical = false
            params.isMergeRequest = true

            projectBranch = "origin/" + env.gitlabSourceBranch
            println "projectBranch before checkout ${projectBranch}"
            checkout scmGit(branches: [[name: '${projectBranch}']], extensions: [], userRemoteConfigs: [[url: 'git@srv-gitlab.domaine.local:minds-labs/minds-rgpd/minds_rgpd_api.git']])
        } else {
            checkout scm
        }

        sh("chmod +x ./.platforms/ci/git-version.sh ./.platforms/ci/get-git-version.sh")
        version = sh(script: "./.platforms/ci/get-git-version.sh", returnStdout: true).trim()
        println("version  = $version")

        stage('Prepare') {
            sh("chmod 777 -R .platforms/ci/")
            sh("if [ -d target/ ]; then sudo chown -R jenkins target/; fi")
            sh("if [ -d target/ ]; then rm -rf target/; fi")
            sh("if [ -f git-version.json ]; then rm git-version.json; fi")
            sh("find . -type f -print0 | xargs -0 dos2unix -q")
            PROJECT_VERSION=version
            PropertiesUtils.updateValue(".env", "PROJECT_VERSION", version)
        }

        stage('Build') {
            if (!params.skipBuild) {
                println "Building project"
                sh """
                    SKIP_TU=${params.skipTU} \
                    SKIP_TI=${params.skipTI} \
                    .platforms/ci/build.sh
                """
                // Publish HTML reports
                if (fileExists('./target/site/jacoco/index.html')) {
                    JenkinsService.instance().publishHtml("./target/site/jacoco/", "index.html", "Code coverage", true)
                }
            }
        }

        stage("Licenses") {
            if (!params.skipLicense) {
                println "Check if exist contaminating licenses..."
                sh "bash ./.platforms/ci/check-licenses.sh"
            }
        }

        stage("Vulnerability") {
            if (!params.skipVulnerabilityCheck) {
                println "Launch vulnerability checking"
                sh "bash ./.platforms/ci/check-vulnerability.sh"

                println "Publish report"
                JenkinsService.instance().publishHtml("./target/", "dependency-check-report.html", "Owasp report", true)
            }
        }

        stage("Security Tests (Trivy)") {
            if (!params.skipTrivy) {
                boolean trivyError=false
                try {
                    // Rapport au format HTML
                    sh("bash .platforms/ci/trivy.sh")
                } catch (err) {
                    trivyError=true
                } finally {
                    // Publication du rapport HTML
                    JenkinsService.instance().publishHtml("./target/.trivy/", "trivy.html", "Trivy Report", true)
                    archiveArtifacts artifacts: 'target/.trivy/trivy.html', excludes: null
                    if (trivyError) {
                        String url = JenkinsService.instance().getJobUrl() + "/TrivyReport/"
                        JenkinsService.instance().setStageAsUnstable("Trivy - Niveau de sécurité insuffisant > ${url}")
                    }
                }
            }
        }

        stage("Sonarqube") {
            if (!params.skipSonarqube) {
                println "Qualimetry Sonarqube"
                HttpService.instance().waitForService("https://sonarqube.tools.k8s/")
                String sonarBranch = projectBranch
                sh "bash ./.platforms/ci/sonar.sh --git-branch ${sonarBranch} ${isSonarqubeCritical}"

                // Validation de l'analyse sonar (blocage si bug critique)
                if (isSonarqubeCritical) {
                    sonarToken=ConfigUtils.SONAR_TOKEN
                    def sonarproject = new Sonar()
                    println("Analysing ${projectName}")
                    sonarproject.key = projectName
                    SonarService.instance().checkProjectStatus(sonarproject, "main", true)
                }
            }
        }

        stage("Package") {
            if (!params.skipPackage) {
                DockerService.instance().login(DockerRegistry.getDefaultRegistry())
                println "Creating Docker Images"
                if (isMergeRequest) {
                    sh "bash ./.platforms/ci/package.sh"
                } else {
                    sh "bash ./.platforms/ci/package.sh --with-push"
                }
            }
        }

        stage("Deploy") {
            if (!params.skipDeploy && params.deployTo != ''){
                if (params.deployTo == "prod") {
                    timeout(time: 30, unit: 'MINUTES') {
                        input(
                            message: "Confirmer le déploiement en PROD (${projectBranch})",
                            ok: "Déployer"
                        )
                    }
                }
                withKubeConfig([credentialsId: 'kubeconfig-minds-admin']) {
                    println "Deploy project"
                    sh "bash ./.platforms/k8s/deploy.sh ${params.deployTo}"
                }
            }
        }

        stage("API Security Test (Zap)") {
            if (!params.skipAPISecurity) {
                boolean apiSecurityError = false
                try {
                    // Use the same credentials as frontend ZAP scanning
                    withCredentials([
                        // OAuth2 Client credentials
                        usernamePassword(credentialsId: 'minds-rgpd-api-oauth',
                                    usernameVariable: 'OAUTH_CLIENT_ID',
                                    passwordVariable: 'OAUTH_CLIENT_SECRET'),
                        // User credentials
                        usernamePassword(credentialsId: 'minds-rgpd-api-admin',
                                    usernameVariable: 'ZAP_USERNAME',
                                    passwordVariable: 'ZAP_PASSWORD')

                    ]) {
                        println "Running API Security Scan for environment: ${params.deployTo}"
                        sh """
                            OAUTH_CLIENT_ID='${OAUTH_CLIENT_ID}' \
                            OAUTH_CLIENT_SECRET='${OAUTH_CLIENT_SECRET}' \
                            ZAP_USERNAME='${ZAP_USERNAME}' \
                            ZAP_PASSWORD='${ZAP_PASSWORD}' \
                            bash .platforms/ci/security-scan.sh ${params.deployTo}
                        """

                    }
                } catch (err) {
                    apiSecurityError = true
                    println "API Security scan failed: ${err.getMessage()}"
                } finally {
                    // Archive API security reports
                    archiveArtifacts artifacts: 'security-reports/**/*', allowEmptyArchive: true, excludes: null

                    // Publish HTML reports
                    if (fileExists('security-reports/index.html')) {
                        publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'security-reports',
                            reportFiles: 'index.html',
                            reportName: 'ZAP Security Report',
                            reportTitles: 'OWASP ZAP Security Scan Results'
                        ])

                    }

                    if (fileExists('security-reports/api-coverage.html')) {
                        publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'security-reports',
                            reportFiles: 'api-coverage.html',
                            reportName: 'API Coverage Report',
                            reportTitles: 'OWASP ZAP API Coverage Report'
                        ])
                    }

                    if (apiSecurityError) {
                        String url = JenkinsService.instance().getJobUrl() + "/APISecurityReport/"
                        JenkinsService.instance().setStageAsUnstable("⚠️ API Security Test - Issues detected > ${url}")
                    } else {
                        println "✅ API Security scan completed successfully"
                    }
                }
            }
        }

        if (isMergeRequest) {
            println "'${projectName}': Successful merge request"
        } else {
            println "'${projectName}': Successful project deployment"
        }

        GitlabService.instance().updatePipelineStatusToSuccess()
    } catch (err) {
      JenkinsService.instance().raiseTechnicalError(err)
      GitlabService.instance().updatePipelineStatusToFailed()
    }
}
