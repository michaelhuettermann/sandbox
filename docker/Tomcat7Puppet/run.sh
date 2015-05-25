#!/bin/bash
#apply the manifest locally
puppet apply /root/site.pp

# The container will run as long as the script is running, i.e. call sth long-living here
exec tail -f /opt/apache-tomcat/logs/catalina.out 


