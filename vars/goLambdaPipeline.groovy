def call(body) {

  // evaluate the body block, and collect configuration into the object
  def params= [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = params
  body()

  pipeline {
    agent {
      label 'general'
    }

    stages {
      stage("Run golang validation") {
        steps {
          goMetaLinter()
        }
      }

      stage("Build") {
        agent {
          docker {
            image 'adhocteam/gometalinter'
            reuseNode true
          }
        }
        steps {
            sh """set -eux
                  CGO_ENABLED=0 GOOS=linux go build ./...
                  zip ${params.lambda}.zip ${params.lambda}
                  """
        }
      }

      stage("Release") {
        when { branch 'master' }
        steps {
            sh "aws s3 cp ${params.lambda}.zip s3://adhoc.team-${params.env}-lambda-releases/${params.lambda}.zip"
        }
      }

      stage("Deploy") {
        when { branch 'master' }
        steps {
            sh "aws lambda update-function-code --region us-east-1 --function-name arn:aws:lambda:us-east-1:${params.account}:function:${params.lambda} --s3-bucket adhoc.team-${params.env}-lambda-releases --s3-key ${params.lambda}.zip --publish"
        }
        post {
          success {
            slackDeployStatus name: "${params.name}"
          }
          failure {
            slackDeployStatus name: "${params.name}", failed: true
          }
        }
      }
    }

    post {
      always {
        deleteDir()
        cleanWs()
      }
    }
  }
}