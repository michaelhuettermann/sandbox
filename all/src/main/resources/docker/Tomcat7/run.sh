#!/bin/bash

# chmod 755 /shareme/*.war
# cp /shareme/*.war /var/lib/tomcat7/webapps/all.war  

/etc/init.d/tomcat7 start

# The container will run as long as the script is running, that's why
# we need something long-lived here
exec tail -f /var/log/tomcat7/catalina.out 
