# Workflow YAML Generator

![e2e testing workflow](https://github.com/ch007m/pipeline-dsl-builder/actions/workflows/e2e-testing.yaml/badge.svg)

The Workflow YAML Generator tool simplifies the life of the users when they play with a Workflow engine as Tekton, [Konflux](https://konflux-ci.dev/) to generate their corresponding YAML resource file from a configuration file containing the definition about the Job and the actions/steps using a `common/unified` language !

The application has been designed around the following principles:

- Have a quarkus standalone application able to generate different YAML resource files for a provider: Tekton, Konflux, etc
- Define the Job and actions using a `common` language
- Generate using the Fabric8 kubernetes Fluent API & Builder the resources using [Tekton model v1](https://github.com/fabric8io/kubernetes-client/tree/main/extensions/tekton/model-v1/)
- Propose `Factories` able to generate the Tekton resources such as: params, labels, workspaces, results, finally using `defaulting` values or YAML content from the configuration file
- Support to specify a domain/group: `example, build, etc` to organize the different resources generated

**Note**: This project is complementary to what Dekorate can populate today for [Tekton](https://github.com/dekorateio/dekorate/tree/main/annotations/tekton-annotations) !

## How to use it

Git clone the project and create the jar file of the standalone Quarkus application:

```bash
./mvnw package
```

Create a configuration YAML file where you define the parameters as:
 - The workflow provider to be used: `konflux` or `tekton`
 - The `domain` to group the generated files under the output path
 - A job
```bash
cat <<EOF > my-config.yaml
provider: tekton
resourceType: PipelineRun
domain: example

job:
  name: simple-job-embedded-script
  description: Simple example of a Tekton pipeline echoing a message

  actions:
  - name: say-hello
    # The script to be executed using a linux container
    script: |
      #!/usr/bin/env bash
      
      set -e
      echo "Say Hello"
```
and launch it:
```bash
java -jar target/quarkus-app/quarkus-run.jar builder -c my-config.yaml -o out/flows
```  

Next, check the pipeline(s) generated under `./out/flows/<domain>`

**Remark**: Use the parameter `-h` to get the help usage of the application

The [configurations](./configurations) folder proposes different YAML config examples of what you can do :-)

## Some scenario ideas

Some examples of configuration are described part of this [document](SCENARIO.md) - Enjoy :-)
