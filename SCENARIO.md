# Scenario

## Provider: konflux

### PipelineRun performing a pack build

Command to be executed: 
```bash
java -jar target/quarkus-app/quarkus-run.jar -o out/flows -c configurations/konflux/build-quarkus-cfg.yaml
```
using as configuration: 
```yaml
# configurations/konflux/build-quarkus-cfg.yaml
```
Generated file: 
```yaml
# generated/konflux/build/build-quarkus-cfg.yaml
```
## Provider: konflux

### PipelineRun using the pack client to build a builder image

Command to be executed: 
```bash
java -jar target/quarkus-app/quarkus-run.jar -o out/flows -c configurations/konflux/buildpack-builder-cfg.yaml
```
using as configuration: 
```yaml
# configurations/konflux/buildpack-builder-cfg.yaml
```
Generated file: 
```yaml
# generated/konflux/buildpack/buildpack-builder-cfg.yaml
```
## Provider: tekton

### This Pipeline builds a builder image using the pack CLI.

Command to be executed: 
```bash
java -jar target/quarkus-app/quarkus-run.jar -o out/flows -c configurations/tekton/pack-builder-cfg.yaml
```
using as configuration: 
```yaml
# configurations/tekton/pack-builder-cfg.yaml
```
Generated file: 
```yaml
# generated/tekton/buildpack/pack-builder-cfg.yaml
```
## Provider: tekton

### Simple example of a Tekton pipeline echoing a message

Command to be executed: 
```bash
java -jar target/quarkus-app/quarkus-run.jar -o out/flows -c configurations/tekton/simple-job-embedded-script-cfg.yaml
```
using as configuration: 
```yaml
# configurations/tekton/simple-job-embedded-script-cfg.yaml
```
Generated file: 
```yaml
# generated/tekton/example/simple-job-embedded-script-cfg.yaml
```
## Provider: tekton

### Simple example of a Tekton pipeline echoing a message

Command to be executed: 
```bash
java -jar target/quarkus-app/quarkus-run.jar -o out/flows -c configurations/tekton/simple-job-fetch-script-cfg.yaml
```
using as configuration: 
```yaml
# configurations/tekton/simple-job-fetch-script-cfg.yaml
```
Generated file: 
```yaml
# generated/tekton/example/simple-job-fetch-script-cfg.yaml
```
