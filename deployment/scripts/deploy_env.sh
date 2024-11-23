#!/bin/bash -e
set -x

NAMESPACE=$1
IMAGE_TAG=$2

export MAIN_CHART_PATH="${GITHUB_WORKSPACE}/deployment/wild-tag"

echo -e "\e[32m### Installing WildTag HELM chart ###\e[0m"

helm upgrade --install ${NAMESPACE} ${MAIN_CHART_PATH} -n ${NAMESPACE} \
    --set core.imageTag=$IMAGE_TAG \
    --set nginx.image.tag=$IMAGE_TAG

echo -e "\e[32m### HELM chart successfully installed ###\e[0m"