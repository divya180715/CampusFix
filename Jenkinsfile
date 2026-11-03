
pipeline {
    agent any

    tools {
        maven 'Maven-3.9.6'
        jdk 'JDK-17'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Compile & Test') {
            steps {
                echo 'Running JUnit 5 Tests via Maven...'
                sh 'mvn clean test'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Static Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    withCredentials([
                        string(
                            credentialsId: 'sonar-token',
                            variable: 'SONAR_TOKEN'
                        )
                    ]) {
                        sh '''
                            mvn sonar:sonar \
                            -Dsonar.projectKey=CampusFix \
                            -Dsonar.projectName="CampusFix" \
                            -Dsonar.host.url=http://host.docker.internal:9000 \
                            -Dsonar.token="$SONAR_TOKEN"
                        '''
                    }
                }
            }
        }

        stage('Package WAR') {
            steps {
                echo 'Building production WAR distribution...'
                sh 'mvn package -DskipTests=true'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.war',
                                     fingerprint: true
                }
            }
        }

        stage('Docker Build & Verify') {
            steps {
                echo 'Building Docker Image...'
                sh 'docker build -t campusfix-app:latest .'
            }
        }
    }

    post {
        success {
            echo 'CampusFix pipeline built, tested, analyzed, and packaged successfully!'
        }
        failure {
            echo 'Pipeline failed. Check build logs for diagnostic details.'
        }
    }
}
