## Tekton Pipeline YAML Generator

The goal of this Quarkus Application is to simplify the life of the users when they play with Tekton or any compliant project like [Konflux](https://konflux-ci.dev/) which uses an opinionated pipeline's [template](https://github.com/konflux-ci/build-definitions/blob/main/pipelines/template-build/template-build.yaml) to generate the required Tekton YAML
resources.

**Note**: This project is complementary to what Dekorate can populate today for [Tekton](https://github.com/dekorateio/dekorate/tree/main/annotations/tekton-annotations) !

The application has been designed around the following principles:

- Have a quarkus standalone application able to generate different Tekton resources for a specific provider: Tekton, Konflux, etc
- Support to provide the needed parameters or configuration using a YAML configurator file
- Generate using the Fabric8 kubernetes Fluent API & Builder the resources using [Tekton model v1](https://github.com/fabric8io/kubernetes-client/tree/main/extensions/tekton/model-v1/)
- Propose `Factories` able to generate the Tekton objects such as: params, labels, workspaces, results, finally using `defaulting` values or YAML content from the configuration file
- Support different domains: `buildpacks, s2i, etc` and types: `ubi-builder, etc` in order to chain the proper tasks and resources within the pipeline generated. If by example you select as domain: `buildpacks` and type: `builder` then the application will generate a pipeline able to `build` an  UBI builder image for buildpacks ! 

### How to use it

Git clone the project and package the application:

```shell script
./mvnw package
```

Create a configuration YAML file where you will define the following parameters:
 - The type to be used: `konflux` or `tekton`
 - Select the `domain` such as: `buidpacks` and next the type: `builder` `stack`, `meta-buildpack`, `buildpack`, etc. The combination of the domain and the `type` will allow the tool to select the proper task, workspaces, finally, when, results, etc resources
```bash
cat <<EOF > my-config.yaml
# The type will be used by the application to generate the resources for the selected provider: konflux, tekton
type: tekton

# A job represents a collection of kubernetes resources able to perform different tasks, steps
job:
  # The domain allows to organize the resources to be generated BUT also to select the type of the build - https://github.com/konflux-ci/build-definitions/blob/main/pipelines/template-build/template-build.yaml#L112
  # to be executed.
  # Such a type matches a corresponding Task which is either:
  # - example: dummy task to echo a message
  # - pack: to build an image using the Pack CLI
  # - build: to build an application using a builder image
  # - builder: to create a builder image
  # - stack: to create a base stack image build or run
  # - meta/composite: to package the buildpacks of a "meta/composite" buildpack project
  # - buildpack: to package a "buildpack" project
  # - extension: to package an "extension" project
  domain: example
  # One of the supported resources: PipelineRun, Pipeline, Task
  type: PipelineRun
  name: pipeline-1 # name of the pipeline to be created
EOF
```
and launch it:
```bash
java -jar target/quarkus-app/quarkus-run.jar -c my-config.yaml -o out/flows
```  

Next, check the pipeline(s) generated under `./out/flows`

**Remark**: Use the parameter `-h` to get the help usage of the application

To, by generate a Konflux pipeline for `buildpacks`, create this cfg file
```bash
cat <<EOF > my-konflux.yaml
type: konflux
job:
  domain: buildpack
  name: ubi-buildpacks-builder-pipeline
  builder:
    repository:
      name: https://github.com/paketo-community/builder-ubi-base
      branch: main
EOF
```

The `configuration-examples` folder proposes different YAML configuration of what you can configure :-)

### Scenarios available

#### Tekton

##### Simple pipeline with script embedded
```bash
cat <<EOF > cfg.yml
type: tekton
name: example
namespace:

job:
  # The domain allows to organize the resources, tasks to be generated
  domain: example
  # One of the supported resources: PipelineRun, Pipeline, Task
  resourceType: Pipeline
  name: pipeline-1 # name of the pipeline to be created
  description: Simple example of a Tekton pipeline echoing a message
EOF
```
Resource generated:
```yaml
---
apiVersion: "tekton.dev/v1"
kind: "Pipeline"
metadata:
  annotations:
    tekton.dev/pipelines.minVersion: "0.40.0"
    tekton.dev/displayName: "Simple example of a Tekton pipeline echoing a message"
    tekton.dev/platforms: "linux/amd64"
  labels:
    app.kubernetes.io/version: "0.2"
  name: "pipeline-1"
spec:
  tasks:
  - name: "task-embedded-script"
    taskSpec:
      steps:
      - image: "ubuntu"
        name: "run-script"
        script: |-
          #!/usr/bin/env bash

          set -e
          echo "Say Hello"
```
#### PipelineRun to run the pack CLI and create a builder image

```bash
cat <<EOF > cfg.yml
type: tekton
name: "pack builder"
namespace:

job:
  # The domain allows to specify the type of the build to be executed.
  # Such a type matches a corresponding Task which is either:
  # - pack: to build an image using the Pack CLI
  domain: pack
  # One of the supported resources: PipelineRun, Pipeline, Task
  resourceType: PipelineRun
  name: pack-builder-push
  description: "This Pipeline builds a builder image using the pack CLI."
EOF
java -jar target/quarkus-app/quarkus-run.jar -o out/flows -c cfg.yml
```
Resource generated:
```yaml
---
apiVersion: "tekton.dev/v1"
kind: "PipelineRun"
metadata:
  annotations:
    tekton.dev/displayName: "This Pipeline builds a builder image using the pack CLI."
    tekton.dev/pipelines.minVersion: "0.40.0"
    tekton.dev/platforms: "linux/amd64"
  labels:
    app.kubernetes.io/version: "0.2"
  name: "pack-builder-push-run"
spec:
  params:
  - name: "debug"
    value: "true"
  - name: "git-url"
    value: "https://github.com/redhat-buildpacks/ubi-image-builder.git"
  - name: "source-dir"
    value: "."
  - name: "output-image"
    value: "quay.io/snowdrop/ubi-builder"
  - name: "imageUrl"
    value: "buildpacksio/pack"
  - name: "imageTag"
    value: "latest"
  - name: "packCmdBuilderFlags"
    value:
    - "-v"
    - "--publish"
  pipelineSpec:
    tasks:
    - name: "git-clone"
      params:
      - name: "url"
        value: "$(params.git-url)"
      - name: "subdirectory"
        value: "."
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/konflux-ci/tekton-catalog/task-git-clone:0.1@sha256:de0ca8872c791944c479231e21d68379b54877aaf42e5f766ef4a8728970f8b3"
        - name: "name"
          value: "git-clone"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      workspaces:
      - name: "output"
        workspace: "source-dir"
    - name: "fetch-packconfig-registrysecret"
      runAfter:
      - "git-clone"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/ch007m/tekton-bundle:latest@sha256:af13b94347457df001742f8449de9edb381e90b0d174da598ddd15cf493e340f"
        - name: "name"
          value: "fetch-packconfig-registrysecret"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      workspaces:
      - name: "data-store"
        workspace: "data-store"
      - name: "pack-workspace"
        workspace: "pack-workspace"
    - name: "list-source-workspace"
      runAfter:
      - "fetch-packconfig-registrysecret"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/ch007m/tekton-bundle:latest@sha256:af13b94347457df001742f8449de9edb381e90b0d174da598ddd15cf493e340f"
        - name: "name"
          value: "list-source-workspace"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      workspaces:
      - name: "source-dir"
        workspace: "source-dir"
      - name: "pack-workspace"
        workspace: "pack-workspace"
    - name: "pack-builder"
      params:
      - name: "PACK_SOURCE_DIR"
        value: "$(params.source-dir)"
      - name: "PACK_CLI_IMAGE"
        value: "$(params.imageUrl)"
      - name: "PACK_CLI_IMAGE_VERSION"
        value: "$(params.imageTag)"
      - name: "BUILDER_IMAGE_NAME"
        value: "$(params.output-image)"
      - name: "PACK_BUILDER_TOML"
        value: "ubi-builder.toml"
      - name: "PACK_CMD_FLAGS"
        value:
        - "$(params.packCmdBuilderFlags)"
      runAfter:
      - "fetch-packconfig-registrysecret"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/ch007m/tekton-bundle:latest@sha256:af13b94347457df001742f8449de9edb381e90b0d174da598ddd15cf493e340f"
        - name: "name"
          value: "pack-builder"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      workspaces:
      - name: "source-dir"
        workspace: "source-dir"
      - name: "pack-workspace"
        workspace: "pack-workspace"
  workspaces:
  - name: "source-dir"
    volumeClaimTemplate:
      apiVersion: "v1"
      kind: "PersistentVolumeClaim"
      spec:
        accessModes:
        - "ReadWriteOnce"
        resources:
          requests:
            storage: "1Gi"
  - name: "pack-workspace"
    volumeClaimTemplate:
      apiVersion: "v1"
      kind: "PersistentVolumeClaim"
      spec:
        accessModes:
        - "ReadWriteOnce"
        resources:
          requests:
            storage: "1Gi"
  - name: "data-store"
    projected:
      sources:
      - secret:
          name: "pack-config-toml"
      - secret:
          name: "quay-creds"
```
#### Konflux

```bash
 CFG=konflux/build-quarkus-cfg.yaml
./mvnw package;java -jar target/quarkus-app/quarkus-run.jar -o out/flows -c configurations/$CFG
```

### Bundles packaged

```bash
tkn bundle push quay.io/ch007m/tekton-bundle:latest \
  -f bundles/fetch-packconfig-registrysecret.yaml \
  -f bundles/list-source-workspace.yaml \
  -f bundles/pack-builder.yaml
  
tkn bundle list quay.io/ch007m/tekton-bundle:latest     
task.tekton.dev/list-source-workspace
task.tekton.dev/fetch-packconfig-registrysecret
task.tekton.dev/pack-builder
```

### Trusted Konflux Tekton tasks

To get the list of the konflux tekton bundles (oci or git) supported/trusted:
```bash
## https://www.conftest.dev/
brew install conftest
mkdir temp && cd temp
conftest pull --policy './temp' oci::quay.io/konflux-ci/tekton-catalog/data-acceptable-bundles:latest
cat temp/data/data/trusted_tekton_tasks.yml | yq -o=json | jq -r '.trusted_tasks | keys[]' > temp/bundles_list.txt
cat temp/bundles_list.txt
```
To extract the task resource from the bundle, you can use the tekton client with the following command:
```bash
REGISTRY_NAME=quay.io/konflux-ci/tekton-catalog
BUNDLE_NAME=task-git-clone
BUNDLE_VERSION=0.1
BUNDLE_URL=$REGISTRY_NAME/$BUNDLE_NAME:$BUNDLE_VERSION
tkn bundle list $BUNDLE_URL task -o json > git-clone.json
tkn bundle list $BUNDLE_URL task -o yaml > git-clone.yaml

BUNDLE_NAME=task-git-clone-oci-ta
BUNDLE_VERSION=0.1
BUNDLE_URL=$REGISTRY_NAME/$BUNDLE_NAME:$BUNDLE_VERSION
tkn bundle list $BUNDLE_URL task -o json > git-clone-oci-ta.json
tkn bundle list $BUNDLE_URL task -o yaml > git-clone-oci-ta.yaml
```




