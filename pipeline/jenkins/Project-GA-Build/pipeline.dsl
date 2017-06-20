node {
   stage('Prepare') {
       sh 'curl -O http://$ARTI3/simple/libs-releases-staging-local/com/huettermann/web/$version/all-$version.war'
   }

   stage('Certify WAR') {
       sh 'cp all-$version.war all-$version-GA.war'   
   }

   stage('Promote WAR to Bintray') {
       sh '''#!/bin/sh
       curl -u michaelhuettermann:${bintray_key} -X DELETE https://api.bintray.com/packages/huettermann/meow/cat/versions/$version
       curl -u michaelhuettermann:${bintray_key} -H "Content-Type: application/json" -X POST https://api.bintray.com/packages/huettermann/meow/cat/$version --data """{ "name": "$version", "desc": "desc" }"""
       curl -T "$WORKSPACE/all-$version-GA.war" -u michaelhuettermann:${bintray_key} -H "X-Bintray-Package:cat" -H "X-Bintray-Version:$version" https://api.bintray.com/content/huettermann/meow/
       curl -u michaelhuettermann:${bintray_key} -H "Content-Type: application/json" -X POST https://api.bintray.com/content/huettermann/meow/cat/$version/publish --data '{ "discard": "false" }'
       echo '''
    }
   
   stage('Certify Docker Image') {
       sh 'docker login -u michaelhuettermann -p ${bintray_key} huettermann-docker-registry.bintray.io'
       sh 'docker tag huttermann-docker-local.jfrog.io/michaelhuettermann/tomcat7:$version $BINTRAYREGISTRY/michaelhuettermann/tomcat7:$version'
   }
   
   stage('Promote Docker Image to Bintray') {
       sh 'docker push BINTRAYREGISTRY/michaelhuettermann/tomcat7:$version --disable-content-trust'
   }
}
