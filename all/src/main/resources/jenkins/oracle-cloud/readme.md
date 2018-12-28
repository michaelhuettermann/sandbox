
##### Deployment with/to Oracle Cloud 

This example promotes and runs a previously built Docker image, hosted on *Oracle Cloud Infrastructure Registry*, to the runtime environment that is 
*Oracle Container Service Classic*. *Twistlock* is utilized to inspect the images (with its layers, transitively) in the registry for known vulnerabilities. 
The Docker image serves as an example of a primitive that can be aggregated more complex setups, and does bundle OpenJDK 8, 
on Alpine Linux, with Tomcat 9, and ships a Java EE WAR deployment unit, see 
[here](https://github.com/michaelhuettermann/sandbox/blob/master/all/src/main/resources/docker/alpine/Dockerfile). 
The resulting web application is running [here](http://129.150.204.182:8002/all/). In this case the glue is achieved by 
Jenkins pipeline, but of course you can take any available automation engine to integrate with the Oracle Cloud REST API.

###### Overview: the included components.
![DevOps cycle](pics/cycle.png) 

###### Oracle Cloud Infrastructure Registry: the Docker images are hosted.
![Image registry](pics/registry.png)

###### Twistlock: content of Docker registry is inspected.
![Container inspection](pics/inspect.png) 

###### Oracle Cloud Infrastructure Container Service Classic: Docker container runtime (service console).
![Container runtime](pics/container.png)  

##### Files
* **create-deployment.json**, define the deployment, according to Oracle Cloud API  
* **new-service.json**, define the service, according to Oracle Cloud API
* **pipeline.groovy**, the Jenkins pipeline groovy script, [Project Cloud Deploy here](http://129.213.104.3:8080/jenkins/blue/organizations/jenkins/pipelines/)

##### Parameters of pipeline script
`version`, the version of the Docker image to deploy.

##### Further information
* https://cloud.oracle.com/compute/
* https://docs.oracle.com/en/cloud/iaas/container-cloud/
* https://cloud.oracle.com/containers/registry/
* https://www.twistlock.com/
* https://jenkins.io/blog/2018/11/12/inspecting-binaries-with-jenkins/
* https://youtu.be/meC-u84o0xU
