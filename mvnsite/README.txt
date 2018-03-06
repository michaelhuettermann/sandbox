mvn archetype:generate | grep site
=> org.apache.maven.archetypes:maven-archetype-site
mvn archetype:generate
102
groupId: com.conti
packageId: site
cd site
mvn site
cd target
zip -r site site
