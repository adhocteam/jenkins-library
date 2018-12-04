import groovy.json.JsonOutput

def call(String name, Boolean failed=false) {
    def colorCode = failed ? '#FF0000' : '#118762'
    def str = JsonOutput.toJson([
        attachments: [[
            fallback: "status",
            color: colorCode,
            title: "status",
            title_link: env.RUN_DISPLAY_URL,
            text: "fullMsg"
        ]]]
    )
    def attach = new JSONArray(str)

    slackSend color: colorCode, channel: '@bob', message: "Deploy", attachments: attach
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


    // def json = new groovy.json.JsonBuilder()
    // json {
    //     attachments ([
    //         {
    //             fallback status
    //             color colorCode
    //             title status
    //             title_link env.RUN_DISPLAY_URL
    //             text fullMsg
    //             fields([
    //                 {
    //                             title 'Priority'
    //                             value 'High'
    //                             'short' false
    //                 }
    //             ])
    //             image_url 'http://my-website.com/path/to/image.jpg'
    //             thumb_url 'http://example.com/path/to/thumb.png'
    //             footer 'Slack API'
    //             footer_icon 'https://platform.slack-edge.com/img/default_application_icon.png'
    //             ts 123456789
    //         }

    //     ])
    // }

    // return JsonOutput.toJson([
    //     attachments: [[
    //         fallback: status,
    //         color: colorCode,
    //         title: status,
    //         title_link: env.RUN_DISPLAY_URL,
    //         text: fullMsg,
    //         fields: [
    //             [
    //                 title: ':pr:',
    //                 value: commitURL,
    //                 'short': false
    //             ],
    //             [
    //                 title: 'Committer',
    //                 value: author,
    //                 'short': true
    //             ],
    //             [
    //                 title: ':github:',
    //                 value: githubLink,
    //                 'short': true
    //             ]
    //         ],
    //     ]]]
    // )
}