import org.artifactory.build.DetailedBuildRun
import org.jfrog.build.api.Build

build {
    beforeSave { DetailedBuildRun buildRun ->
        Build build = buildRun.build
        build.modules.each { m ->
            log.debug "property: ${m.properties}"
        }
    }
}
