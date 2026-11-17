pipeline {
    agent any

    parameters {
        booleanParam(
            name: 'PUBLISH_IMAGE',
            defaultValue: false,
            description: 'Whether to publish the built Docker image to remote registry'
        )
        stringParam(
            name: 'IMAGE_TAG',
            defaultValue: 'latest',
            description: 'Docker image tag'
        )
        stringParam(
            name: 'REGISTRY_CREDENTIALS_ID',
            defaultValue: 'ghcr-credentials',
            description: 'Jenkins credentials ID for registry authentication'
        )
        stringParam(
            name: 'IMAGE_NAME',
            defaultValue: 'ghcr.io/nuera/central-neuro-inventory-demo',
            description: 'Full image name including registry'
        )
    }

    environment {
        JAVA_HOME = tool 'jdk-21'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                echo "Checking out source code..."
                checkout scm
            }
        }

        stage('Compile & Test') {
            steps {
                echo "Executing unit and integration tests..."
                sh './mvnw clean test -B'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package Application') {
            steps {
                echo "Packaging application JAR artifact..."
                sh './mvnw package -DskipTests -B'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo "Building Docker image: ${params.IMAGE_NAME}:${params.IMAGE_TAG}..."
                sh "docker build -t ${params.IMAGE_NAME}:${params.IMAGE_TAG} ."
            }
        }

        stage('Publish Image') {
            when {
                expression { return params.PUBLISH_IMAGE == true }
            }
            steps {
                echo "Publishing Docker image to remote registry..."
                withCredentials([usernamePassword(
                    credentialsId: params.REGISTRY_CREDENTIALS_ID,
                    usernameVariable: 'REGISTRY_USER',
                    passwordVariable: 'REGISTRY_PASS'
                )]) {
                    sh "echo \$REGISTRY_PASS | docker login -u \$REGISTRY_USER --password-stdin ghcr.io"
                    sh "docker push ${params.IMAGE_NAME}:${params.IMAGE_TAG}"
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline finished. Cleaning workspace temporary assets..."
            cleanWs deleteDirs: true, notFailBuild: true
        }
        success {
            echo "Build, test, packaging, and image operations completed successfully."
        }
        failure {
            echo "Pipeline execution failed. Review stage logs and Surefire reports."
        }
    }
}
