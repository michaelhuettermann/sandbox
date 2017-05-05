node {

    def server 
    def rtMaven 
    def buildInfo 
    
    stage ('Setup') {
       sh "rm -rf /Users/michaelh/work/data/share/transfer"
       //def SERVER_ID = 'saas'
       def SERVER_ID = 'il'
       //def SERVER_ID = '-844406945@1408787223604'
       server = Artifactory.server SERVER_ID

       rtMaven = Artifactory.newMavenBuild()
       rtMaven.tool = 'M3.3.1'
       rtMaven.resolver server: server, releaseRepo: 'repo1', snapshotRepo: 'repo1'
       //rtMaven.deployer server: server, releaseRepo: 'libs-local', snapshotRepo: 'libs-snapshots-local'
       rtMaven.deployer server: server, releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local'
 }

    stage ('Checkout') {
       git url: 'git@github.com:michaelhuettermann/sandbox.git'
       def gitCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
       echo "Hash ${gitCommit}"
       def workspace = pwd()
       echo "workspace=${workspace}"
    }

    stage ('Check preconditions') {
       parallel "Sanity check 1": {
          echo "1"
       }, "Sanity check 2": {
          echo "2i"
       }, "Sanity check 3": {
          echo "3"
       }, "Sanity check 4": {
          echo "4a"
          echo "4b"
          echo "4c"
       }
    }
    
    stage ('Unit test') {
       sh "mvn clean test -f all/pom.xml"
    }
    
    stage ('Integration test') {
       rtMaven.deployer.deployArtifacts = false
       rtMaven.run pom: 'all/pom.xml', goals: 'clean integration-test -Pweb'
    }

    stage ('Build env, with Puppet') {
       sh "puppet apply all/src/main/resources/puppet/init.pp"
    }

    stage ('Copy WAR') {
       sh "cp all/target/*.war /Users/michaelh/work/data/share/transfer/"
    }

    stage ('Fetch version') {
       def v = version()
       if (v) {
          echo "Version=${v}"
       }
    }

    stage ('Database migration') {
       sh "mvn clean install -Pdb flyway:clean flyway:init flyway:info flyway:migrate flyway:info -f all/pom.xml"
    }
    
    stage('SonarQube analysis') {
       withSonarQubeEnv('Sonar') {
         sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.3.0.603:sonar -f all/pom.xml -Dsonar.projectKey=com.huettermann:all:master -Dsonar.login=$SONAR_UN -Dsonar.password=$SONAR_PW -Dsonar.language=java -Dsonar.sources=. -Dsonar.tests=. -Dsonar.test.inclusions=**/*Test*/** -Dsonar.exclusions=**/*Test*/**'
       }
    }
   
    stage("SonarQube Quality Gate"){
       timeout(time: 1, unit: 'HOURS') {  
          def qg = waitForQualityGate()  
          if (qg.status != 'OK') {
             error "Pipeline aborted due to quality gate failure: ${qg.status}"
          }
       }
    }

    stage ('Build Docker image and run container') {
sh '''#!/bin/sh
    cp /Users/michaelh/work/data/share/transfer/*.war .
rm -f index.html
cd all/src/main/resources/docker/Tomcat7
echo "All images"
docker images | grep tomcat7
echo "---------------------------------------"
echo "All active containers"
docker ps
echo "---------------------------------------"
echo "Stopping and removing containers"
docker stop $(docker ps -a | grep 8002 | cut -d " " -f1)
docker rm $(docker ps -a | grep Exit | cut -d " " -f1)
echo "---------------------------------------"
echo "Building new Tomcat 7 container"
docker build -t tomcat7 .
echo "---------------------------------------"
echo "Running Tomcat container"
docker run -d -p 8002:8080 -v $WORKSPACE:/shareme tomcat7
echo "---------------------------------------"
echo "All images"
docker images | grep tomcat7
echo "---------------------------------------"
echo "All active containers"
docker ps
sleep 10
echo "---------------------------------------"'''
    }
    
    stage ('Check Web app in Container') {
sh '''#!/bin/sh
pwd
echo "--------------------------------"
echo "Check start page available"
curl -v -o index.html http://localhost:8002/all/
echo "--------------------------------"
echo "Downloaded page: " 
cat index.html
echo "--------------------------------"
echo "Template page: "
cat all/src/main/resources/docker/template/index.html
echo "--------------------------------"
diff=$(diff index.html all/src/main/resources/docker/template/index.html)
length=$(echo ${#diff})
  
if [ $length -eq 0 ]
  then echo "Result: ok"
  else exit 1
fi

echo "---------------------------------------"'''
    }
    
    stage ('Label') {
      def matcher = manager.getLogMatcher(".*Hash (.*)\$") 
      if(matcher?.matches()) {    
         manager.addShortText(matcher.group(1).substring(0,8))
      }
      println "Labeled!"
    }
    
    stage ('Distribute WAR') {
          echo "Deploy Deployment Unit to Artifactory."
          def uploadSpec = """
    {
    "files": [
        {
            "pattern": "all/target/all-(*).war",
            "target": "libs-snapshot-local/com/huettermann/web/{1}/"
        }
      ]
    }
    """
    buildInfo = Artifactory.newBuildInfo()
    buildInfo.env.capture = true
    buildInfo=server.upload(uploadSpec)
    //server.publishBuildInfo(buildInfo)
    

    
    sh '''#!/bin/sh

echo buildinfo >> all/buildinfo2.json    
curl -X PUT -u$ARTIFACTORY_UN:$ARTIFACTORY_PW -H "Content-type: application/json" -T all/buildinfo.json "https://xray-demo.jfrog.io/artifactory/api/build"
echo "---------------------------------------"'''  

    
    }
    
    stage ('Inspect WAR') {
    def scanConfig = [
    'buildName'      : buildInfo.name,
    'buildNumber'    : buildInfo.number,
    'failBuild'      : false
    ]
    def scanResult = server.xrayScan scanConfig
    echo scanResult as String
    }
     
    stage ('Distribute Docker image') {
       echo "Push Docker image to Artifactory Docker Registry."
sh '''#!/bin/sh
docker login xray-demo-docker-local.jfrog.io -u="$DOCKER_UN_ADMIN" -p="$DOCKER_PW_ADMIN"
docker tag michaelhuettermann/tomcat7 xray-demo-docker-local.jfrog.io/michaelhuettermann/tomcat7
docker push xray-demo-docker-local.jfrog.io/michaelhuettermann/tomcat7
docker logout xray-demo-docker-local.jfrog.io
echo "---------------------------------------"'''
    }

    
}


    def version() {
      def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
      matcher ? matcher[0][1] : null
    }


    def buildinfo = '
  
    {
    "version": "1.0.1",
    "name": "Pipeline_conf_xray",
    "number": "6",
    "type": "GENERIC",
    "buildAgent": {
        "name": "Pipeline",
        "version": "Pipeline"
    },
    "agent": {
        "name": "hudson",
        "version": "2.32.3"
    },
    "started": "2017-03-21T13:15:57.692+0200",
    "durationMillis": 7810,
    "principal": "anonymous",
    "artifactoryPrincipal": "admin",
    "artifactoryPluginVersion": "2.9.2",
    "url": "http://localhost:8080/job/Pipeline_conf_xray/6/",
    "licenseControl": {
        "runChecks": false,
        "includePublishedArtifacts": false,
        "autoDiscover": false,
        "scopesList": "",
        "licenseViolationsRecipientsList": ""
    },
    "buildRetention": {
        "count": -1,
        "deleteBuildArtifacts": false,
        "buildNumbersNotToBeDiscarded": []
    },
    "modules": [{
        "id": "Pipeline_conf_xray",
        "artifacts": [{
            "type": "zip",
            "sha1": "b04f3ee8f5e43fa3b162981b50bb72fe1acabb33",
            "md5": "76cdb2bad9582d23c1f6f4d868218d6c",
            "name": "ArtifactoryPipelineNoProps.zip"
        }, {
            "type": "zip",
            "sha1": "b04f3ee8f5e43fa3b162981b50bb72fe1acabb33",
            "md5": "76cdb2bad9582d23c1f6f4d868218d6c",
            "name": "ArtifactoryPipeline.zip"
        }, {
            "type": "jar",
            "sha1": "657304d2403c13c16a12e2187554694605275f6f",
            "md5": "186398b199d5442b43b0ec702f95c0db",
            "name": "xwork-2.0.6.jar"
        }],
        "dependencies": [{
            "sha1": "b04f3ee8f5e43fa3b162981b50bb72fe1acabb33",
            "md5": "76cdb2bad9582d23c1f6f4d868218d6c",
            "id": "ArtifactoryPipeline.zip"
        }]
    }],
    "buildDependencies": [],
    "governance": {
        "blackDuckProperties": {
            "runChecks": false,
            "includePublishedArtifacts": false,
            "autoCreateMissingComponentRequests": false,
            "autoDiscardStaleComponentRequests": false
        }
    }
}'
