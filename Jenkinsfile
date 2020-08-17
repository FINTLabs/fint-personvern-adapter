pipeline {
    agent { label 'docker' }
    stages {
        stage('Build') {
            steps {
                script {
                    props=readProperties file: 'gradle.properties'
                }
                sh "docker build --tag ${GIT_COMMIT} ."
            }
        }
        stage('Publish') {
            when {
                branch 'master'
            }
            steps {
                withDockerRegistry([credentialsId: 'fintlabs.azurecr.io', url: 'https://fintlabs.azurecr.io']) {
                    sh "docker tag ${GIT_COMMIT} fintlabs.azurecr.io/personvern-adapter:build.${BUILD_NUMBER}"
                    sh "docker push fintlabs.azurecr.io/personvern-adapter:build.${BUILD_NUMBER}"
                }
                withDockerRegistry([credentialsId: 'asgeir-docker', url: '']) {
                    sh "docker tag ${GIT_COMMIT} fint/personvern-adapter:latest"
                    sh "docker push fint/personvern-adapter:latest"
                }
            }
        }
        stage('Publish Version') {
            when {
                tag pattern: "v\\d+\\.\\d+\\.\\d+(-\\w+-\\d+)?", comparator: "REGEXP"
            }
            steps {
                script {
                    VERSION = TAG_NAME[1..-1]
                }
                withDockerRegistry([credentialsId: 'asgeir-docker', url: '']) {
                    sh "docker tag ${GIT_COMMIT} fint/personvern-adapter:${VERSION}"
                    sh "docker push fint/personvern-adapter:${VERSION}"
                    sh "docker tag fint/personvern-adapter:${VERSION} fint/personvern-adapter:latest"
                    sh "docker push fint/personvern-adapter:latest"
                }
                withDockerRegistry([credentialsId: 'fintlabs.azurecr.io', url: 'https://fintlabs.azurecr.io']) {
                    sh "docker tag ${GIT_COMMIT} fintlabs.azurecr.io/personvern-adapter:${VERSION}"
                    sh "docker push fintlabs.azurecr.io/personvern-adapter:${VERSION}"
                }
            }
        }
        stage('Publish PR') {
            when { changeRequest() }
            steps {
                withDockerRegistry([credentialsId: 'fintlabs.azurecr.io', url: 'https://fintlabs.azurecr.io']) {
                    sh "docker tag ${GIT_COMMIT} fintlabs.azurecr.io/personvern-adapter:${BRANCH_NAME}.${BUILD_NUMBER}"
                    sh "docker push fintlabs.azurecr.io/personvern-adapter:${BRANCH_NAME}.${BUILD_NUMBER}"
                }
            }
        }
    }
}
