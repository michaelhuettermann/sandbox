/*
Copyright [2017] [Michael Hüttermann]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Hints: This pipeline goes through the stages to stop an existing container, delete
the deployment and service, create new ones, replace the Docker image (for
demo purposes), and creates new service and deployment. The pipeline facilitates
the REST API for Oracle Container Cloud Service. The pipeline is for demo-purposes,
although it goes through typical steps, it is not production-ready. The pipeline
is parameterized, i.e. the version to be deployed can be injected. There are
other parameters as well, such as the bearer string and the URI where to find the
Docker image. As you can see, those demo aspects can be easily replaced by variants,
e.g. storing the Docker image directly and solely in Oracle Cloud, or using other
Cloud services. Some REST API calls use a bit more comprehensive JSON objects, which
are located in the same directory level and are just referenced. This demo pipeline
also shows how it could be extended with more full-fledged routines, e.g. using
Python to dynamically check the current state of processing, although usually, the
events are just queued and processed sequentially. The sleeps are mainly for having
a nicer experience while watching the pipeline with Jenkins Blue Ocean.
   */
node {
    def WORKSPACE

    @Library('Util') _

    stage('Prepare') {
        WORKSPACE = pwd()
        echo "where am I ... ${WORKSPACE}"
        echo "which version to process ... ${version}"
        sh "rm ${WORKSPACE}/*.json"
        sh "curl -O https://raw.githubusercontent.com/michaelhuettermann/sandbox/master/all/src/main/resources/jenkins/oracle-cloud/new-service.json"
        sh "curl -O https://raw.githubusercontent.com/michaelhuettermann/sandbox/master/all/src/main/resources/jenkins/oracle-cloud/create-deployment.json"
        sh "sed -i '' 's/VERSION/${version}/g' ${WORKSPACE}/new-service.json"
        sh "sed -i '' 's/VERSION/${version}/g' ${WORKSPACE}/create-deployment.json"
    }

    stage('Deployment Stop') {

sh '''
#!/bin/bash 
export PYTHONIOENCODING=utf8
echo -ne "Stopping deployment "
curl -sk  -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/deployments/meow-deploy/stop"
result=$(curl -sk -X 'GET' -H "Authorization: Bearer ${BEARER}" https://${CLOUDIP}/api/v2/deployments/meow-deploy) 
deploying=$(echo $result | grep "availability")
if [ -z "$deploying" ]; then
    echo "No deployment found ... !"
else 
    echo "Deployment found ..."
    for (( ; ; ))
    do
       result=$(curl -sk -X 'GET' -H "Authorization: Bearer ${BEARER}" https://${CLOUDIP}/api/v2/deployments/meow-deploy  | python -c "import sys, json; print(json.load(sys.stdin)['deployment']['current_state'])")
       if [ "$result" == "0" ]; then
          echo "Deployment stopped!"
          break
       else
          echo -ne "."
          sleep 2
          continue
       fi
    done
fi
'''
    }
    stage('Deployment Delete') {
        sh '''
curl -sk  -X "DELETE" -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/deployments/meow-deploy"
sleep 5
'''
    }
    stage('Service Delete') {
        sh '''
curl -sk -X  "DELETE" -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/services/meow"
sleep 5
'''
}
    stage('Image Delete') {
        sh '''
curl -sk  -X "DELETE" -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/images/${BINTRAYREGISTRY}/michaelhuettermann/alpine-tomcat7:${version}/hosts/2970cd1b-5571-6fda-3f21-1c6b19cd9ab1"
sleep 5
'''
    }
    stage('Image Pull') {
        sh '''
curl -sk  -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/images/${BINTRAYREGISTRY}/michaelhuettermann/alpine-tomcat7:${version}/hosts/2970cd1b-5571-6fda-3f21-1c6b19cd9ab1/pull"
sleep 5
'''
    }
    stage('Service Create') {
        echo "workspace = ${WORKSPACE}"
        sh '''
#!/bin/bash 
curl -ski -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/services/" --data "@${WORKSPACE}/new-service.json"
sleep 5
'''
    }
    stage('Deployment Create') {
        echo "workspace = ${WORKSPACE}"
        timeout(time:5, unit:'MINUTES') {
            input message:"Really sure to go the very last mile, with version ${version}?"
        }
        sh '''
#!/bin/bash 
curl -ski -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/deployments/" --data "@${WORKSPACE}/create-deployment.json"
sleep 5
'''
    }

}