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
  # The domain allows to organize the resources, tasks to be generated: example, buildpack
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




