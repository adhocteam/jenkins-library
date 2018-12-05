import net.sf.json.JSONArray
import net.sf.json.JSONObject

def call(Map config) {
    def name = config.get('name', 'reponame')
    def failed = config.get('failed', false)

    def colorCode = failed ? '#FF0000' : '#118762'
    def status = failed ? ":no_entry: ${name} Deployment Failed" : ":github-check: ${name} Deployment Success"

    def githubURL = env.GIT_URL[0..-5]
    def githubLink = "<${githubURL}|keyreport>"

    def shortMsg = sh(returnStdout: true, script: 'git log -1 --pretty="%s"').trim()
    def fullMsg = sh(returnStdout: true, script: 'git log -1 --pretty="%B"').trim()
    def author = sh(returnStdout: true, script: 'git log -1 --pretty="%an"').trim()
    def commitURL = "<${githubURL}/commit/${env.GIT_COMMIT}|${env.GIT_COMMIT[0..6]}>"

    JSONArray attachments = new JSONArray()
    JSONObject attachment = new JSONObject()

    attachment.put('fallback', status.toString())
    attachment.put('color', colorCode)
    attachment.put('title', status.toString())
    attachment.put('title_link', env.RUN_DISPLAY_URL)
    attachment.put('text', fullMsg)

    JSONArray fields = new JSONArray()
    JSONObject committer = new JSONObject()
    committer.put('title', 'Commiter')
    committer.put('value', author)
    committer.put('short', true)
    fields.add(committer)

    JSONObject commit = new JSONObject()
    commit.put('title', ':github-merged:')
    commit.put('value', commitURL.toString())
    commit.put('short', true)
    fields.add(commit)

    JSONObject ghLink = new JSONObject()
    ghLink.put('title', ':github:')
    ghLink.put('value', githubLink.toString())
    ghLink.put('short', true)
    fields.add(ghLink)

    attachment.put('fields', fields)
    attachments.add(attachment);

    echo attachments.toString()

    slackSend(color: colorCode, channel: '@bob', attachments: attachments.toString())
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

    JSONArray attachments = new JSONArray()
    JSONObject attachment = new JSONObject()

    attachment.put('fallback', status)
    attachment.put('color', colorCode)
    attachment.put('title', status)
    attachment.put('title_link', env.RUN_DISPLAY_URL)
    attachment.put('text', fullMsg)

    JSONArray fields = new JSONArray()
    JSONObject commit = new JSONObject()
    commit.put('title', 'pr')
    commit.put('value', commitURL)
    commit.put('short', false)
    fields.add(commit)

    JSONObject committer = new JSONObject()
    commit.put('title', 'Commiter')
    commit.put('value', author)
    commit.put('short', false)
    fields.add(committer)

    JSONObject ghLink = new JSONObject()
    commit.put('title', 'github')
    commit.put('value', githubLink)
    commit.put('short', false)
    fields.add(ghLink)

    attachment.add(fields)
    attachments.add(attachment);
    return attachments
}