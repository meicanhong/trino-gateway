NAME = trino-gateway
REGISTRY = docker-registry.footprint.network
NAMESPACE=mexl
TAG = beta

build:
	echo building ${REGISTRY}/${NAMESPACE}/${NAME}:${TAG}

ifeq ($(TAG), beta)
	docker build -t ${REGISTRY}/${NAMESPACE}/${NAME}:${TAG} .
	docker run -d -p 8080:8080 --name="trino-gateway" ${REGISTRY}/${NAMESPACE}/${NAME}:${TAG}
endif
