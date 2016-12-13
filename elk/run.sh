export JFROG="-v /home/michaelhuettermann/work/tools/jfrogcli/data/:/root/data"

# To get all the /dev/* devices needed for sshd and alike:
export DEV_MOUNTS="-v /dev/null:/dev/null -v /dev/urandom:/dev/urandom -v /dev/random:/dev/random"
export DEV_MOUNTS="${DEV_MOUNTS} -v /dev/full:/dev/full -v /dev/zero:/dev/zero"
### OPTIONAL -> if you got an etcd/helixdns instance running
export DNS_STUFF="--dns=172.17.0.3"
### OPTIONAL -> link carbon container to provide metrics target
export LINK="--link carbon:carbon"
### OPTIONAL -> if you want to store Elasticsearchs data outside 
export ES_PERSIST="-v ${HOME}/elasticsearch:/var/lib/elasticsearch"
### OPTIONAL -> To use a mapped in configuration directory
# if not used, the default will be used within the container
export LS_CONF="-v ${HOME}/logstash.d/:/etc/logstash/conf.d/"
### OPTIONAL -> map apache2 config into container
export AP_LOG="-v ${HOME}/var/log/apache2/:/var/log/apache2"
### OPTIONAL -> set the external port to something else then 80
export HTTP_PORT="-e HTTPPORT=8080 -p 8080:80"
### OPTIONAL -> To secure kibana and elasticsearch user/passwd could be set
# if a user is set and no passwd, the user will be set as password
export HTUSER=kibana
export HTPASSWD=secretpw

export DEV_MOUNTS="-v /dev/null:/dev/null -v /dev/urandom:/dev/urandom -v /dev/random:/dev/random"
export DEV_MOUNTS="${DEV_MOUNTS} -v /dev/full:/dev/full -v /dev/zero:/dev/zero"

#sysctl -w vm.max_map_count=262144
    
#docker run -d -h elk --name elk --privileged ${DEV_MOUNTS} qnib/elk:latest
docker run -p 5601:5601 -p 9200:9200 -p 5044:5044 $JFROG -it --name elk sebp/elk 
    
