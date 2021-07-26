#!/usr/bin/env bash
set -e
set -o pipefail

# K8S_CA_CERT=/var/run/secrets/kubernetes.io/serviceaccount/ca.crt

# if [ -f "$K8S_CA_CERT" ]; then
#     if command -v update-ca-certificates &> /dev/null
#     then
#         cp $K8S_CA_CERT /usr/local/share/ca-certificates/k8s-ca.crt
#         update-ca-certificates
#         echo "Kubernetes CA certificate was aded to the system trust store"
#     fi
#     if command -v keytool &> /dev/null
#     then
#         keytool -storepass changeit -noprompt -trustcacerts -importcert -cacerts \
#         -alias cacertk8s -file $K8S_CA_CERT
#         echo "Kubernetes CA certificate was aded to the JAVA trust store"
#     fi
# fi

echo "Executing \"${@}\"..."

exec "$@"
