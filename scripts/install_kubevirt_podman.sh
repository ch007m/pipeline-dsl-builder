#!/usr/bin/env bash

export KUBEVIRT_VERSION=v1.2.1
export KUBEVIRT_CDI_VERSION=v1.59.0
export KUBE_NAMESPACE=vm-services

function is_nested_virt_enabled() {
  kvm_nested="unknown"
  if [ -f "/sys/module/kvm_intel/parameters/nested" ]; then
    kvm_nested=$( cat /sys/module/kvm_intel/parameters/nested )
  elif [ -f "/sys/module/kvm_amd/parameters/nested" ]; then
    kvm_nested=$( cat /sys/module/kvm_amd/parameters/nested )
  fi
  [ "$kvm_nested" == "1" ] || [ "$kvm_nested" == "Y" ] || [ "$kvm_nested" == "y" ]
}

echo "Deploying KubeVirt"
kubectl apply -f "https://github.com/kubevirt/kubevirt/releases/download/${KUBEVIRT_VERSION}/kubevirt-operator.yaml"
kubectl apply -f "https://github.com/kubevirt/kubevirt/releases/download/${KUBEVIRT_VERSION}/kubevirt-cr.yaml"

echo "Configuring Kubevirt to use emulation if needed"
if ! is_nested_virt_enabled; then
  kubectl -n kubevirt patch kubevirt kubevirt --type=merge --patch '{"spec":{"configuration":{"developerConfiguration":{"useEmulation":true}}}}'
fi

echo "Deploying KubeVirt containerized-data-importer"
kubectl apply -f "https://github.com/kubevirt/containerized-data-importer/releases/download/${KUBEVIRT_CDI_VERSION}/cdi-operator.yaml"
kubectl apply -f "https://github.com/kubevirt/containerized-data-importer/releases/download/${KUBEVIRT_CDI_VERSION}/cdi-cr.yaml"

echo "Waiting for KubeVirt to be ready"
kubectl wait --for=condition=Available kubevirt kubevirt --namespace=kubevirt --timeout=5m

echo "Patch the StorageProfile to use the storageclass standard and give ReadWrite access"
kubectl get StorageProfile
kubectl patch --type merge -p '{"spec": {"claimPropertySets": [{"accessModes": ["ReadWriteOnce"], "volumeMode": "Filesystem"}]}}' StorageProfile standard

kubectl create clusterrolebinding pod-kubevirt-viewer --clusterrole=kubevirt.io:view --serviceaccount=${KUBE_NAMESPACE}:default
kubectl create clusterrolebinding cdi-kubevirt-viewer --clusterrole=cdi.kubevirt.io:view --serviceaccount=${KUBE_NAMESPACE}:default
kubectl create clusterrolebinding vm-podman --clusterrole=admin --serviceaccount=${KUBE_NAMESPACE}:default

# Give RBAC to the SA argocd-server of the namespace argocd to access Applications running in another namespaces
kubectl create clusterrolebinding argocd-server-applications --clusterrole=argocd-applicationset-controller --serviceaccount=argocd:argocd-server

kubectl create ns vm-images
kubectl apply -n vm-images -f manifest/installation/virt/quay-to-pvc-datavolume.yml
kubectl wait datavolume -n vm-images podman-remote --for condition=Ready=True --timeout=360s

ssh-keygen -N "" -f id_rsa
kubectl create secret generic podman-ssh-key -n ${KUBE_NAMESPACE} --from-file=key=id_rsa.pub

MANIFEST_PATH=./manifest/installation/virt
kustomize build ${MANIFEST_PATH} | kubectl apply -n ${KUBE_NAMESPACE} -f -
kubectl wait --for=condition=Ready vm/vm-podman -n ${KUBE_NAMESPACE} --timeout=360s
