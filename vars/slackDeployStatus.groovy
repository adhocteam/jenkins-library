import net.sf.json.JSONArray
import net.sf.json.JSONObject

def call(Map config) {
    def repoName = extractRepoName()
    def name = config.get('name', repoName)
    def failed = config.get('failed', false)

    def colorCode = failed ? '#FF0000' : '#118762'
    def status = failed ? ":no_entry: ${name} Deployment Failed" : ":github-check: ${name} Deployment Success"

    def githubURL = env.GIT_URL[0..-5]
    def githubLink = "<${githubURL}|${repoName}>"


    def shortMsg = sh(returnStdout: true, script: 'git log -1 --pretty="%s"').trim()
    def fullMsg = sh(returnStdout: true, script: 'git log -1 --pretty="%B"').trim()
    def author = sh(returnStdout: true, script: 'git log -1 --pretty="%an"').trim()

    def prNum = ""

    // Swallow the error if no PR number found
    try { prNum = extractPR(shortMsg) }
    catch (err) {}

    def commitURL = "<${githubURL}/commit/${env.GIT_COMMIT}|${env.GIT_COMMIT[0..6]}>"
    if (prNum) {
        //Get the matched group with the number
        commitURL = "<${githubURL}/pulls/${prNum}|PR-${prNum}>"
    }

    JSONArray attachments = new JSONArray()
    JSONObject attachment = new JSONObject()

    attachment.put('fallback', status.toString())
    attachment.put('color', colorCode)
    attachment.put('title', status.toString())
    attachment.put('title_link', env.RUN_DISPLAY_URL)
    attachment.put('text', fullMsg)

    JSONArray fields = new JSONArray()
    JSONObject committer = new JSONObject()
    committer.put('title', 'Committer')
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

    def slackChannel = config.get('channel', '#inf-alerts')

    slackSend(color: colorCode, channel: slackChannel, attachments: attachments.toString())
}

@NonCPS
def extractRepoName() { (env.GIT_URL =~ /.*\/([^\/]+).git/)[0][1] }

@NonCPS
def extractPR(shortMsg) { (shortMsg =~ /.*#([0-9]+).*/)[0][1] }
