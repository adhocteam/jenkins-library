def call() {
    docker.image('adhocteam/gometalinter').inside() {
        sh '''set -eux
        gometalinter \
            --disable-all \
            --enable deadcode \
            --severity deadcode:error \
            --enable gofmt \
            --enable ineffassign \
            --enable misspell \
            --enable vet \
            --tests \
            --vendor \
            --deadline 120s \
            ./...

        gometalinter \
            --disable-all \
            --enable golint \
            --vendor \
            --skip proto \
            --deadline 120s \
            ./...'''
    }
}