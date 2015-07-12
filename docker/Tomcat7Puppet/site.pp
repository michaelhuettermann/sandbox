class { 'tomcat': } ->
tomcat::instance { 'install':
  source_url => 'http://archive.apache.org/dist/tomcat/tomcat-7/v7.0.62/bin/apache-tomcat-7.0.62.tar.gz'
} ->
exec { 'run':
  command     => '/opt/apache-tomcat/bin/catalina.sh start' 
} ->
tomcat::war { 'all.war': 
        catalina_base => '/opt/apache-tomcat',
        war_source => 'http://dl.bintray.com/michaelhuettermann/meow/all-1.0.0-GA.war',
}