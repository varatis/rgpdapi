@Library("jenkins-pipeline-library")

import fr.creative.jenkins.BuildContextHolder
import fr.creative.jenkins.config.ConfigUtils
import fr.creative.jenkins.exception.TechnicalException
import fr.creative.jenkins.model.DockerRegistry
import fr.creative.jenkins.model.Module
import fr.creative.jenkins.model.Sonar
import fr.creative.jenkins.service.*
import fr.creative.jenkins.utils.*

node() {
    // Nom du Projet Gitlab
    String projectName = "minds-rgpd-api"
    // Branche GIT à builder/packager/déployer
    String projectBranch = params.BRANCH_NAME
    // Environnement sur lequel on souhaite déployer l'application
    String deployTo = params.DEPLOY_TO
    // Version de l'application que l'on souhaite déployer
    String version = params.PROJECT_VERSION
    // Skip Build
    boolean skipBuild = params.SKIP_BUILD
    // Skip licenses checking
    boolean skipLicense = params.SKIP_LICENSE
    // Skip Sonarqube
    boolean skipSonarqube = params.SKIP_SONARQUBE
    // set sonar critical behaviour
    boolean isSonarqubeCritical = true
    // Skip Vulnerability checking
    boolean skipVulnerabilityCheck = params.SKIP_VULNERABILITY_CHECK
    // Skip Trivy
    boolean skipTrivy = params.SKIP_TRIVY
    // Skip Package
    boolean skipPackage = params.SKIP_PACKAGE
    // Skip Deploy
    boolean skipDeploy = params.SKIP_DEPLOY
    // Skip api security scan
    boolean skipAPISecurity = params.SKIP_API_SECURITY_SCAN
    // Skip tests unitaires
    boolean skipTU = params.SKIP_TU
    // Skip tests d'intégration
    boolean skipTI = params.SKIP_TI

    // URL Channel Teams
    // TODO
    String webhookUrl = "https://creativecorebusiness.webhook.office.com/webhookb2/3757b4c6-ab34-424d-8289-0ea5d29282bc@07cdf6c2-b866-4ffc-a7cf-eaeb75545f95/JenkinsCI/8a1c571231184f73b10b5f85519a528d/41524f31-8514-413e-8217-a9a6bad73477"

    boolean isMergeRequest = false

    try {
        BuildContextHolder.init(this)

        GitlabService.instance().updatePipelineStatusToRunning()

       // Ne pas deployer s'il s'agit d'une pipeline déclenché par gitlab
        if(GitlabService.instance().isTriggeredByGitlab()){
            skipDeploy = true
            skipVulnerabilityCheck = true
            isSonarqubeCritical = false
            isMergeRequest = true

            projectBranch = "origin/" + env.gitlabSourceBranch

            println "projectBranch before checkout ${projectBranch}"
            checkout scmGit(branches: [[name: '${projectBranch}']], extensions: [], userRemoteConfigs: [[url: 'git@srv-gitlab.domaine.local:minds-labs/minds-rgpd/minds_rgpd_api.git']])
        } else {
            checkout scm
        }

        if (version == "") {
            // Use git commit short hash like the frontend instead of branch name
            sh("chmod +x ./.platforms/ci/git-version.sh ./.platforms/ci/get-git-version.sh")
            version = sh(script: "./.platforms/ci/get-git-version.sh", returnStdout: true).trim()
            println("version  = $version")
        }

        stage("Approval PROD") {
            if (deployTo == "prod") {
                timeout(time: 30, unit: 'MINUTES') {
                    input(
                        message: "Confirmer le déploiement en PROD (${params.BRANCH_NAME})",
                        ok: "Déployer"
                    )
                }
            }
        }

        stage('Prepare') {
            sh("chmod 777 -R .platforms/ci/")
            sh("if [ -d target/ ]; then sudo chown -R jenkins target/; fi")
            sh("if [ -d target/ ]; then rm -rf target/; fi")
            sh("if [ -f git-version.json ]; then rm git-version.json; fi")
            sh("find . -type f -print0 | xargs -0 dos2unix")
            PROJECT_VERSION=version
            PropertiesUtils.updateValue(".env", "PROJECT_VERSION", version)
            if (isMergeRequest) {
                JenkinsService.instance().appendBuildDescription("'${projectName}': Merge Request Branch '${projectBranch}' To 'origin/develop'")
            } else {
                JenkinsService.instance().appendBuildDescription("'${projectName}': Deploy Branch '${projectBranch}' To '${deployTo}'")
            }
        }

        stage('Build') {
            if (!skipBuild) {
                println "Building project"
                sh """
                    SKIP_TU=${skipTU} \
                    SKIP_TI=${skipTI} \
                    .platforms/ci/build.sh
                """
                // Publish HTML reports
                if (fileExists('./target/site/jacoco/index.html')) {
                    JenkinsService.instance().publishHtml("./target/site/jacoco/", "index.html", "Code coverage", true)
                }
            }
        }

        stage("Licenses") {
            if (!skipLicense) {
                println "Check if exist contaminating licenses..."
                sh "bash ./.platforms/ci/check-licenses.sh"
            }
        }

        stage("Vulnerability") {
            if (!skipVulnerabilityCheck) {
                println "Launch vulnerability checking"
                sh "bash ./.platforms/ci/check-vulnerability.sh"

                println "Publish report"
                JenkinsService.instance().publishHtml("./target/", "dependency-check-report.html", "Owasp report", true)
            }
        }

        stage("Security Tests (Trivy)") {
            if (!skipTrivy) {
                boolean trivyError=false
                try {
                    // Rapport au format HTML
                    sh("bash .platforms/ci/trivy.sh")
                } catch (err) {
                    trivyError=true
                } finally {
                    // Publication du rapport HTML
                    JenkinsService.instance().publishHtml("./target/.trivy/", "trivy.html", "Trivy Report", true)
                    archiveArtifacts artifacts: 'trivy/.trivy/trivy.html', excludes: null
                    if (trivyError) {
                        qualityWarningStage += " TRIVY |"
                        String url = JenkinsService.instance().getJobUrl() + "/TrivyReport/"
                        JenkinsService.instance().setStageAsUnstable("Trivy - Niveau de sécurité insuffisant > ${url}")
                    }
                }
            }
        }

        stage("Sonarqube") {
            if (!skipSonarqube) {
                println "Qualimetry Sonarqube"
                HttpService.instance().waitForService("https://sonarqube.tools.k8s/")
                String sonarBranch = "main"
                sh "bash ./.platforms/ci/sonar.sh --git-branch ${sonarBranch}"

                // Validation de l'analyse sonar (blocage si bug critique)
                if (isSonarqubeCritical) {
                    def sonarproject = new Sonar()
                    println("Analysing minds-rgpd-api")
                    sonarproject.key = "minds-rgpd-api"
                    SonarService.instance().checkProjectStatus(sonarproject, sonarBranch, true)
                }
            }
        }

        stage("Package") {
            if (!skipPackage) {
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
            if (!skipDeploy){
                withKubeConfig([credentialsId: 'kubeconfig-minds-admin']) {
                    println "Deploy project"
                    sh "bash ./.platforms/k8s/deploy.sh ${deployTo}"
                }
            }
        }

        stage("API Security Test (Zap)") {
            if (!skipAPISecurity) {
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
                        println "Running API Security Scan for environment: ${deployTo}"
                        sh """
                            OAUTH_CLIENT_ID='${OAUTH_CLIENT_ID}' \
                            OAUTH_CLIENT_SECRET='${OAUTH_CLIENT_SECRET}' \
                            ZAP_USERNAME='${ZAP_USERNAME}' \
                            ZAP_PASSWORD='${ZAP_PASSWORD}' \
                            bash .platforms/ci/security-scan.sh ${deployTo}
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
        //MicrosoftTeamsService.instance().send(webhookUrl, "Deploy 'TunnelCourtier' (Branch '${projectBranch}' To '${deployTo}') SUCCESS")
    } catch (err) {
      JenkinsService.instance().raiseTechnicalError(err)
      GitlabService.instance().updatePipelineStatusToFailed()
      //MicrosoftTeamsService.instance().send(webhookUrl, "Deploy 'TunnelCourtier' (Branch '${projectBranch}' To '${deployTo}') FAILED")
    }
}
