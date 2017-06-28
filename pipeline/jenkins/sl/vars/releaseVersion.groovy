dev call(args) {
    sh "mvn com.huettermann:versionfetcher:1.0.0:release versions:set -DgenerateBackupPoms=false -f ${args}"
}
