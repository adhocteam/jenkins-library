import groovy.json.JsonOutput

def call(String name, Boolean failed=false) {
    def colorCode = failed ? '#FF0000' : '#118762'
    def attachment = getAttachment(name, failed)
    echo attachment
    slackSend color: colorCode, attachments: attachment.trim()
}

@NonCPS
def getAttachment(String name, Boolean failed=false) {
    def colorCode = failed ? '#FF0000' : '#118762'
    def status = failed ? ":no_entry: ${name} Deployment Failed" : ":github-check: ${name} Deployment Success"

    // Strip .git from the end
    def githubURL = env.GIT_URL[0..-5]
    def repoName = (env.GIT_URL =~ /.*\/([^\/]+).git/)[0][1]
    def githubLink = "<${githubURL}|${repoName}>"

    // Gets the last commit message and the committer's name
    def shortMsg = sh(returnStdout: true, script: 'git log -1 --pretty="%s"').trim()
    def fullMsg = sh(returnStdout: true, script: 'git log -1 --pretty="%B"').trim()
    def author = sh(returnStdout: true, script: 'git log -1 --pretty="%an"').trim()
    // Look for a PR number in the message
    def pr = shortMsg =~ /.*#([0-9]+).*/

    def commitURL = "<${githubURL}/commit/${env.GIT_COMMIT}|${env.GIT_COMMIT[0..6]}>"
    if (pr) {
        //Get the matched group with the number
        commitURL = "<${githubURL}/pulls/${pr[0][1]}|PR-${pr[0][1]}>"
    }

    return JsonOutput.toJson([
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
}