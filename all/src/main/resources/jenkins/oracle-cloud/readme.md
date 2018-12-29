
##### Deployment with/to Oracle Cloud 

This example promotes a previously built Docker image toward production. 
The dockerized Java EE web application runs on Linux Alpine and Tomcat, with OpenJDK, and it is running 
[here](http://129.150.204.182:8002/all/). This is part of a holistic workflow spanning several pipelines including build and test (multiple test 
categories) of the web application, package it with 
Docker, run different checks on source code and binaries, derive frozen versions by dynamically nailing down Maven snapshot versions, and 
allow to cherry-pick RC and GA versions. In this use case, GA versions of the application are pushed to Oracle Cloud, and are then handled by
the pipeline and toolchain you find in this directory.

###### Overview: the included components.
![DevOps cycle](pics/cycle.png) 

The Docker images are hosted on *Oracle Cloud Infrastructure Registry* and from there they are deployed to the runtime environment that is 
*Oracle Container Service Classic*. *Twistlock* is utilized to inspect the images (with its layers, transitively) in the registry for known 
vulnerabilities. In order to demo the open architecture and feature-rich Oracle Cloud API, the parts are glued together by the Oracle Cloud REST API, 
however, you can of course also use any other integration depending on your basic conditions and individual requirements. 
 
###### Oracle Cloud Infrastructure Registry: the Docker images are hosted.
![Image registry](pics/registry.png)

The Docker images are hosted on configured Docker registries, on Oracle Cloud. The Docker image which is managed with this pipeline is an example of a primitive that can be aggregated to more complex setups. The Dockerfile can be found 
[here](https://github.com/michaelhuettermann/sandbox/blob/master/all/src/main/resources/docker/alpine/Dockerfile).  
 

###### Twistlock: content of Docker registry is inspected.
![Container inspection](pics/inspect.png) 

Twistlock integrates with Oracle Cloud and inspects the Docker images managed in the configured Docker registries. This is a valuable complementary check, in addition
to build-time analyzes, also part of the overall workflow.

###### Oracle Cloud Infrastructure (OCI) Container Service Classic: Docker container runtime (service console).
![Container runtime](pics/container.png)  

To provision the Docker image and run it on Oracle Cloud, a Groovy based Jenkins build job is used. 
The Jenkins pipeline, that maps to the sources located in this directory, can be viewed 
[as the Project Cloud Deploy here](http://129.213.104.3:8080/jenkins/blue/organizations/jenkins/pipelines/). OCI Container Classic uses units such as 
*stacks*, *services*, *deployments*, to aggregate and manage items. The script does stop an existing deployment, if available, creates a new service and deployment, and 
finally runs the Docker image. 

##### Files
* **create-deployment.json**, define the deployment, according to Oracle Cloud API  
* **new-service.json**, define the service, according to Oracle Cloud API
* **pipeline.groovy**, the Jenkins pipeline groovy script

Please note, the scripts are dynamically applying the current version. In this pipeline, the target version is injected by the user while triggering the pipeline. 

##### Parameters of pipeline script
`version`, the version of the Docker image to deploy.

##### Further information
* https://cloud.oracle.com/compute/ Oracle Cloud Infrastructure Compute 
* https://docs.oracle.com/en/cloud/iaas/container-cloud/ Oracle Container Service Classic
* https://cloud.oracle.com/containers/registry/ Oracle Cloud Infrastructure Registry
* https://www.twistlock.com/ Twistlock
* https://jenkins.io/blog/2018/11/12/inspecting-binaries-with-jenkins/ Inspecting sources and binaries
* https://youtu.be/meC-u84o0xU Inspecting sources and binaries
* https://jenkins.io/blog/2017/04/18/continuousdelivery-devops-sonarqube/ Delivery pipelines, with SonarQube and Artifactory
* https://jenkins.io/blog/2017/07/05/continuousdelivery-devops-artifactory/ Delivery pipelines: how to promote Java EE and Docker binaries toward production
* https://github.com/michaelhuettermann/sandbox/tree/master/all/src/main/resources/jenkins The Jenkins pipelines
* https://github.com/michaelhuettermann/sandbox/tree/master/all The self-contained web app
