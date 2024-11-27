#!/bin/bash -e
set -x

NAMESPACE=$1
ZONE_NAME=$2
RECORD_SUFFIX=$3
RECORD_NAME="$NAMESPACE""$RECORD_SUFFIX"

DEPLOYS=$(helm ls -n "${NAMESPACE}" --all --short --filter "${NAMESPACE}") # check if there's an active deployment
if [[  ${DEPLOYS} != "" ]];
then

  for i in ${DEPLOYS}; do
      echo -e "\033[33m*** Deleting existing deployments ***\033[0m"
      # if so, kill the active deployment
      helm del -n "${NAMESPACE}" "${i}";
  done
  ./deployment/scripts/wait_for_termination.sh "${NAMESPACE}"
  kubectl delete persistentvolumeClaims -n "${NAMESPACE}" --all #helm purge doesn't delete claims.

fi


# List and filter records, then remove them
gcloud dns record-sets list --zone="$ZONE_NAME" --filter="name~$RECORD_NAME" \
  --format="value(name,type,rrdatas)" | while read -r name type rrdatas; do
    echo "Removing record: $name $type $rrdatas"
    gcloud dns record-sets delete "$name" --type="$type" --zone="$ZONE_NAME"
done
