def call(args) {
    sh '''#!/bin/bash
        set -e
        result=$(docker run -v "$PWD/terraform":/terraform hashicorp/terraform:light fmt -check=true ./terraform || true)
        if [[ -n $result ]]; then
            echo "ERROR: Please run terraform fmt for the following files:\n$result."
            exit 1
        fi
    '''
}