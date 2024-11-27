#!/bin/sh -e

NAMESPACE=$1
#WILD_URL=$2

RETRIES=0
TOTAL_RETRIES=600 # total time is 50 minutes

echo "\e[1;36m>>> Checking all pods are up and running <<<\e[0m"

while true; do

	export KUBECTL_PODS_RESULT=$(kubectl get pods -n ${NAMESPACE} | grep -v '3/3' | grep -v '1/1' | grep -v 'NAME')
	export KUBECTL_DEPLOYMENT_RESULT=$(kubectl get deployments -n ${NAMESPACE} | grep -v '3/3' | grep -v '1/1' | grep -v 'NAME')

	if [ "$KUBECTL_PODS_RESULT" ] && [ "$KUBECTL_DEPLOYMENT_RESULT" ] ;
	then
      if [ "$KUBECTL_DEPLOYMENT_RESULT" ] ;
      then
         echo "$KUBECTL_DEPLOYMENT_RESULT"
         echo "\033[33m>>> Not all deployments are ready\033[0m"
      fi
      if [ "$KUBECTL_PODS_RESULT" ] ;
      then
         echo "$KUBECTL_PODS_RESULT"
         echo "\033[33m>>> Not all pods are up yet\033[0m"
      fi
    if [ $RETRIES -lt $TOTAL_RETRIES ] ;
    then
      echo "\033[33m*** Attempt $RETRIES out of $TOTAL_RETRIES failed. waiting for 5 seconds... ***\033[0m"
      sleep 5
      RETRIES=$((RETRIES+1))
    else
      echo "\e[31m*** Attempt $RETRIES - Not all pods/deployments are up and running. ***\e[0m";
      # Describe deployments that have failed
      for deployment in $(kubectl get deploy -n ${NAMESPACE} -o json | jq -r '[.items[]|select(.status.readyReplicas!=.status.replicas)]'| jq '.[].metadata.name'| tr -d '"')
      do
         echo "\e[31m### Deployment FAILED: ###\e[0m"
         kubectl describe deployment $deployment -n ${NAMESPACE}
      done
      exit 1 ;
    fi

	else
      echo "\e[32m### All pods are up. ###\e[0m"
      echo "###########################"
      echo "########  SUCCESS  ########"
      echo "###########################"
      break
	fi

done



#
#RETRIES=0
#
#sleep 10
#
#echo "\e[1;36m>>> Trying healthz endpoint [$WILD_URL/api/healthz] <<<\e[0m"
#
#EXIT_CODE=0
#curl -sLk -w "%{http_code}\\n" "$WILD_URL/api/healthz" -o /dev/null --connect-timeout 10 --retry 2 || EXIT_CODE=$?
#echo "\e[1;36m>>> Exit code recieved on initial CURL: $EXIT_CODE <<<\e[0m"
#
#while [ 1=1 ]
#do
#  EXIT_CODE=0
#  result=`curl -sLk -w "%{http_code}\\n" "$WILD_URL/api/healthz" -o /dev/null --connect-timeout 10 --retry 2 || EXIT_CODE=$?`
#  echo "\e[1;36m>>> Result code is: [$result] <<<\e[0m"
#
#  if [ "$result" = "200" ] ;
#  then
#     echo "\e[32m### Management $WILD_URL is available! ###\e[0m";
#     sleep 10
#     exit 0;
#  else
#     if [ $RETRIES -lt $TOTAL_RETRIES ] ;
#     then
#        echo "\033[33m*** Attempt $RETRIES out of $TOTAL_RETRIES failed. waiting for 5 seconds... ***\033[0m"
#        sleep 5
#        RETRIES=$((RETRIES+1))
#     else
#        echo "\e[31m*** Attempt $RETRIES failed. ***\e[0m"
#        echo "\e[31m*** Management $WILD_URL is NOT available!. Job failed! ***\e[0m";
#        exit 1 ;
#     fi
#  fi
#done
