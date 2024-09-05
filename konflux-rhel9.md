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
iVbGvryp8MruuNyi/N5ST7tbLXJs4FuSBlXMJySIsTpGhMqW/oybyWSGe/n7U90T
g5FiUYYCupAd8XDRQJ2z5s5eOFKigFlMkyvntzRRhLyLF76F8w/ICGAPvPX6zF46
+5fCcqRcnybNg50OHx0gMophB4RGe7X9B9Ra4aOs4ufdloM5Fui6mL2PtmSuPFox
08Rje8HdruG80ky9Zyuh/8AKHTcEAgxfmvLqwq2tSUlCo9Pg192EYpVvjV0yT7hp
Wb5yRn/fY+ZxMB8Em+v4Q5kTnnhRMB/nrz3T3QIDAQABAoIBAQC/YKXfwSKveUFV
mgm5ti+x6ou3CNrszSLb7/hYpRI3vezvmLwzbC/HUekiFB+df54rldGwuBgju9BL
vX3NozePeakcHok1Df/E5N8S9jzWeilJNIkxhkpoti07x3AiygnLE9Tr1Z6bMumj
gS4KmLWAd5UR1DAwEiwuVITj25GGcFqKpTKbeQgB6nj6+abERLh3QT2sly6YsPbO
G6oe4JOr6EI+7D3/UsbNUv3qFRDX5hO7vDHtJhdJehyW//FzHF3fdroWOSg+bWAK
Q1PTqYmO3RUPC9vW4MVFDQQlYF5WNzg00muh9yZi4+IqwmWBuoamKLp5pCjAlkJZ
KJGAiL2hAoGBAP/YHYrs1N5IOXlgZoZ5cUQ463ADZpMHIrncYatNe7j3jMhI7NsL
DvhuBmsy2xe3ghaWphGMaWwC6749QYsWbeE3FqkzwVFbc61iPECSZ1hcOmAnFRyN
4IWydpP9j7UEO6a3HdxQ7B3IYap1GiPKfb++iSmqYR8F64z6lZ4RUOxVAoGBAM+8
YeMwT6RZZHShvZikPpMarT/u+wJuvowubpP7r3x1BTWRDM7KLslnQj3rAXh4a9Ec
4AYV1fq7hBjnqLWxjqgz8aiQc+/6POBXG9C2NG9tKgObtLKw+QEGE8o+JTf+KPAd
FzdYq3H9XVy5dDu3tc1k4wkINFbVvC0EhWp3LlFpAoGBAJdB1CWAY1GPbbtezP6P
6fElnbw5pnkibNtpeaznQFBYurjmtHHEFfO2SMEz7egVrClio4gYdXNQPsPYP1nV
xtyxzwn1+UL6SGenfmvGoqbQ0Aps0MRy9NzWZ9iSvlWMzR+Bf3vzs8Tf5S370Zp7
auDj6v/hJU5MF7jfpXkwT6GJAoGADODx9KLHHTpJhw2L8o2kL3yE5yTKvQDeoVQz
mMsOuxmKJCME90EDm4riSXJrWeulS4aNwPLTnELJ0r1x8Sm73WOzBK9H8MXDxmjA
GbViFNJgu26Iylc8aLrWuUAXEJyaLyCuksjVgDCj/B6nPRiLlds+VA4FKKkBjIzu
NIaFAZkCgYEAgT5iJ1H1bdrpIWrlZxMOeB/N7g/vgHjszeYJXdfGnPLIy+/NIbXU
N19HGJfF5zhra5NkRJtgqf8NFhBvYQ4DnM8vu/hRSbTJEkLG4vDQzDyxmU6sEoow
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