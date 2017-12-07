IMAGE_NAME=unexpected-keyboard-build

docker build -q -t "$IMAGE_NAME" - < build.Dockerfile
docker run -ti --rm -v"`pwd`:/app" "$IMAGE_NAME" "$@"
