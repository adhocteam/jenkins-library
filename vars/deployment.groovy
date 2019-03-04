def call(String repo, String environment, String url, Closure body) {
  def deployID = ""

  def createDeployment = """
    set -eu

    # Create Github Deployment & get the deployment ID
    curl --request POST \
      --url https://api.github.com/repos/adhocteam/${repo}/deployments \
      --header 'accept: application/vnd.github.ant-man-preview+json' \
      --header "authorization: token \$GH_TOKEN" \
      --header 'content-type: application/json' \
      --data '{
        "ref": "${CHANGE_BRANCH}",
        "environment": "${environment}",
        "required_contexts": [],
        "description": "Starting deployment for ${environment}"
      }' | jq ".id"
    """

  def setStatus = { String id, String status ->
    """
    set -eu

    curl --request POST \
      --url "https://api.github.com/repos/adhocteam/${repo}/deployments/${id}/statuses" \
      --header 'accept: application/vnd.github.ant-man-preview+json, application/vnd.github.flash-preview+json' \
      --header "authorization: token \$GH_TOKEN" \
      --header 'content-type: application/json' \
      --data '{
      "environment": "${environment}",
      "state": "${status}",
      "environment_url": "${url}",
      "description": "Deployment to ${environment}: ${status}."
      }'
    """
  }

  def status = "failure"
  try {
    withCredentials([string(credentialsId: 'github-token', variable: 'GH_TOKEN')]) {
      deployID = sh(returnStdout: true, script: createDeployment).trim()
    }
    body.call()
    status = "success"
  } catch (e) {
    throw e
  } finally {
    script = setStatus(deployID, status)
    withCredentials([string(credentialsId: 'github-token', variable: 'GH_TOKEN')]) {
      sh script
    }
  }
}
