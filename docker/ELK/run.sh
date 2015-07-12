#!/bin/bash
service elasticsearch start
nohup /opt/logstash/bin/logstash -f /etc/logstash/conf.d/01-apache-input.conf > /dev/null 2>&1 &
kibana-4.0.1-linux-x64/bin/kibana