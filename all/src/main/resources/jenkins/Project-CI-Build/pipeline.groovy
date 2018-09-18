node {

    def server
    def rtMaven
    def buildInfo
    def workspace
    def v

    @Library('Util') _

    stage('Initialize') {
        sh "rm -f index.html || true"
        println flag
        println addprem
        server = Artifactory.server flag
        rtMaven = Artifactory.newMavenBuild()
        rtMaven.tool = 'M3.5.4'
        rtMaven.resolver server: server, releaseRepo: 'repo1', snapshotRepo: 'repo1'
        rtMaven.deployer server: server, releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local'
    }

    stage('Checkout') {
        git url: 'git@github.com:michaelhuettermann/sandbox.git'
        def gitCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
        echo "Hash ${gitCommit}"
        workspace = pwd()
        echo "workspace=${workspace}"
    }

    stage('Setup') {
        parallel "Release version": {
            releaseVersion 'all/pom.xml'
            v = version()
            if (v) {
                echo "Version calculated=${v}"
            }
        //}, "Prepare env, with Puppet": {
        //   sh "puppet apply all/src/main/resources/puppet/init.pp"
        //}, "Prepare env, with Chef": {
        //    if (addprem == "true") {
        //        sh "knife artifactory download poise-python 1.6.0"
        //    }
        //}, "Reset Docker": {
        //    sh '''#!/bin/sh
        //    echo "All images"
        //    sudo docker images | grep tomcat7 || true
        //    echo "---------------------------------------"
        //    echo "All active containers"
        //    docker ps
        //    echo "Stopping and removing containers"
        //    docker stop $(sudo docker ps -a | grep 8002 | cut -d " " -f1) || true
        //    docker rm $(sudo docker ps -a | grep Exit | cut -d " " -f1) || true
        //    echo "Removing untagged Docker images"
        //    docker rmi -f $(sudo docker images | grep "<none>" | awk "{print \$3}") || true
        //    echo "---------------------------------------"'''
       //}, "Run Socat": {
       //     sh '''#!/bin/sh
       //     so=$(docker ps | grep socat)
       //     echo $so
       //     if [ -n "$so" ]
       //     then
       //     echo "Bytestream container already running ..."
       //     else
       //     docker run -d -v /var/run/docker.sock:/var/run/docker.sock -p 127.0.0.1:1234:1234 bobrik/socat TCP-LISTEN:1234,fork UNIX-CONNECT:/var/run/docker.sock
       //     fi
       //     echo "---------------------------------------"'''
       }
    }

    stage('Unit test') {
        try {
            sh "mvn clean test -f all/pom.xml -DcoverageSkip=true"
        } catch(err) {
            throw err
        } finally {
            step([$class: 'JUnitResultArchiver', testResults: 'all/target/surefire-reports/TEST-*.xml'])
        }
    }

    stage('Integration test') {
        rtMaven.deployer.deployArtifacts = false
        rtMaven.run pom: 'all/pom.xml', goals: 'clean integration-test -Pnolibs,web -DcoverageSkip=true'
    }

    stage('Reserve binary') {
        stash includes: 'all/target/*.war', name: 'war'
    }

    stage('Database migration') {
        //sh "mvn clean install -Pdb flyway:clean flyway:init flyway:info flyway:migrate flyway:info -f all/pom.xml"
    }

    stage('SonarQube analysis') {
        withCredentials([string(credentialsId: 'SQ_TOKEN', variable: 'SQ')]) {
            withSonarQubeEnv('Sonar') {
                sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.4.0.905:sonar -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=$SQ -f all/pom.xml -Dsonar.projectKey=com.huettermann:all:master -Dsonar.junit.reportPaths=target/surefire-reports -Dsonar.jacoco.reportPaths=target/jacoco.exec -Dsonar.userHome=/mnt/fsdata/sonarqube -Dsonar.working.directory=/mnt/fsdata/sonarqube'
            }
            timeout(time: 2, unit: 'MINUTES') {
                def qg = waitForQualityGate()
                if (qg.status != 'OK') {
                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                }
            }
        }
    }

    stage('Distribute WAR') {
        unstash 'war'
        echo "Deploy Deployment Unit to Artifactory."
        def uploadSpec = """
       {
           "files": [
               {
                   "pattern": "all/target/all-(*).war",
                   "target": "libs-release-local/com/huettermann/web/{1}/",
                   "props":  "eat=pizza;drink=beer" 
               } ]         
           }
           """
        buildInfo = Artifactory.newBuildInfo()
        buildInfo.env.capture = true
        buildInfo = server.upload(uploadSpec)
    }

    //stage('Check Property/Plugin') {
    //    if (addprem == "true") {
    //        sh '''#!/bin/sh
    //       echo "Now the usage of Groovy plugin ..."
    //       echo hello > hello.txt
    //       cat hello.txt
    //       curl -u admin:AKCp2WXX7SDvcsmny528sSDnaB3zACkNQoscD8D1WmxhMV9gk6Wp8mVWC8bh38kJQbXagUT8Z -X PUT "http://localhost:8071/artifactory/simple/generic-local/hello.txt;qa=false" -T hello.txt
    //       rm hello.txt
    //       jfrog rt dl --url=http://localhost:8071/artifactory --apikey=AKCp2WXX7SDvcsmny528sSDnaB3zACkNQoscD8D1WmxhMV9gk6Wp8mVWC8bh38kJQbXagUT8Z generic-local/hello.txt
    //       echo "---------------------------------------"
    //       cat hello.txt
    //       echo "---------------------------------------"
    //       cat all/src/main/resources/artifactory/preventDownload.groovy
    //       echo "---------------------------------------"'''
    //    }
    //}

    stage('Build Docker image and run container') {
        sh '''
       if [ "$flag" == "saas" ]; then
           ARTI=$ARTI3
           ARTIREGISTRY=$ARTI3REGISTRY
       fi
       if [ "$flag" == "ra2" ]; then
           ARTI=$ARTI2
           ARTIREGISTRY=$ARTI2REGISTRY
       fi
       if [ "$flag" == "ra1" ]; then
           ARTI=$ARTI1
           ARTIREGISTRY=$ARTI1REGISTRY
       fi
       
       ver=$(mvn -f all/pom.xml org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dartifact=com.huettermann:all -Dexpression=project.version|grep -Ev \'(^\\[|Download\\w+:)\')
       echo $ver > version.properties
        
       cd all/src/main/resources/docker/alpine
       echo "Building new Tomcat 7 container"
       sudo docker build -f Dockerfile --build-arg ARTI=$ARTI --build-arg VER=$ver -t $ARTIREGISTRY/michaelhuettermann/alpine-tomcat7:$ver . 
       echo "---------------------------------------"
       echo "Running Tomcat container"
       sudo docker run -d -p 8002:8080 $ARTIREGISTRY/michaelhuettermann/alpine-tomcat7:$ver
       echo "---------------------------------------"
       echo "All images"
       sudo docker images | grep tomcat7
       echo "---------------------------------------"
       echo "All active containers"
       sudo docker ps
       sleep 10'''
    }

    stage('Sanity check Webapp') {
        sh '''#!/bin/sh
      pwd
      echo "--------------------------------"
      echo "Check start page available"
      curl -v -o index.html http://localhost:8002/all/message?param=world
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

    stage('Distribute Docker image') {
        withCredentials([usernamePassword(credentialsId: 'DOCKER', passwordVariable: 'DOCKER_PW', usernameVariable: 'DOCKER_UN')]) {
            echo "Push Docker image to Artifactory Docker Registry."
            String version = new File("${workspace}/version.properties").text.trim()
            println "Processing version: ${version}"
            server.username = "$DOCKER_UN"
            server.password = "$DOCKER_PW"
            def artDocker = Artifactory.docker server:server, host: "tcp://127.0.0.1:1234"

            artDocker.addProperty("eat", "pizza").addProperty("drink", "beer")
            def dockerInfo = artDocker.push("$ARTI3REGISTRY/michaelhuettermann/alpine-tomcat7:${version}", "docker-local")
            buildInfo.append(dockerInfo)
            server.publishBuildInfo(buildInfo)
        }
    }

    //stage('Binary inspect') {
    //    if (flag != "saas") {
    //        def scanConfig = [
    //                'buildName'  : buildInfo.name,
    //                'buildNumber': buildInfo.number,
    //                'failBuild'  : false
    //        ]
    //        def scanResult = server.xrayScan scanConfig
    //        echo scanResult as String
    //    }
    //}

    stage('Certify') {
        //archiveArtifacts artifacts: 'all/target/*.war', fingerprint: true
        def matcher = manager.getLogMatcher(".*Hash (.*)\$")
        if (matcher?.matches()) {
            manager.addShortText(matcher.group(1).substring(0, 8))
        }
        println "Labeled!"
    }

    //stage('Tidy up') {
    //    if (addprem == "true") {
    //        sh "jfrog rt del --url=http://localhost:8071/artifactory --quiet=true --apikey=AKCp2WXX7SDvcsmny528sSDnaB3zACkNQoscD8D1WmxhMV9gk6Wp8mVWC8bh38kJQbXagUT8Z generic-local/hello.txt"
    //    }
    //}

}

def version() {
    def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
    matcher ? matcher[0][1] : null
}
    