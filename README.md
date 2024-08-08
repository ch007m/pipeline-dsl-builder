# Tekton Pipeline YAML Generator

The goal of this Quarkus Application is to simplify the life of the users when they play with Tekton or any compliant project like [Konflux](https://konflux-ci.dev/) which uses an opinionated pipeline's [template](https://github.com/konflux-ci/build-definitions/blob/main/pipelines/template-build/template-build.yaml) to generate the required Tekton YAML
resources.

The application has been designed around the following principles:

- Have a quarkus standalone application able to generate different Tekton resources for a specific provider: Tekton, Konflux, etc
- Support to provide the needed parameters or configuration using a YAML configurator file
- Generate using the Fabric8 kubernetes Fluent API & Builder the resources using [Tekton model v1](https://github.com/fabric8io/kubernetes-client/tree/main/extensions/tekton/model-v1/)
- Propose `Factories` able to generate the Tekton resources such as: params, labels, workspaces, results, finally using `defaulting` values or YAML content from the configuration file
- Support to specify a domain/group: `example, build, etc` to organize the different resources generated

**Note**: This project is complementary to what Dekorate can populate today for [Tekton](https://github.com/dekorateio/dekorate/tree/main/annotations/tekton-annotations) !

## How to use it

Git clone the project and compile the code:

```bash
./mvnw package
```

Create a configuration YAML file where you will define the following parameters:
 - The `pipeline` provider to be used: `konflux` or `tekton`
 - The `domain` to group the generated files under the output path
 - A job with their parameters
```bash
cat <<EOF > my-config.yaml
type: tekton
domain: example

# A job represents a collection of kubernetes resources able to perform different tasks, steps
job:
  # One of the supported resources: PipelineRun, Pipeline, Task
  resourceType: PipelineRun
  name: pipeline-1
EOF
```
and launch it:
```bash
java -jar target/quarkus-app/quarkus-run.jar -c my-config.yaml -o out/flows
```  

Next, check the pipeline(s) generated under `./out/flows/<domain>`

**Remark**: Use the parameter `-h` to get the help usage of the application

The `configurations` folder proposes different YAML configurations of what you can do :-)

## Some scenario ideas

See some idea of scenario [here](SCENARIO.md)

## To be parked

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

To get the list of the konflux tekton bundles (oci or git) supported/trusted:
```bash
## See tool doc: https://www.conftest.dev/
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




