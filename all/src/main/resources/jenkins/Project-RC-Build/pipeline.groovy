import groovy.json.JsonSlurperClassic

@NonCPS
def parseJsonToMap(String json) {
    final slurper = new JsonSlurperClassic()
    return new HashMap<>(slurper.parseText(json))
}
pipeline {
    agent any
    stages {
        stage('Prepare') {
            steps {
                checkout scm
                withCredentials([string(credentialsId: 'ARTIFACTORY_TOKEN', variable: 'ARTIFACTORY')]) {
                    sh 'curl -H "X-JFrog-Art-Api:$ARTIFACTORY" -X POST https://$ARTI3/api/search/aql -T "${WORKSPACE}/all/src/main/resources/jenkins/Project-RC-Build/search.aql" > "${WORKSPACE}/all/src/main/resources/jenkins/Project-RC-Build/out.json"'
                }
                script {
                    workspace = pwd()
                    String json = readFile("${workspace}/all/src/main/resources/jenkins/Project-RC-Build/out.json").trim()
                    String content = ''
                    def map = parseJsonToMap(json)
                    map.results.each{ k, v ->
                        myVersion = "${k.name}".split("-")[1].replaceAll(".war","")
                        println myVersion
                        content <<= myVersion+"\n"
                    }
                    writeFile file: "${workspace}/all/src/main/resources/jenkins/Project-RC-Build/versions.txt", text: content.toString().trim()
                }
            }
        }
        stage('Input') {
            steps {
                script {
                    String f = readFile("${workspace}/all/src/main/resources/jenkins/Project-RC-Build/versions.txt")
                    env.version = input message: 'User input required', ok: 'Release!',
                            parameters: [choice(name: 'version', choices: "$f", description: 'Which version should be promoted??')]
                    println env.version
                }
            }
        }
        stage('k8s down') {
            when {
                branch 'production'
            }
            steps {
                sh 'kubectl delete ReplicationController meow || true'
                sh 'kubectl delete service meow  || true'
                sh 'kubectl get services   || true'
            }
        }
        stage('Certify WAR') {
            steps {
                echo 'Certifying WAR ...'
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
        stage('k8s up') {
            when {
                branch 'production'
            }
            steps {
                sh "sed -i '' 's/VERSION/$version/g' ${WORKSPACE}/all/src/main/resources/jenkins/Project-RC-Build/meow.yaml"
                sh "sed -i '' 's#REGISTRY#$ARTI3REGISTRY#g' ${WORKSPACE}/all/src/main/resources/jenkins/Project-RC-Build/meow.yaml"
                sh 'kubectl apply -f ${WORKSPACE}/all/src/main/resources/jenkins/Project-RC-Build/meow.yaml'
                sh 'kubectl get services'
                sh 'curl $(minikube service meow  --url)/all/message?param=world'
                sh 'curl $(minikube service meow  --url)/all/'
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
