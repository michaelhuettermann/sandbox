node {

    def server 
    def rtMaven 
    def buildInfo
    def SERVER_ID 

    stage ('Setup') {
       sh "rm -rf /Users/michaelh/work/data/share/transfer"
       //SERVER_ID = 'saas'
       //SERVER_ID = 'yoda'
       SERVER_ID = 'il'
       
       println SERVER_ID
       println flag
       
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

    stage ('Build env, with Puppet') {
       sh "puppet apply all/src/main/resources/puppet/init.pp"
    }
    
    stage ('Integration test') {
       rtMaven.deployer.deployArtifacts = false
       rtMaven.run pom: 'all/pom.xml', goals: 'clean integration-test -Pweb'
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
    server.publishBuildInfo(buildInfo)
   }
          
    stage ('Xray Quality Gate') {
    def scanConfig = [
    'buildName'      : buildInfo.name,
    'buildNumber'    : buildInfo.number,
    'failBuild'      : false
    ]
    def scanResult = server.xrayScan scanConfig
    echo scanResult as String
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
docker build -t michaelhuettermann/tomcat7 .
echo "---------------------------------------"
echo "Running Tomcat container"
docker run -d -p 8002:8080 michaelhuettermann/tomcat7
//docker run -d -p 8002:8080 -v $WORKSPACE:/shareme michaelhuettermann/tomcat7
echo "---------------------------------------"
echo "All images"
docker images | grep tomcat7
echo "---------------------------------------"
echo "All active containers"
docker ps
sleep 10
echo "---------------------------------------"'''
    }
    
    stage ('Sanity check Webapp') {
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
    
    stage ('Distribute Docker image') {
       echo "Push Docker image to Artifactory Docker Registry."
if(SERVER_ID == "yoda") {
sh '''#!/bin/sh
docker login http://yodafrog.sas.jfrog.internal:5001 -u="$DOCKER_UN_ADMIN" -p="$DOCKER_PW_ADMIN"
docker tag michaelhuettermann/tomcat7 yodafrog.sas.jfrog.internal:5001/michaelhuettermann/tomcat7
docker push yodafrog.sas.jfrog.internal:5001/michaelhuettermann/tomcat7
docker logout http://yodafrog.sas.jfrog.internal:5001
echo "---------------------------------------"'''
} else if (SERVER_ID == "il") {
sh '''#!/bin/sh
docker login xray-demo-docker-local.jfrog.io -u="$DOCKER_UN_ADMIN" -p="$DOCKER_PW_ADMIN"
docker tag michaelhuettermann/tomcat7 xray-demo-docker-local.jfrog.io/michaelhuettermann/tomcat7
docker push xray-demo-docker-local.jfrog.io/michaelhuettermann/tomcat7
docker logout xray-demo-docker-local.jfrog.io
echo "---------------------------------------"'''
}
    }

    stage ('Label') {
      def matcher = manager.getLogMatcher(".*Hash (.*)\$") 
      if(matcher?.matches()) {    
         manager.addShortText(matcher.group(1).substring(0,8))
      }
      println "Labeled!"
    }
    
}

    def version() {
      def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
      matcher ? matcher[0][1] : null
    }



