node {

    def server 
    def rtMaven 
    def buildInfo
    def SERVER_ID 

    stage ('Setup') {
       sh "rm -rf /Users/michaelh/work/data/share/transfer"
       println flag
       println addprem
       server = Artifactory.server flag
       rtMaven = Artifactory.newMavenBuild()
       rtMaven.tool = 'M3.3.1'
       rtMaven.resolver server: server, releaseRepo: 'repo1', snapshotRepo: 'repo1'
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
    
    stage ('Produce RC') {
       sh "mvn com.huettermann:versionfetcher:1.0.0:release versions:set -DgenerateBackupPoms=false -f all/pom.xml"
    }
 
    stage ('Unit test') {
       sh "mvn clean test -f all/pom.xml"
    }

    stage ('Build env, with Puppet') {
       sh "puppet apply all/src/main/resources/puppet/init.pp"
    }
    
    stage ('Build env, with Chef') {
       if(addprem == "true") {
          sh "knife artifactory download poise-python 1.6.0"
       }
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
       timeout(time: 5, unit: 'MINUTES') {  
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
                   "target": "libs-release-local/com/huettermann/web/{1}/",
                   "props":  "where=koeln;owner=huettermann" 
               } ]         
           }
           """
       buildInfo = Artifactory.newBuildInfo()
       buildInfo.env.capture = true
       buildInfo=server.upload(uploadSpec)
    }

    stage ('Check Property/Plugin') {
       if(addprem == "true") {
           sh '''#!/bin/sh
           echo "Now the usage of Groovy plugin ..."
           echo hello > hello.txt
           cat hello.txt
           curl -u admin:AKCp2WXX7SDvcsmny528sSDnaB3zACkNQoscD8D1WmxhMV9gk6Wp8mVWC8bh38kJQbXagUT8Z -X PUT "http://localhost:8071/artifactory/simple/generic-local/hello.txt;qa=false" -T hello.txt
           rm hello.txt
           jfrog rt dl --url=http://localhost:8071/artifactory --apikey=AKCp2WXX7SDvcsmny528sSDnaB3zACkNQoscD8D1WmxhMV9gk6Wp8mVWC8bh38kJQbXagUT8Z generic-local/hello.txt
           echo "---------------------------------------"
           cat hello.txt
           echo "---------------------------------------"
           cat all/src/main/resources/artifactory/preventDownload.groovy
           echo "---------------------------------------"'''
       }
    }

    stage ('Build Docker image and run container') {
       if(flag == "saas") {
           sh '''
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
           echo "Building new Tomcat 7 container"
           docker build -f Dockerfile -t huttermann-docker-local.jfrog.io/michaelhuettermann/tomcat7:1.0.0 .
           echo "---------------------------------------"
           echo "Running Tomcat container"
           docker run -d -p 8002:8080 michaelhuettermann/tomcat7
           echo "---------------------------------------"
           echo "All images"
           docker images | grep tomcat7
           echo "---------------------------------------"
           echo "All active containers"
           docker ps
           sleep 10''' 
       } else if (flag == "il") {
           sh '''#!/bin/sh
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
           docker build -f Dockerfile-IL -t michaelhuettermann/tomcat7 .
           echo "---------------------------------------"
           echo "Running Tomcat container"
           docker run -d -p 8002:8080 michaelhuettermann/tomcat7
           echo "---------------------------------------"
           echo "All images"
           docker images | grep tomcat7
           echo "---------------------------------------"
           echo "All active containers"
           docker ps
           sleep 10
           echo "---------------------------------------"'''
      } else if (flag == "yoda") {
           sh '''#!/bin/sh
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
           cho "Building new Tomcat 7 container"
           docker build -f Dockerfile-FR -t michaelhuettermann/tomcat7 .
           echo "---------------------------------------"
           echo "Running Tomcat container"
           docker run -d -p 8002:8080 michaelhuettermann/tomcat7
           echo "---------------------------------------"
           echo "All images"
           docker images | grep tomcat7
           echo "---------------------------------------"
           echo "All active containers"
           docker ps
           sleep 10
           echo "---------------------------------------"'''
      }
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
       if(flag == "yoda") {
          def artDocker= Artifactory.docker("$DOCKER_UN_ADMIN", "$DOCKER_PW_ADMIN")
          def dockerInfo = artDocker.push("$ARTI1REGISTRY/michaelhuettermann/tomcat7:latest", "docker-dev-local")
          buildInfo.append(dockerInfo)
          server.publishBuildInfo(buildInfo)
       } else if (flag == "il") {
          def artDocker= Artifactory.docker("$DOCKER_UN_ADMIN", "$DOCKER_PW_ADMIN")
          def dockerInfo = artDocker.push("$ARTI2REGISTRY/michaelhuettermann/tomcat7:latest", "docker-dev-local")
          buildInfo.append(dockerInfo)
          server.publishBuildInfo(buildInfo)
       } else if (flag == "saas") {
          def artDocker= Artifactory.docker("$DOCKER_UN", "$DOCKER_PW")
          def dockerInfo = artDocker.push("$ARTI3REGISTRY/michaelhuettermann/tomcat7:1.0.0", "docker-local")
          buildInfo.append(dockerInfo)
          server.publishBuildInfo(buildInfo)
       }
    }
                    
    stage ('Xray Quality Gate') { 
    if(flag != "saas") {
       def scanConfig = [
       'buildName'      : buildInfo.name,
       'buildNumber'    : buildInfo.number,
       'failBuild'      : false
       ]
       def scanResult = server.xrayScan scanConfig
       echo scanResult as String
    }
    }
    
    stage ('Certify') {
       def matcher = manager.getLogMatcher(".*Hash (.*)\$") 
       if(matcher?.matches()) {    
           manager.addShortText(matcher.group(1).substring(0,8))
       }
       println "Labeled!"      
    }
    
    stage ('Tidy up') {
       if(addprem == "true") {
          sh "jfrog rt del --url=http://localhost:8071/artifactory --quiet=true --apikey=AKCp2WXX7SDvcsmny528sSDnaB3zACkNQoscD8D1WmxhMV9gk6Wp8mVWC8bh38kJQbXagUT8Z generic-local/hello.txt"
       }
    }
}

    def version() {
      def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
      matcher ? matcher[0][1] : null
    }



