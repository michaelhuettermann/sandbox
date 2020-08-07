node {

     stage('Build+run Docker image') {
        sh '''
 
docker run -i -v http://129.213.104.3:8080/jenkins/job/testDocker/2/execution/node/2/ws/all/src/main/resources/jenkins/cypress:/e2e -w /e2e cypress/included:4.12.1 --browser chrome

       '''
    }



}


