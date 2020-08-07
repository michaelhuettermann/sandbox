node {

     stage('Build+run Docker image') {
        sh '''
 
docker run -i -v .:/e2e -w /e2e cypress/included:4.12.1 --browser chrome

       '''
    }



}


