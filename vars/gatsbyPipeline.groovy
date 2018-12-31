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

      stage('Build preview site') {
        when { changeRequest() }
        agent {
          dockerfile {
            reuseNode true
            args "-e PR_ID=$CHANGE_ID"
          }
        }
        steps {
          sh '''
            set -eux
            cd /site
            npm run preview
            cd -
            cp -r /site/public .
            '''
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
              pullRequest.comment("${params.name} preview generated at http://preview.${params.url}/$CHANGE_ID/")
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
          sh '''
            set -eux
            cd /site
            npm run build
            cd -
            cp -r /site/public .
            '''
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
}