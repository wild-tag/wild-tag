#!/bin/sh

NAMESPACE=$1

IP=""

set pipefail

while true; do
	IP=$(kubectl get services --namespace "$NAMESPACE" -o yaml --selector app.kubernetes.io/name=nginx -o jsonpath='{.items[0].status.loadBalancer.ingress[0].ip}')

	if [ "$IP" != "<pending>" ] && [ ! -z $IP ];
	then
		echo $IP
		exit 0
	else
		sleep 3
	fi
done
