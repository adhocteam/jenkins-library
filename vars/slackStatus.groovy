def call(String name, Boolean failed=false) {
    colorCode = failed ? 'danger' : 'good'
    status = failed ? 'Failed' : 'Success'

    // Strip .git from the end
    githubURL = env.GIT_URL[0..-5]

    // Gets the last commit message and the committer's name
    commitMsg = sh(returnStdout: true, script: 'git log -1 --pretty="%s by %an"').trim()
    // Look for a PR number in the message
    pr = commitMsg =~ /.*#([0-9]*).*/

    if (pr) {
        //Get the matched group with the number
        prNum = pr[0][1]
        msg = "(<${githubURL}|${name}>) <${env.RUN_DISPLAY_URL}|Deploy> of <${githubURL}/pulls/${prNum}|${commitMsg}> -- ${status}"
    }
    else {
        msg = "(<${githubURL}|${name}>) <${env.RUN_DISPLAY_URL}|Deploy> of <${githubURL}/commit/${env.GIT_COMMIT}|${commitMsg}> -- ${status}"
    }

    slackSend color: colorCode, message: msg
}