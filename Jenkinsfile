pipeline {
    agent {
        label 'general'
    }

    stages {

        stage('Build') {
            when { branch "master" }
            steps {
                sh """ set -e
                    for file in Dockerfiles/*; do
                        docker build --pull --no-cache -t "adhocteam/\$(basename \$file .docker)" "\$file"
                    done
                    """
            }
        }

        stage('Publish') {
            when { branch "master" }
            steps {
                withDockerRegistry([ credentialsId: 'dockerhub-user', url: "" ]) {
                sh """ set -e
                    for file in Dockerfiles/*; do
                        docker push "adhocteam/\$(basename \$file .docker)""
                    done
                    """
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