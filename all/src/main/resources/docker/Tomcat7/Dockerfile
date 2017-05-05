FROM michaelhuettermann/infra
MAINTAINER Michael Huettermann

RUN apt-get -y update

# Install curl
RUN apt-get -y install curl

# Install tomcat
RUN apt-get -y install tomcat7
RUN echo "JAVA_HOME=/usr/lib/jvm/java-7-oracle" >> /etc/default/tomcat7

ADD https://xray-demo.jfrog.io/artifactory/list/libs-snapshot-local/com/huettermann/web/1.0.0-SNAPSHOT/all-1.0.0-SNAPSHOT.war /var/lib/tomcat7/webapps/
#ADD /Users/michaelh/work/data/share/transfer/all-1.0.0-SNAPSHOT.war /var/lib/tomcat7/webapps/

RUN chmod 755 /var/lib/tomcat7/webapps/*.war

ADD run.sh /root/run.sh
RUN chmod +x /root/run.sh

EXPOSE 8080

CMD ["/root/run.sh"]
 
