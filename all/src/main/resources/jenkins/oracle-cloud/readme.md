
##### Deployment with/to Oracle Cloud Infrastructure

This example includes the Groovy based Jenkins build pipeline that takes a parameterized version number from the user, to identify a 
previously built Docker image, hosted at *Oracle Cloud Infrastructure Registry*, and provisions a container from it on 
*Oracle Container Service Classic*. *Twistlock* is utilized to inspect the images for known vulnerabilities.

Overview: the included components.
![DevOps cycle](pics/cycle.png) 

Oracle Cloud Infrastructure Registry: the Docker images are hosted.
![Image registry](pics/registry.png)

Twistlock: content of Docker registry is inspected.
![Container inspection](pics/inspect.png) 

Oracle Cloud Infrastructure Container Service Classic: Docker container runtime (service console).
![Container runtime](pics/container.png)  

##### Files
* **create-deployment.json**, define the deployment, according to Oracle Cloud API  
* **new-service.json**, define the service, according to Oracle Cloud API
* **pipeline.groovy**, the Jenkins pipeline groovy script, [Project Cloud Deploy here](http://129.213.104.3:8080/jenkins/blue/organizations/jenkins/pipelines/)

##### Parameters of pipeline script
`version`, the version to deploy.

##### Further information
* https://cloud.oracle.com/compute/
* https://docs.oracle.com/en/cloud/iaas/container-cloud/
* https://cloud.oracle.com/containers/registry/
* https://www.twistlock.com/
* https://jenkins.io/blog/2018/11/12/inspecting-binaries-with-jenkins/
* https://youtu.be/meC-u84o0xU