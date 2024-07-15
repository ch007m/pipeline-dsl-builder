## Tekton Pipeline YAML Generator

The goal of this Quarkus Application is to simplify the life of the users when they play with Tekton or any compliant project like [Konflux](https://konflux-ci.dev/) which uses an opinionated pipeline's [template](https://github.com/konflux-ci/build-definitions/blob/main/pipelines/template-build/template-build.yaml) to generate the required Tekton YAML
resources.

**Note**: This project is complementary to what Dekorate can populate today for [Tekton](https://github.com/dekorateio/dekorate/tree/main/annotations/tekton-annotations) !

The application has been designed around the following principles:

- Have a quarkus standalone application able to generate different Tekton resources for a specific flavor: Tekton, Konflux, etc
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
 - The flavor to be used: `konflux` or `tekton`
 - Select the `domain` such as: `buidpacks` and next the type: `builder` `stack`, `meta-buildpack`, `buildpack`, etc. The combination of the domain and the `type` will allow the tool to select the proper task, workspaces, finally, when, results, etc resources
```bash
cat <<EOF > my-config.yaml
# The flavor will be used to render the pipeline according to a specific provider: konflux, tekton
flavor: tekton

pipeline:
  # The domain allows to organize the resources, tasks to be used: example, buildpack
  domain: example
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
flavor: konflux
pipeline:
  domain: buildpack
  name: ubi-buildpacks-builder-pipeline
  builder:
    repository:
      name: https://github.com/paketo-community/builder-ubi-base
      branch: main
EOF
```

The `configuration-examples` folder proposes different YAML configuration of what you can configure :-)

