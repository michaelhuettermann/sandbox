#!/bin/sh

echo "Stopping and removing containers"
docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)
echo "---------------------------------------"
echo "Building new Tomcat 7 container"
docker build -t tomcat7 .
echo "---------------------------------------"
echo "Running Tomcat container"
docker run -d -p 8888:8080 -v /home/michaelhuettermann/work/ws-git/sandbox/all/src/main/resources/docker/Tomcat7:/shareme tomcat7
echo "---------------------------------------"
echo "All images"
docker images
echo "---------------------------------------"
echo "All active containers"
docker ps
sleep 10
echo "---------------------------------------"