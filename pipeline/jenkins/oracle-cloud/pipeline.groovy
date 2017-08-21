node {
    stage('Deploy Cloud') {
sh '''
curl -sk  -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/deployments/meow-deploy/stop"
sleep 5
curl -sk  -X "DELETE" -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/deployments/meow-deploy"
sleep 5
curl -sk -X  "DELETE" -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/services/meow"
sleep 5
curl -sk  -X "DELETE" -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/images/huettermann-docker-registry.bintray.io/michaelhuettermann/alpine-tomcat7:1.0.0/hosts/2970cd1b-5571-6fda-3f21-1c6b19cd9ab1"
sleep 5
curl -sk  -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/images/huettermann-docker-registry.bintray.io/michaelhuettermann/alpine-tomcat7:1.0.0/hosts/2970cd1b-5571-6fda-3f21-1c6b19cd9ab1/pull"
sleep 20
curl -ski -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/services/" --data "@new-service.json"
sleep 5
curl -ski -X "POST"   -H "Authorization: Bearer ${BEARER}"  "https://${CLOUDIP}/api/v2/deployments/" --data "@create-deployment.json"
sleep 5
'''
    }
}
