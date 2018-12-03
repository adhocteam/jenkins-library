def call(String name, Bool failed=false) {
    // Remove whitespace and leading # character
    pr = sh(returnStdout: true, script: 'git log -1 --pretty=%B | grep -o "#[0-9]*"').trim()[1..-1]
    colorCode = failed ? "danger" : "good"
    status = failed ? "Failed" : "Success"

    // Strip .git from the end
    githubURL = env.GIT_URL[0..-5]

    if (pr) {
        msg = "(<${githubURL}|${name}) <${env.RUN_DISPLAY_URL}|Deploy> of <${githubURL}/pulls/${pr}|PR-${pr}> -- ${status}"
    }
    else {
        short = env.GIT_COMMIT[0..6]
        msg = "(<${githubURL}|${name}) <${env.RUN_DISPLAY_URL}|Deploy> of <${githubURL}/commit/${env.GIT_COMMIT}|Commit ${short}> -- ${status}"
    }

    slackSend color: colorCode, message: msg
}