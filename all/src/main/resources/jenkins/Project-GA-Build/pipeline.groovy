pipeline {
    agent any

    withCredentials([string(credentialsId: 'BINTRAY_KEY', variable: 'BINTRAY')]) {

        stages {
        stage('Prepare') {
            steps {
                sh 'curl -O http://$ARTI3/simple/libs-releases-staging-local/com/huettermann/web/$version/all-$version.war'
            }
        }

        stage('Certify WAR') {
            steps {
                sh 'cp all-$version.war all-$version-GA.war'
            }
        }

        stage('Promote WAR to Bintray') {
            steps {
                sh '''
       curl -u michaelhuettermann:${BINTRAY} -X DELETE https://api.bintray.com/packages/huettermann/meow/cat/versions/$version
       curl -u michaelhuettermann:${BINTRAY} -H "Content-Type: application/json" -X POST https://api.bintray.com/packages/huettermann/meow/cat/$version --data """{ "name": "$version", "desc": "desc" }"""
       curl -T "$WORKSPACE/all-$version-GA.war" -u michaelhuettermann:${bintray_key} -H "X-Bintray-Package:cat" -H "X-Bintray-Version:$version" https://api.bintray.com/content/huettermann/meow/
       curl -u michaelhuettermann:${BINTRAY} -H "Content-Type: application/json" -X POST https://api.bintray.com/content/huettermann/meow/cat/$version/publish --data '{ "discard": "false" }'
       '''
            }
        }

        stage('Certify Docker Image') {
            steps {
                sh 'docker login -u michaelhuettermann -p ${BINTRAY} huettermann-docker-registry.bintray.io'
                sh 'docker tag $ARTI3REGISTRY/michaelhuettermann/alpine-tomcat7:$version $BINTRAYREGISTRY/michaelhuettermann/alpine-tomcat7:$version'
            }
        }

        stage('Promote Docker Image to Bintray') {
            steps {
                sh 'docker push $BINTRAYREGISTRY/michaelhuettermann/alpine-tomcat7:$version --disable-content-trust'
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