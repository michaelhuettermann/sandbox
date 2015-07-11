#!/usr/bin/env bash

add-apt-repository ppa:webupd8team/java
apt-get -y update
echo "oracle-java7-installer shared/accepted-oracle-license-v1-1 boolean true" | debconf-set-selections
apt-get -y install oracle-java7-installer
echo "JAVA_HOME=/usr/lib/jvm/java-7-oracle" >> /etc/default/tomcat7
apt-get -y install curl
apt-get -y install puppet librarian-puppet 

puppet module install puppetlabs-stdlib
puppet module install puppetlabs-tomcat 



