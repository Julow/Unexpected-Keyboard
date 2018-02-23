IMAGE_NAME=unexpected-keyboard-build

if [[ $# -le 0 ]]; then set "bash"; fi

docker build -t "$IMAGE_NAME" - < Dockerfile
docker run -it --rm -v"`pwd`:/app:ro" -e "BUILD_DIR=/tmp/bin" \
	"$IMAGE_NAME" "$@"
