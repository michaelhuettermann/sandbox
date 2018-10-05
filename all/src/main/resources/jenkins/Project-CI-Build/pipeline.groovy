node {

    def server
    def rtMaven
    def buildInfo
    def workspace
    def ver

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
            ver = version()
            if (ver) {
                echo "Version calculated=${ver}"
            }
        }, "Prepare env, with Puppet": {
            if (addprem == "true") {
                sh "puppet apply all/src/main/resources/puppet/init.pp"
            }
        }, "Prepare env, with Chef": {
            if (addprem == "true") {
                sh "knife artifactory download poise-python 1.6.0"
            }
        }, "Reset Docker": {
            sh '''#!/bin/sh
            echo "All images"
            docker images | grep tomcat7 || true
            echo "---------------------------------------"
            echo "All active containers"
            docker ps
            echo "Now a bit more catchy"
            docker ps --format "table {{.ID}}\\t{{.Status}}\\t{{.Image}}"
            echo "Stopping and removing working containers"
            docker stop $(docker ps -a | grep 8002 | cut -d " " -f1) || true
            docker rm $(docker ps -a | grep Exit | cut -d " " -f1) || true
            echo "Removing untagged Docker images"
            docker rmi -f $(docker images | grep "<none>" | awk "{print \$3}") || true
            echo "---------------------------------------"'''
       }
    }

    stage('Unit test') {
        try {
            sh "mvn clean package -f all/pom.xml -DcoverageSkip=false"
        } catch(err) {
            throw err
        } finally {
            step([$class: 'JUnitResultArchiver', testResults: 'all/target/surefire-reports/TEST-*.xml'])
        }
    }

    stage('Integration test') {
        if (addprem == "true") {
            rtMaven.deployer.deployArtifacts = false
            rtMaven.run pom: 'all/pom.xml', goals: 'integration-test -Pweb -DcoverageSkip=true'
            //rtMaven.run pom: 'all/pom.xml', goals: 'clean integration-test -Pnolibs,web -DcoverageSkip=false'
        }
    }

    stage('Reserve binary') {
        stash includes: 'all/target/*.war', name: 'war'
    }

    stage('Database migration') {
        if (addprem == "true") {
            sh "mvn clean install -Pdb flyway:clean flyway:init flyway:info flyway:migrate flyway:info -f all/pom.xml"
        }
    }

    stage('SonarQube analysis') {
        withCredentials([string(credentialsId: 'SQ_TOKEN', variable: 'SQ_TOKEN')]) {
            withSonarQubeEnv('Sonar') {
                sh "mvn sonar:sonar -Dsonar.projectKey=com.huettermann:all -Dsonar.organization=michaelhuettermann-github -Dsonar.login=$SQ_TOKEN -Dsonar.host.url=https://sonarcloud.io -f all/pom.xml -Dsonar.jacoco.reportPaths=${workspace}/all/target/jacoco.exec"
            }
            timeout(time: 5, unit: 'MINUTES') {
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
            
       ver=$(mvn -o -f all/pom.xml org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dartifact=com.huettermann:all -Dexpression=project.version|grep -Ev \'(^\\[|Download\\w+:)\')
       echo $ver > version.properties
        
       cd all/src/main/resources/docker/alpine
       echo "Building new Tomcat 7 container"
       docker build -f Dockerfile --build-arg ARTI=$ARTI --build-arg VER=$ver -t $ARTIREGISTRY/michaelhuettermann/alpine-tomcat7:$ver . 
       echo "---------------------------------------"
       echo "Running Tomcat container"
       docker run -d -p 8002:8080 $ARTIREGISTRY/michaelhuettermann/alpine-tomcat7:$ver
       echo "---------------------------------------"
       echo "All images"
       docker images | grep tomcat7
       echo "---------------------------------------"
       echo "All active containers"
       docker ps
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

    stage('Scan') {
        String version = new File("${workspace}/version.properties").text.trim()
        println "Scanning for version: ${version}"
        twistlockScan ca: '', cert: '', compliancePolicy: 'warn', \
         dockerAddress: 'unix:///var/run/docker.sock', \
         ignoreImageBuildTime: false, key: '', logLevel: 'true', \
         policy: 'warn', repository: 'huttermann-docker-local.jfrog.io/michaelhuettermann/alpine-tomcat7', \
         requirePackageUpdate: false, tag: "$version", timeout: 10
    }

    stage('Publish') {
        String version = new File("${workspace}/version.properties").text.trim()
        println "Publishing scan results for version: ${version}"
        twistlockPublish ca: '', cert: '', \
         dockerAddress: 'unix:///var/run/docker.sock', key: '', \
         logLevel: 'true', repository: 'huttermann-docker-local.jfrog.io/michaelhuettermann/alpine-tomcat7', tag: "$version", \
         timeout: 10
    }

    stage('Distribute Docker image') {
        withCredentials([usernamePassword(credentialsId: 'DOCKER', passwordVariable: 'DOCKER_PW', usernameVariable: 'DOCKER_UN')]) {
            echo "Push Docker image to Artifactory Docker Registry."
            String version = new File("${workspace}/version.properties").text.trim()
            println "Processing version: ${version}"
            server.username = "$DOCKER_UN"
            server.password = "$DOCKER_PW"
            def artDocker = Artifactory.docker server:server
            artDocker.addProperty("eat", "pizza").addProperty("drink", "beer")
            def dockerInfo = artDocker.push("$ARTI3REGISTRY/michaelhuettermann/alpine-tomcat7:$version", "docker-local")
            buildInfo.append(dockerInfo)
            server.publishBuildInfo(buildInfo)
        }
    }

    stage('Certify') {
        //archiveArtifacts artifacts: 'all/target/*.war', fingerprint: true
        def matcher = manager.getLogMatcher(".*Hash (.*)\$")
        if (matcher?.matches()) {
            manager.addShortText(matcher.group(1).substring(0, 8))
        }
        println "Labeled!"
    }

}

def version() {
    def matcher = readFile('all/pom.xml') =~ '<version>(.+)</version>'
    matcher ? matcher[0][1] : null
}
    