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

                withCredentials([string(credentialsId: 'ARTIFACTORY_TOKEN', variable: 'ARTIFACTORY')]) {
                    sh 'curl -H "X-JFrog-Art-Api:$ARTIFACTORY" -X POST https://$ARTI3/api/search/aql -T /Users/michaelh/.jenkins/jobs/Project-RC-Build/workspace/all/src/main/resources/jenkins/Project-RC-Build/search.aql > all/src/main/resources/jenkins/Project-RC-Build/out.json'
                }
                script {
                    workspace2 = pwd()
                    echo workspace2
                    echo $WORKSPACE

                    new File('/Users/michaelh/.jenkins/jobs/Project-RC-Build/workspace/all/src/main/resources/jenkins/Project-RC-Build/versions.txt').delete()
                    f = new File('/Users/michaelh/.jenkins/jobs/Project-RC-Build/workspace/all/src/main/resources/jenkins/Project-RC-Build/versions.txt')
                    String json = new File('/Users/michaelh/.jenkins/jobs/Project-RC-Build/workspace/all/src/main/resources/jenkins/Project-RC-Build/out.json').text
                    def map = parseJsonToMap(json)
                    map.results.each{ k, v ->
                        myVersion = "${k.name}".split("-")[1].replaceAll(".war","")
                        println myVersion
                        f.append(myVersion+"\n")
                    }
                }
            }
        }
        stage('Input') {
            steps {
                script {
                    f = new File('/Users/michaelh/.jenkins/jobs/Project-RC-Build/workspace/all/src/main/resources/jenkins/Project-RC-Build/versions.txt')
                    env.version = input message: 'User input required', ok: 'Release!',
                            parameters: [choice(name: 'version', choices: "$f.text", description: 'Which version should be promoted??')]
                    println env.version
                }
            }
        }
        //stage('Prepare') {
        //    steps {
        //        echo 'Preparing ...'
        //        sh 'curl -O http://$ARTI3/list/libs-release-local/com/huettermann/web/$version/all-$version.war'
        //        devOpticsConsumes masterUrl: 'http://localhost:8080/', jobName: 'devoptics/application-comp'
        //    }
        //}
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
                //fingerprint 'README.md'
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
            //deleteDir()
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
