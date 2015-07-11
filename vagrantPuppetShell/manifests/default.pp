class { 'tomcat': } ->
tomcat::instance { 'install':
  source_url => 'http://mirrors.ae-online.de/apache/tomcat/tomcat-7/v7.0.62/bin/apache-tomcat-7.0.62.tar.gz'
} ->
exec { 'run':
  command     => '/opt/apache-tomcat/bin/catalina.sh start' 
} 
