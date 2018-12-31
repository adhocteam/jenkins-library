def call(Map pipelineParams) {

  pipeline {
    agent {
      label 'general'
    }

    stages {
    stage('Build preview site') {
      when { changeRequest() }
      agent {
        dockerfile {
          reuseNode true
          args "-e PR_ID=$CHANGE_ID"
        }
      }
      steps {
        sh 'gatsby build --prefix-paths'
      }
    }
    stage('Deploy preview') {
        when { changeRequest() }
        steps {
          sh "aws s3 sync public s3://preview.${params.url}/$CHANGE_ID --delete --no-progress --acl public-read"
        }
        post {
          success {
            script {
              pullRequest.comment("${params.name} preview generated at http://preview.${params.url}.s3-website-us-east-1.amazonaws.com/$CHANGE_ID")
            }
          }
        }
      }


      stage('Build production site') {
        when { branch 'master' }
        agent {
          dockerfile {
            reuseNode true
          }
        }
        steps {
          sh 'gatsby build'
        }
      }
      stage('Deploy production') {
        when { branch 'master' }
        steps {
          sh "aws s3 sync public s3://${params.url} --delete --no-progress --acl public-read"
        }
        post {
          success {
            slackDeployStatus name: params.name
          }
          failure {
            slackDeployStatus name: params.name, failed: true
          }
        }
      }
    }

    post {
      cleanup {
        cleanWs()
      }
    }
  }