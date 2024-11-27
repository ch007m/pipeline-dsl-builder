# RHEL-9

VM created using the RHOS UI with:

- name: "RHEL-9"
- Flavor: ocp4.compute
- Image: RHEL-9.0.0-x86_64-released - https://rhos-d.infra.prod.upshift.rdu2.redhat.com/dashboard/ngdetails/OS::Glance::Image/2ca6a6a1-5a34-4066-b74a-4983c30bfff0
- Network: provider-shared
- Key: snowdrop-team

Next, ssh to the VM and 
```bash
# https://access.redhat.com/solutions/253273
sudo subscription-manager register --username <username> --password <password> --auto-attach
sudo subscription-manager refresh
sudo subscription-manager attach --auto
sudo subscription-manager list
```
Provision the VM and start podman
```bash
sudo yum install git curl wget jq podman
sudo systemctl enable podman
sudo systemctl start podman
systemctl --user start podman.socket
ls $XDG_RUNTIME_DIR/podman/podman.sock
```
Log on to the registries:
```bash
podman login -u ch007m@gmail.com -p xxxxxx docker.io
```
Disable `setenforce` to avoid such an error reported during the build of the Dockerfile: https://github.com/containers/podman/issues/3234
```bash
sudo setenforce 0
```
Install or curl the installation bash script
```bash
curl -sL -H 'Cache-Control: no-cache, no-store' \
  https://raw.githubusercontent.com/ch007m/pipeline-dsl-builder/main/scripts/install_buildpack_testing_environment.sh \
  | bash
```

# HowTo use konflux CI

- Create a kind + konflux cluster: https://github.com/konflux-ci/konflux-ci?tab=readme-ov-file#bootstrapping-the-cluster
- Enable the Pipelines Triggering via Webhooks: https://github.com/konflux-ci/konflux-ci?tab=readme-ov-file#enable-pipelines-triggering-via-webhooks
- Create a smee channel: https://smee.io/Wdavq8x0Oc9Jnl9M 
- Edit the [smee-client.yaml](https://github.com/konflux-ci/konflux-ci/blob/main/smee/smee-client.yaml#L28) file to specify your smee URL and deploy the resources
```bash
cat <<EOF | kubectl apply -f -
---
apiVersion: v1
kind: Namespace
metadata:
  name: smee-client
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gosmee-client
  namespace: smee-client
spec:
  progressDeadlineSeconds: 600
  replicas: 1
  revisionHistoryLimit: 10
  selector:
    matchLabels:
      app: gosmee-client
  strategy:
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 25%
    type: RollingUpdate
  template:
    metadata:
      labels:
        app: gosmee-client
    spec:
      containers:
      - args:
        - client
        - https://smee.io/Wdavq8x0Oc9Jnl9M
        - http://pipelines-as-code-controller.pipelines-as-code:8080
        image: ghcr.io/chmouel/gosmee:v0.21.0
        imagePullPolicy: Always
        name: gosmee
        resources:
          limits:
            cpu: 100m
            memory: 32Mi
          requests:
            cpu: 10m
            memory: 32Mi
        securityContext:
          readOnlyRootFilesystem: true
          runAsNonRoot: false
        terminationMessagePath: /dev/termination-log
        terminationMessagePolicy: File
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      schedulerName: default-scheduler
      securityContext: {}
      terminationGracePeriodSeconds: 30
```
- Create the secrets fof the different services as documented here: 
```bash
cat <<EOF > githubapp-konfluxci.private-key.pem
-----BEGIN RSA PRIVATE KEY-----
MIIEpQIBAAKCAQEAz5wEbPeh/3dAORFmistuarwxuJVn0k2WUBecz61l2gqFNT4m
...
lfmt/yn/tXHrBihYCrUI/D4JB5R2VVXo2feegyOenpwiUHQNgfFEk8c=
-----END RSA PRIVATE KEY-----

EOF

APP_ID=947228
WEBHOOK_SECRET=konfluxci
PATH_PRIVATE_KEY="githubapp-konfluxci.private-key.pem"
kubectl -n pipelines-as-code delete secret pipelines-as-code-secret
kubectl -n pipelines-as-code create secret generic pipelines-as-code-secret \
--from-literal github-private-key="$(cat $PATH_PRIVATE_KEY)" \
--from-literal github-application-id="$APP_ID" \
--from-literal webhook.secret="$WEBHOOK_SECRET"

kubectl -n build-service delete secret pipelines-as-code-secret
kubectl -n build-service create secret generic pipelines-as-code-secret \
--from-literal github-private-key="$(cat $PATH_PRIVATE_KEY)" \
--from-literal github-application-id="$APP_ID" \
--from-literal webhook.secret="$WEBHOOK_SECRET"

kubectl -n integration-service delete secret pipelines-as-code-secret
kubectl -n integration-service create secret generic pipelines-as-code-secret \
--from-literal github-private-key="$(cat $PATH_PRIVATE_KEY)" \
--from-literal github-application-id="$APP_ID" \
--from-literal webhook.secret="$WEBHOOK_SECRET"
```