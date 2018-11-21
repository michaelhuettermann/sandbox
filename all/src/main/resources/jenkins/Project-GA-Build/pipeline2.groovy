pipeline {
    agent any
    stages {
        stage('Prepare') {
            steps {
                echo 'Prepare'
            }
        }
        stage('Certify Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'OCIR', passwordVariable: 'OCIR_PW', usernameVariable: 'OCIR_UN')]) {
                    sh 'docker login -u $OCIR_UN -p $OCIR_PW iad.ocir.io  '
                    sh 'docker tag $ARTI3REGISTRY/michaelhuettermann/alpine-tomcat7:$version iad.ocir.io/mh/michaelhuettermann/alpine-tomcat7:$version'
                    sh 'docker images'
                }
            }
        }
        stage('Promote Docker Image to Registry') {
            steps {
                sh 'docker push iad.ocir.io/mh/michaelhuettermann/alpine-tomcat7:$version'
            }
        }
    }
    post {
        always {
            echo 'Finished!'
            sh 'docker logout iad.ocir.io'
            deleteDir()
        }
        success {
            echo 'Succeeeded.'
        }
        unstable {
            echo 'Unstable.'
        }
        failure {
            echo 'Failed.'
        }
        changed {
            echo 'Things in life change.'
        }
    }
}