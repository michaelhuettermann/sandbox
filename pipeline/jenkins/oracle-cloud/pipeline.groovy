node {
    stage('Deploy Cloud') {
sh '''
#!/bin/bash -ex
set +x;
export PYTHONIOENCODING=utf8
echo -ne "Stopping deployment "
curl -sk  -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/deployments/meow-deploy/stop"
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
curl -sk  -X "DELETE" -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/deployments/meow-deploy"
sleep 5
curl -sk -X  "DELETE" -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/services/meow"
sleep 5
curl -sk  -X "DELETE" -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/images/huettermann-docker-registry.bintray.io/michaelhuettermann/alpine-tomcat7:1.0.0/hosts/2970cd1b-5571-6fda-3f21-1c6b19cd9ab1"
sleep 5
curl -sk  -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/images/huettermann-docker-registry.bintray.io/michaelhuettermann/alpine-tomcat7:1.0.0/hosts/2970cd1b-5571-6fda-3f21-1c6b19cd9ab1/pull"
sleep 20
curl -ski -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/services/" --data "@/Users/michaelh/work/data/share/intellilj/sandbox/pipeline/jenkins/oracle-cloud/new-service.json"
sleep 5
curl -ski -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/deployments/" --data "@/Users/michaelh/work/data/share/intellilj/sandbox/pipeline/jenkins/oracle-cloud/create-deployment.json"
sleep 5
'''
    }
}
