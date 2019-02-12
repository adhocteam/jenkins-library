def call(Map config) {

    def env = config.get('env', 'dev')
    def app = config.get('app', 'api-dev')
    def command = config.get('command', '["bundle", "exec", "rake", "db:migrate"]')


    def getSecurityGroup = """aws ec2 describe-security-groups \
        --region=us-east-1 \
        --filters 'Name=tag:env,Values=${env}' \
                    'Name=tag:app,Values=${app}' \
                    'Name=group-name,Values=${app}-app-*' \
        | jq --raw-output '.SecurityGroups[0].GroupId'"""

    def getSubnet = """aws ec2 describe-subnets \
        --region=us-east-1 \
        --filters 'Name=tag:env,Values=${env}' \
                    'Name=tag:name,Values=app-sub-*' \
        | jq --raw-output '.Subnets[0].SubnetId'"""

    echo "Getting data from AWS"
    def sg_id = sh(returnStdout: true, script: getSecurityGroup).trim()
    def subnet_id = sh(returnStdout: true, script: getSubnet).trim()

    def startTask = """aws ecs run-task \
        --region=us-east-1 \
        --cluster=${app} \
        --task-definition ${env}-${app} \
        --overrides '{"containerOverrides": [{"name": "${app}", "command": ${command}}]}' \
        --count 1 \
        --launch-type FARGATE \
        --network-configuration "awsvpcConfiguration={subnets=[${subnet_id}],securityGroups=[${sg_id}],assignPublicIp=DISABLED}" \
        | jq --raw-output '.tasks[0].taskArn'"""

    echo "Scheduling task with command: ${command}"
    def task_arn = sh(returnStdout: true, script: startTask).trim()


    def getStatus = """aws ecs describe-tasks \
        --cluster "${app}" \
        --tasks "${task_arn}" \
        | jq --raw-output '.tasks[0].lastStatus'"""

    // Wait for AWS to schedule and execute our task
    sleep 10
    for (i in 1..10) {

        def status = sh(returnStdout: true, script: getStatus).trim()
        echo "Task status: ${status}"
        if (status == "STOPPED") {
            break
        }
        sleep ( 15 * i )
    }

    def getExitCode = """aws ecs describe-tasks \
        --region=us-east-1 \
        --cluster "${app}" \
        --tasks "${task_arn}" \
        | jq --raw-output '.tasks[0].containers[0].exitCode'"""

    def exit_code = sh(returnStdout: true, script: getExitCode).trim()
    echo "Exit Code: ${exit_code}"

    if (exit_code > 0) {
        error "Task failed! Check the cloudwatch logs for details"
    }

    echo "Task success!"
}


