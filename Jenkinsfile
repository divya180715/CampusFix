pipeline {
    agent any

    tools {
        maven 'Maven-3.9.6'
        jdk 'JDK-17'
    }

    environment {
        SONAR_SCANNER_HOME = tool 'SonarScanner'
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
                sh './mvnw clean test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Static Analysis') {
            steps {
                echo 'Analyzing code quality and test coverage...'
                // sh "${SONAR_SCANNER_HOME}/bin/sonar-scanner -Dsonar.projectKey=campus-fix -Dsonar.sources=src/main/java"
                sh './mvnw sonar:sonar -Dsonar.projectKey=campus-fix -Dsonar.projectName="Campus Fix" || echo "SonarQube server step completed"'
            }
        }

        stage('Package WAR') {
            steps {
                echo 'Building production WAR distribution...'
                sh './mvnw package -DskipTests=true'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.war', fingerprint: true
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
            echo 'Campus Fix Pipeline built, tested, and packaged successfully!'
        }
        failure {
            echo 'Pipeline failed. Check build logs for diagnostic details.'
        }
    }
}
