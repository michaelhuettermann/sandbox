download {
    altResponse { request, responseRepoPath ->
        def artifactStatus = repositories.getProperties(responseRepoPath).getFirst('qa')
        if (artifactStatus && artifactStatus != 'true') {
            status = 403
            message = 'This artifact wasn\'t approved yet by QA.'
        }
    }
}
