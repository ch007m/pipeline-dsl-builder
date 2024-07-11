## Tekton Pipeline YAML Generator

The goal of this Quarkus Application is to simplify the life of the users when they play with Tekton or any compliant project like [Konflux](https://konflux-ci.dev/) which uses an opinionated pipeline's [template](https://github.com/konflux-ci/build-definitions/blob/main/pipelines/template-build/template-build.yaml) to generate the required Tekton YAML
resources.

**Note**: This project is complementary to what Dekorate can populate today for [Tekton](https://github.com/dekorateio/dekorate/tree/main/annotations/tekton-annotations) !

The application has been designed around the following principles:

- Have a quarkus standalone application able to generate different Tekton resources for a specific flavor: Tekton, Konflux, etc
- Support to provide the needed parameters or configuration using a YAML configurator file
- Generate using the Fabric8 kubernetes Fluent API & Builder the resources using [Tekton model v1](https://github.com/fabric8io/kubernetes-client/tree/main/extensions/tekton/model-v1/)
- Propose Java `Factories` able to generate the params, labels, workspaces, results, finally tekton objects using `defaulting` values or YAML content from the configuration file

### How to use it

Git clone the project and package the application:

```shell script
./mvnw package
```

Create a configuration YAML file:
```bash
cat <<EOF > my-config.yaml
flavor: konflux
builder:
  name: ubi-buildpacks-builder-pipeline
EOF

```
and launch it:
```bash
java -jar target/quarkus-app/quarkus-run.jar -c my-config.yaml -o out/flows
```  

Next, check the pipeline(s) generated under `./out/flows`

**Remark**: Use the parameter `-h` to get the help usage of the application

