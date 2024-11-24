#!/bin/bash -e
set -x

NAMESPACE=$1


export DEPLOYS=$(helm ls -n ${NAMESPACE} --all --short --filter ${NAMESPACE}) # check if there's an active deployment
if [[  ${DEPLOYS} != "" ]];
then
  for i in ${DEPLOYS}; do
      echo -e "\033[33m*** Deleting existing deployments ***\033[0m"
      # if so, kill the active deployment
      helm del -n ${NAMESPACE} ${i};
  done
  ./deployment/scripts/wait_for_termination.sh ${NAMESPACE}
  kubectl delete persistentvolumeClaims -n ${NAMESPACE} --all #helm purge doesn't delete claims.

fi


export MAIN_CHART_PATH="${GITHUB_WORKSPACE}/deployment/wild-tag"

echo -e "\e[32m### Installing WildTag HELM chart ###\e[0m"

helm upgrade --install ${NAMESPACE} ${MAIN_CHART_PATH} -n ${NAMESPACE}

echo -e "\e[32m### HELM chart successfully installed ###\e[0m"