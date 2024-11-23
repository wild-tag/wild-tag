#!/bin/bash
set -e

export NAMESPACE=$1

while true; do

  kubectl get pods --namespace=${NAMESPACE}
  kubectl get services --namespace=${NAMESPACE}

	export KUBECTL_PODS_RESULT=$(kubectl get pods --namespace=${NAMESPACE})
	export KUBECTL_SVC_RESULT=$(kubectl get services --namespace=${NAMESPACE})

	if [ "$KUBECTL_PODS_RESULT" ] || [ "$KUBECTL_SVC_RESULT" ];
	then
	  echo $SOME_ACTIVE_PODS
	  echo "not terminated yet..."
		sleep 3
	else
		echo "terminated!"
    exit 0
	fi
done