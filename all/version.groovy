import groovy.xml.StreamingMarkupBuilder
import groovy.util.XmlNodePrinter
import org.codehaus.groovy.tools.xml.DomToGroovy
import groovy.xml.XmlUtil

def dir = new File(project.basedir, 'pom.xml')

def pom = new XmlSlurper().parse(dir)

def version = pom.version.toString()
println "Version of pom: ${pom.version}"
