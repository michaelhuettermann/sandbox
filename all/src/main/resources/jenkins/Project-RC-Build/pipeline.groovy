pipeline {
    agent any
    stages {
        stage('Prepare') {
            steps {
                echo 'Preparing ...'
                sh 'curl -O http://$ARTI3/list/libs-release-local/com/huettermann/web/$version/all-$version.war'

                devOpticsConsumes masterUrl: 'http://localhost:8080', jobName: 'devoptics/application-comp'
            }
        }
        stage('Unnecessary things') {
            when {
                branch 'production'
            }
            steps {
                echo 'Deploying'
            }

        }
        stage('Certify WAR') {
            steps {
                echo 'Certifying WAR ...'
                fingerprint 'all-$version.war'
                fingerprint 'README.md'
            }
        }
        stage('Promote WAR') {
            steps {
                withCredentials([string(credentialsId: 'ARTIFACTORY_TOKEN', variable: 'ARTIFACTORY')]) {
                    sh 'jfrog rt cp --url=https://$ARTI3 --apikey=$ARTIFACTORY --flat=true libs-release-local/com/huettermann/web/$version/ ' + 'libs-releases-staging-local/com/huettermann/web/$version/'
                }
            }
        }
        stage('Certify Docker Image') {
            steps {
                echo 'Certifying Docker Image ...'
            }
        }
        stage('Promote Docker Image') {
            steps {
                withCredentials([string(credentialsId: 'ARTIFACTORY_TOKEN', variable: 'ARTIFACTORY')]) {
                        sh '''curl -H "X-JFrog-Art-Api:$ARTIFACTORY" -X POST https://$ARTI3/api/docker/docker-local/v2/promote ''' +
                       '''-H "Content-Type:application/json" ''' +
                       '''-d \'{"targetRepo" : "docker-prod-local", "dockerRepository" : "michaelhuettermann/alpine-tomcat7", "tag": "\'$version\'", "copy": true }\'
                       '''
                }
            }
        }
    }
    post {
        always {
            echo 'Finished!'
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
