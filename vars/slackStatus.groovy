import groovy.json.JsonOutput

def call(String name, Boolean failed=false) {
    colorCode = failed ? '#FF0000' : '#118762'
    status = failed ? ":no_entry: ${name} Deployment Failed" : ":github-check: ${name} Deployment Success"

    // Strip .git from the end
    githubURL = env.GIT_URL[0..-5]
    repoName = (env.GIT_URL =~ /.*\/([^\/]+).git/)[0][1]
    githubLink = "<${githubURL}|${repoName}>"

    // Gets the last commit message and the committer's name
    shortMsg = sh(returnStdout: true, script: 'git log -1 --pretty="%s"').trim()
    fullMsg = sh(returnStdout: true, script: 'git log -1 --pretty="%B"').trim()
    author = sh(returnStdout: true, script: 'git log -1 --pretty="%an"').trim()
    // Look for a PR number in the message
    pr = shortMsg =~ /.*#([0-9]+).*/

    if (pr) {
        //Get the matched group with the number
        commitURL = "<${githubURL}/pulls/${pr[0][1]}|PR-${pr[0][1]}>"
    }
    else {
        commitURL = "<${githubURL}/commit/${env.GIT_COMMIT}|${env.GIT_COMMIT[0..6]}>"
    }

    attachment = JsonOutput.toJson([
        attachments: [[
            fallback: status,
            color: colorCode,
            title: status,
            title_link: env.RUN_DISPLAY_URL,
            text: fullMsg,
            fields: [
                [
                    title: ':pr:',
                    value: commitURL,
                    short: false
                ],
                [
                    title: 'Committer',
                    value: author,
                    short: true
                ],
                [
                    title: ':github:',
                    value: githubLink,
                    short: true
                ]
            ],
        ]]]
    )

    slackSend color: colorCode, attachments: msg
}