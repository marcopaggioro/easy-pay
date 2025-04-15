docker buildx build --file backend/target/docker/stage/Dockerfile --tag marcopaggioro/easypay:backend --push backend/target/docker/stage

docker buildx build --file frontend/Dockerfile --tag marcopaggioro/easypay:frontend --push frontend