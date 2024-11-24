#!/bin/bash -e
set -x
source "${BASH_SOURCE%/*}/bash_pipeline_utils.sh"

NAMESPACE=$1
SERVER_FULL_IMAGE_NAME=$2


export DEPLOYS=$(helm ls -n ${NAMESPACE} --all --short --filter ${NAMESPACE}) # check if there's an active deployment
if [[  ${DEPLOYS} != "" ]];
then
  for i in ${DEPLOYS}; do
      echo -e "\033[33m*** Deleting existing deployments ***\033[0m"
      # if so, kill the active deployment
      helm del -n ${NAMESPACE} ${i};
  done
fi

echo '$(ls -l)'

./deployment/scripts/wait_for_termination.sh ${NAMESPACE}


kubectl delete persistentvolumeClaims -n ${NAMESPACE} --all #helm purge doesn't delete claims.

export MAIN_CHART_PATH="$WORKSPACE/deployment/wild-tag/"

echo -e "\e[32m### Installing WildTag HELM chart ###\e[0m"

helm upgrade --install ${NAMESPACE} ${MAIN_CHART_PATH} -n ${NAMESPACE}

echo -e "\e[32m### HELM chart successfully installed ###\e[0m"