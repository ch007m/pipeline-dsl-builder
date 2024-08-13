# Scenario

## Provider: konflux

### PipelineRun performing a pack build

Command to be executed: 
```bash
java -jar target/quarkus-app/quarkus-run.jar builder -o out/flows -c configurations/konflux/build-quarkus-cfg.yaml
```
using as configuration: 
```yaml
# configurations/konflux/build-quarkus-cfg.yaml

# The type will be used by the application to generate the resources for the selected provider: konflux, tekton
type: konflux
# The domain allows to specify for Konflux the type of the build-container - https://github.com/konflux-ci/build-definitions/blob/main/pipelines/template-build/template-build.yaml#L112
# to be executed.
# Such a type matches a corresponding Task which is either:
# - pack: to build an image using the Pack CLI
# - build: to build an application using a builder image
# - builder: to create a builder image
# - stack: to create a base stack image build or run
# - meta/composite: to package the buildpacks of a "meta/composite" buildpack project
# - buildpack: to package a "buildpack" project
# - extension: to package an "extension" project
domain: build
namespace: user1

repository:
  name: https://github.com/ch007m/new-quarkus-app-1
  branch: main

job:
  # One of the supported resources: PipelineRun, Pipeline
  resourceType: PipelineRun
  name: my-quarkus-1
  description: PipelineRun performing a pack build


```
Generated file: 
```yaml
# generated/konflux/build/pipelinerun-my-quarkus-1.yaml

---
apiVersion: "tekton.dev/v1"
kind: "PipelineRun"
metadata:
  annotations:
    build.appstudio.redhat.com/commit_sha: "{{revision}}"
    build.appstudio.redhat.com/target_branch: "{{target_branch}}"
    pipelinesascode.tekton.dev/on-cel-expression: "event == 'push' && target_branch\
      \ == 'main'"
    build.appstudio.openshift.io/repo: "https://github.com/ch007m/new-quarkus-app-1?rev={{revision}}"
    pipelinesascode.tekton.dev/max-keep-runs: "3"
  labels:
    pipelines.openshift.io/strategy: "build"
    pipelines.openshift.io/runtime: "java"
    pipelines.openshift.io/used-by: "build-cloud"
  name: "my-quarkus-1"
spec:
  params:
  - name: "git-url"
    value: "{{source_url}}"
  - name: "revision"
    value: "{{revision}}"
  - name: "dockerfile"
    value: "src/main/docker/Dockerfile.jvm"
  - name: "output-image"
    value: "quay.io/ch007m/user-ns1/my-quarkus/quarkus-1:{{revision}}"
  - name: "path-context"
    value: "."
  pipelineSpec:
    finally:
    - name: "show-sbom"
      params:
      - name: "IMAGE_URL"
        value: "$(tasks.build-container.results.IMAGE_URL)"
      taskRef:
        params:
        - name: "bundle"
          value: "Bundle not found"
        - name: "name"
          value: "show-sbom"
        - name: "kind"
          value: "task"
        resolver: "bundles"
    - name: "show-summary"
      params:
      - name: "pipelinerun-name"
        value: "$(context.pipelineRun.name)"
      - name: "git-url"
        value: "$(tasks.clone-repository.results.url)?rev=$(tasks.clone-repository.results.commit)"
      - name: "image-url"
        value: "$(params.output-image)"
      - name: "build-task-status"
        value: "$(tasks.build-container.status)"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/konflux-ci/tekton-catalog/task-summary:0.2@sha256:4b0563bcb5a070b9f7a783bfb831941d4fe5fa42bbb732a63c63f8f7936d4467"
        - name: "name"
          value: "summary"
        - name: "kind"
          value: "task"
        resolver: "bundles"
    results:
    - name: "IMAGE_URL"
      value: "$(tasks.build-container.results.IMAGE_URL)"
    - name: "IMAGE_DIGEST"
      value: "$(tasks.build-container.results.IMAGE_DIGEST)"
    - name: "JAVA_COMMUNITY_DEPENDENCIES"
      value: "$(tasks.build-container.results.JAVA_COMMUNITY_DEPENDENCIES)"
    - name: "CHAINS-GIT_URL"
      value: "$(tasks.clone-repository.results.url)"
    - name: "CHAINS-GIT_COMMIT"
      value: "$(tasks.clone-repository.results.commit)"
    tasks:
    - name: "init"
      params:
      - name: "image-url"
        value: "$(params.output-image"
      - name: "rebuild"
        value: "$(params.rebuild"
      - name: "skip-checks"
        value: "$(params.skip-checks"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/konflux-ci/tekton-catalog/task-init:0.2@sha256:ceed8b7d5a3583cd21e7eea32498992824272a5436f17ce24c56c75919839e42"
        - name: "name"
          value: "init"
        - name: "kind"
          value: "task"
        resolver: "bundles"
    - name: "clone-repository"
      params:
      - name: "url"
        value: "$(params.git-url)"
      runAfter:
      - "git-clone"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/konflux-ci/tekton-catalog/task-git-clone:0.1@sha256:de0ca8872c791944c479231e21d68379b54877aaf42e5f766ef4a8728970f8b3"
        - name: "name"
          value: "git-clone"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      when:
      - input: "$(tasks.init.results.build)"
        operator: "in"
        values:
        - "true"
      workspaces:
      - name: "output"
        workspace: "workspace"
      - name: "basic-auth"
        workspace: "git-auth"
    - name: "prefetch-dependencies"
      params:
      - name: "input"
        value: "$(params.prefetch-input)"
      runAfter:
      - "clone-repository"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/konflux-ci/tekton-catalog/task-prefetch-dependencies:0.1@sha256:03e8293e6cc7d70a5f899751c6a4c2a25c3e3a6cfa7c437f9beca69638ce6988"
        - name: "name"
          value: "prefetch-dependencies"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      when:
      - input: "$(params.prefetch-input)"
        operator: "notin"
        values:
        - ""
      workspaces:
      - name: "source"
        workspace: "workspace"
      - name: "netrc"
        workspace: "netrc"
      - name: "git-basic-auth"
        workspace: "git-auth"
    - name: "build-container"
      params:
      - name: "IMAGE"
        value: "$(params.output-image)"
      - name: "DOCKERFILE"
        value: "$(params.dockerfile)"
      - name: "CONTEXT"
        value: "$(params.context-path)"
      - name: "HERMETIC"
        value: "$(params.hermetic)"
      - name: "PREFETCH_INPUT"
        value: "$(params.prefetch-input)"
      - name: "IMAGE_EXPIRES_AFTER"
        value: "$(params.image-expires-after)"
      - name: "COMMIT_SHA"
        value: "$(tasks.clone-repository.results.commit)"
      - name: "BUILD_ARGS_FILE"
        value:
        - "$(params.build-args[*])"
      - name: "BUILD_ARGS_FILE"
        value: "$(params.build-args-file)"
      runAfter:
      - "prefetch-dependencies"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/konflux-ci/tekton-catalog/task-buildah:0.1@sha256:0cb9100452e9640adbda75a6e23d2cc9c76d2408cbcf3183543b2a7582e39f02"
        - name: "name"
          value: "buildah"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      when:
      - input: "tasks.init.results.build"
        operator: "in"
        values:
        - "true"
      workspaces:
      - name: "source"
        workspace: "workspace"
    - name: "build-source-image"
      params:
      - name: "BINARY_IMAGE"
        value: "$(params.output-image)"
      - name: "BASE_IMAGES"
        value: "$(tasks.build-container.results.BASE_IMAGES_DIGESTS)"
      runAfter:
      - "build-container"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/konflux-ci/tekton-catalog/task-source-build:0.1@sha256:d1fe83481466a3b8ca91ba952f842689c9b9a63183b20fad6927cca10372f08a"
        - name: "name"
          value: "source-build"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      when:
      - input: "$(params.build-source-image)"
        operator: "in"
        values:
        - "true"
      workspaces:
      - name: "workspace"
        workspace: "workspace"
    - name: "deprecated-image-check"
      params:
      - name: "IMAGE_URL"
        value: "$(tasks.build-container.results.IMAGE_URL)"
      - name: "IMAGE_DIGEST"
        value: "$(tasks.build-container.results.IMAGE_DIGEST)"
      - name: "BASE_IMAGES_DIGESTS"
        value: "$(tasks.build-container.results.BASE_IMAGES_DIGESTS)"
      runAfter:
      - "build-container"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/konflux-ci/tekton-catalog/task-deprecated-image-check:0.4@sha256:48f8a4da120a4dec29da6e4faacee81d024324861474e10e0a7fcfcf56677249"
        - name: "name"
          value: "deprecated-image-check"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      when:
      - input: "$(params.skip-checks)"
        operator: "in"
        values:
        - "false"
    - name: "clair-scan"
      params:
      - name: "image-digest"
        value: "$(tasks.build-container.results.IMAGE_DIGEST)"
      - name: "image-url"
        value: "$(tasks.build-container.results.IMAGE_URL)"
      runAfter:
      - "build-container"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/konflux-ci/tekton-catalog/task-clair-scan:0.1@sha256:07f56dc7b7d77d394c6163f2682b3a72f8bd53e0f43854d848ee0173feb2b25d"
        - name: "name"
          value: "clair-scan"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      when:
      - input: "$(params.skip-checks)"
        operator: "in"
        values:
        - "false"
    - name: "ecosystem-cert-preflight-checks"
      params:
      - name: "image-url"
        value: "$(tasks.build-container.results.IMAGE_URL)"
      runAfter:
      - "build-container"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/konflux-ci/tekton-catalog/task-ecosystem-cert-preflight-checks:0.1@sha256:8838d3e1628dbe61f4851b3640d2e3a9a3079d3ff3da955f4a3e4c2c95a013df"
        - name: "name"
          value: "ecosystem-cert-preflight-checks"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      when:
      - input: "$(params.skip-checks)"
        operator: "in"
        values:
        - "false"
    - name: "sast-snyk-check"
      params:
      - name: "image-digest"
        value: "$(tasks.build-container.results.IMAGE_DIGEST)"
      - name: "image-url"
        value: "$(tasks.build-container.results.IMAGE_URL)"
      runAfter:
      - "build-container"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/konflux-ci/tekton-catalog/task-sast-snyk-check:0.1@sha256:d501cb1ff0f999a478a7fb8811fb501300be3f158aaedee663d230624d74d2b4"
        - name: "name"
          value: "sast-snyk-check"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      when:
      - input: "$(params.skip-checks)"
        operator: "in"
        values:
        - "true"
      workspaces:
      - name: "workspace"
        workspace: "workspace"
    - name: "clamav-scan"
      params:
      - name: "image-digest"
        value: "$(tasks.build-container.results.IMAGE_DIGEST)"
      - name: "image-url"
        value: "$(tasks.build-container.results.IMAGE_URL)"
      runAfter:
      - "build-container"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/konflux-ci/tekton-catalog/task-clamav-scan:0.1@sha256:45deb2d3cc6a23166831c7471882a0c8cc8a754365e0598e3e2022cbb1866375"
        - name: "name"
          value: "clamav-scan"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      when:
      - input: "$(params.skip-checks)"
        operator: "in"
        values:
        - "false"
    - name: "sbom-json-check"
      params:
      - name: "IMAGE_URL"
        value: "$(tasks.build-container.results.IMAGE_URL)"
      - name: "IMAGE_DIGEST"
        value: "$(tasks.build-container.results.IMAGE_DIGEST)"
      runAfter:
      - "build-container"
      taskRef:
        params:
        - name: "bundle"
          value: "quay.io/konflux-ci/tekton-catalog/task-sbom-json-check:0.1@sha256:03322cc79854aeba2a4f6ba48b35a97701297f153398a03917d166cfeebd2c08"
        - name: "name"
          value: "sbom-json-check"
        - name: "kind"
          value: "task"
        resolver: "bundles"
      when:
      - input: "$(params.skip-checks)"
        operator: "in"
        values:
        - "false"
  workspaces:
  - name: "workspace"
    volumeClaimTemplate:
      apiVersion: "v1"
      kind: "PersistentVolumeClaim"
      spec:
        accessModes:
        - "ReadWriteOnce"
        resources:
          requests:
            storage: "1Gi"
  - name: "git-auth"
    secret:
      secretName: "{{ git_auth_secret }}"

```
## Provider: konflux

### PipelineRun using the pack client to build a builder image

Command to be executed: 
```bash
java -jar target/quarkus-app/quarkus-run.jar builder -o out/flows -c configurations/konflux/buildpack-builder-cfg.yaml
```
using as configuration: 
```yaml
# configurations/konflux/buildpack-builder-cfg.yaml

# The type will be used by the application to generate the resources for the selected provider: konflux, tekton
type: konflux
# The domain allows to organize the resources, tasks to be generated
domain: buildpack
namespace: user1

repository:
  name: https://github.com/paketo-community/builder-ubi-base
  branch: main

job:
  # One of the supported resources: PipelineRun, Pipeline
  resourceType: Pipeline
  name: buildpack-builder
  description: PipelineRun using the pack client to build a builder image


```
Generated file: 
```yaml
# generated/konflux/buildpack/pipeline-buildpack-builder.yaml

---
apiVersion: "tekton.dev/v1"
kind: "Pipeline"
metadata:
  annotations:
    build.appstudio.redhat.com/commit_sha: "{{revision}}"
    pipelinesascode.tekton.dev/max-keep-runs: "3"
    build.appstudio.openshift.io/repo: "https://github.com/paketo-community/builder-ubi-base?rev={{revision}}"
    pipelinesascode.tekton.dev/on-cel-expression: "event == 'push' && target_branch\
      \ == 'main'"
    build.appstudio.redhat.com/target_branch: "{{target_branch}}"
  labels:
    pipelines.openshift.io/used-by: "build-cloud"
    pipelines.openshift.io/runtime: "java"
    pipelines.openshift.io/strategy: "buildpack"
  name: "buildpack-builder"
spec:
  finally:
  - name: "show-sbom"
    params:
    - name: "IMAGE_URL"
      value: "$(tasks.build-container.results.IMAGE_URL)"
    taskRef:
      params:
      - name: "bundle"
        value: "Bundle not found"
      - name: "name"
        value: "show-sbom"
      - name: "kind"
        value: "task"
      resolver: "bundles"
  - name: "show-summary"
    params:
    - name: "pipelinerun-name"
      value: "$(context.pipelineRun.name)"
    - name: "git-url"
      value: "$(tasks.clone-repository.results.url)?rev=$(tasks.clone-repository.results.commit)"
    - name: "image-url"
      value: "$(params.output-image)"
    - name: "build-task-status"
      value: "$(tasks.build-container.status)"
    taskRef:
      params:
      - name: "bundle"
        value: "quay.io/konflux-ci/tekton-catalog/task-summary:0.2@sha256:4b0563bcb5a070b9f7a783bfb831941d4fe5fa42bbb732a63c63f8f7936d4467"
      - name: "name"
        value: "summary"
      - name: "kind"
        value: "task"
      resolver: "bundles"
  params:
  - description: "Source Repository URL"
    name: "git-url"
    type: "string"
  - default: ""
    description: "Revision of the Source Repository"
    name: "revision"
    type: "string"
  - description: "Fully Qualified Output Image"
    name: "output-image"
    type: "string"
  - default: "."
    description: "The path to your source code"
    name: "path-context"
    type: "string"
  - default: "Dockerfile"
    description: "Path to the Dockerfile"
    name: "dockerfile"
    type: "string"
  - default: "false"
    description: "Force rebuild image"
    name: "rebuild"
    type: "string"
  - default: "false"
    description: "Skip checks against built image"
    name: "skip-checks"
    type: "string"
  - default: "false"
    description: "Execute the build with network isolation"
    name: "hermetic"
    type: "string"
  - default: ""
    description: "Build dependencies to be prefetched by Cachi2"
    name: "prefetch-input"
    type: "string"
  - default: "false"
    description: "Java build"
    name: "java"
    type: "string"
  - default: ""
    description: "Image tag expiration time, time values could be something like 1h,\
      \ 2d, 3w for hours, days, and weeks, respectively."
    name: "image-expires-after"
    type: "null"
  - default: "false"
    description: "Build a source image"
    name: "build-source-image"
    type: "string"
  - default: "[]"
    description: "Array of --build-arg values (\"arg=value\" strings) for buildah"
    name: "build-args"
    type: "array"
  - default: ""
    description: "Path to a file with build arguments for buildah, see https://www.mankier.com/1/buildah-build#--build-arg-file"
    name: "build-args"
    type: "string"
  - default: "true"
    description: "A boolean indicating whether we would like to execute a step"
    name: "enable-sbom"
    type: "string"
  - default: "table"
    description: "Format to be used to export/show the SBOM. Format available for\
      \ grype are 'json table cyclonedx cyclonedx-json sarif template'"
    name: "grype-sbom-format"
    type: "string"
  - default: "true"
    description: "Skip optional checks, set false if you want to run optional checks"
    name: "skip-optional"
    type: "string"
  - default: ""
    description: "Snyk Token Secret Name"
    name: "snyk-secret"
    type: "string"
  results:
  - name: "IMAGE_URL"
    value: "$(tasks.build-container.results.IMAGE_URL)"
  - name: "IMAGE_DIGEST"
    value: "$(tasks.build-container.results.IMAGE_DIGEST)"
  - name: "JAVA_COMMUNITY_DEPENDENCIES"
    value: "$(tasks.build-container.results.JAVA_COMMUNITY_DEPENDENCIES)"
  - name: "CHAINS-GIT_URL"
    value: "$(tasks.clone-repository.results.url)"
  - name: "CHAINS-GIT_COMMIT"
    value: "$(tasks.clone-repository.results.commit)"
  tasks:
  - name: "init"
    params:
    - name: "image-url"
      value: "$(params.output-image"
    - name: "rebuild"
      value: "$(params.rebuild"
    - name: "skip-checks"
      value: "$(params.skip-checks"
    taskRef:
      params:
      - name: "bundle"
        value: "quay.io/konflux-ci/tekton-catalog/task-init:0.2@sha256:ceed8b7d5a3583cd21e7eea32498992824272a5436f17ce24c56c75919839e42"
      - name: "name"
        value: "init"
      - name: "kind"
        value: "task"
      resolver: "bundles"
  - name: "clone-repository"
    params:
    - name: "url"
      value: "$(params.git-url)"
    runAfter:
    - "git-clone"
    taskRef:
      params:
      - name: "bundle"
        value: "quay.io/konflux-ci/tekton-catalog/task-git-clone:0.1@sha256:de0ca8872c791944c479231e21d68379b54877aaf42e5f766ef4a8728970f8b3"
      - name: "name"
        value: "git-clone"
      - name: "kind"
        value: "task"
      resolver: "bundles"
    when:
    - input: "$(tasks.init.results.build)"
      operator: "in"
      values:
      - "true"
    workspaces:
    - name: "output"
      workspace: "workspace"
    - name: "basic-auth"
      workspace: "git-auth"
  - name: "prefetch-dependencies"
    params:
    - name: "input"
      value: "$(params.prefetch-input)"
    runAfter:
    - "clone-repository"
    taskRef:
      params:
      - name: "bundle"
        value: "quay.io/konflux-ci/tekton-catalog/task-prefetch-dependencies:0.1@sha256:03e8293e6cc7d70a5f899751c6a4c2a25c3e3a6cfa7c437f9beca69638ce6988"
      - name: "name"
        value: "prefetch-dependencies"
      - name: "kind"
        value: "task"
      resolver: "bundles"
    when:
    - input: "$(params.prefetch-input)"
      operator: "notin"
      values:
      - ""
    workspaces:
    - name: "source"
      workspace: "workspace"
    - name: "netrc"
      workspace: "netrc"
    - name: "git-basic-auth"
      workspace: "git-auth"
  - name: "buildpacks-builder"
  - name: "build-source-image"
    params:
    - name: "BINARY_IMAGE"
      value: "$(params.output-image)"
    - name: "BASE_IMAGES"
      value: "$(tasks.build-container.results.BASE_IMAGES_DIGESTS)"
    runAfter:
    - "build-container"
    taskRef:
      params:
      - name: "bundle"
        value: "quay.io/konflux-ci/tekton-catalog/task-source-build:0.1@sha256:d1fe83481466a3b8ca91ba952f842689c9b9a63183b20fad6927cca10372f08a"
      - name: "name"
        value: "source-build"
      - name: "kind"
        value: "task"
      resolver: "bundles"
    when:
    - input: "$(params.build-source-image)"
      operator: "in"
      values:
      - "true"
    workspaces:
    - name: "workspace"
      workspace: "workspace"
  - name: "deprecated-image-check"
    params:
    - name: "IMAGE_URL"
      value: "$(tasks.build-container.results.IMAGE_URL)"
    - name: "IMAGE_DIGEST"
      value: "$(tasks.build-container.results.IMAGE_DIGEST)"
    - name: "BASE_IMAGES_DIGESTS"
      value: "$(tasks.build-container.results.BASE_IMAGES_DIGESTS)"
    runAfter:
    - "build-container"
    taskRef:
      params:
      - name: "bundle"
        value: "quay.io/konflux-ci/tekton-catalog/task-deprecated-image-check:0.4@sha256:48f8a4da120a4dec29da6e4faacee81d024324861474e10e0a7fcfcf56677249"
      - name: "name"
        value: "deprecated-image-check"
      - name: "kind"
        value: "task"
      resolver: "bundles"
    when:
    - input: "$(params.skip-checks)"
      operator: "in"
      values:
      - "false"
  - name: "clair-scan"
    params:
    - name: "image-digest"
      value: "$(tasks.build-container.results.IMAGE_DIGEST)"
    - name: "image-url"
      value: "$(tasks.build-container.results.IMAGE_URL)"
    runAfter:
    - "build-container"
    taskRef:
      params:
      - name: "bundle"
        value: "quay.io/konflux-ci/tekton-catalog/task-clair-scan:0.1@sha256:07f56dc7b7d77d394c6163f2682b3a72f8bd53e0f43854d848ee0173feb2b25d"
      - name: "name"
        value: "clair-scan"
      - name: "kind"
        value: "task"
      resolver: "bundles"
    when:
    - input: "$(params.skip-checks)"
      operator: "in"
      values:
      - "false"
  - name: "ecosystem-cert-preflight-checks"
    params:
    - name: "image-url"
      value: "$(tasks.build-container.results.IMAGE_URL)"
    runAfter:
    - "build-container"
    taskRef:
      params:
      - name: "bundle"
        value: "quay.io/konflux-ci/tekton-catalog/task-ecosystem-cert-preflight-checks:0.1@sha256:8838d3e1628dbe61f4851b3640d2e3a9a3079d3ff3da955f4a3e4c2c95a013df"
      - name: "name"
        value: "ecosystem-cert-preflight-checks"
      - name: "kind"
        value: "task"
      resolver: "bundles"
    when:
    - input: "$(params.skip-checks)"
      operator: "in"
      values:
      - "false"
  - name: "sast-snyk-check"
    params:
    - name: "image-digest"
      value: "$(tasks.build-container.results.IMAGE_DIGEST)"
    - name: "image-url"
      value: "$(tasks.build-container.results.IMAGE_URL)"
    runAfter:
    - "build-container"
    taskRef:
      params:
      - name: "bundle"
        value: "quay.io/konflux-ci/tekton-catalog/task-sast-snyk-check:0.1@sha256:d501cb1ff0f999a478a7fb8811fb501300be3f158aaedee663d230624d74d2b4"
      - name: "name"
        value: "sast-snyk-check"
      - name: "kind"
        value: "task"
      resolver: "bundles"
    when:
    - input: "$(params.skip-checks)"
      operator: "in"
      values:
      - "true"
    workspaces:
    - name: "workspace"
      workspace: "workspace"
  - name: "clamav-scan"
    params:
    - name: "image-digest"
      value: "$(tasks.build-container.results.IMAGE_DIGEST)"
    - name: "image-url"
      value: "$(tasks.build-container.results.IMAGE_URL)"
    runAfter:
    - "build-container"
    taskRef:
      params:
      - name: "bundle"
        value: "quay.io/konflux-ci/tekton-catalog/task-clamav-scan:0.1@sha256:45deb2d3cc6a23166831c7471882a0c8cc8a754365e0598e3e2022cbb1866375"
      - name: "name"
        value: "clamav-scan"
      - name: "kind"
        value: "task"
      resolver: "bundles"
    when:
    - input: "$(params.skip-checks)"
      operator: "in"
      values:
      - "false"
  - name: "sbom-json-check"
    params:
    - name: "IMAGE_URL"
      value: "$(tasks.build-container.results.IMAGE_URL)"
    - name: "IMAGE_DIGEST"
      value: "$(tasks.build-container.results.IMAGE_DIGEST)"
    runAfter:
    - "build-container"
    taskRef:
      params:
      - name: "bundle"
        value: "quay.io/konflux-ci/tekton-catalog/task-sbom-json-check:0.1@sha256:03322cc79854aeba2a4f6ba48b35a97701297f153398a03917d166cfeebd2c08"
      - name: "name"
        value: "sbom-json-check"
      - name: "kind"
        value: "task"
      resolver: "bundles"
    when:
    - input: "$(params.skip-checks)"
      operator: "in"
      values:
      - "false"
  workspaces:
  - name: "workspace"
  - name: "git-auth"
    optional: true
  - name: "netrc"
    optional: true

```
## Provider: tekton

### This Pipeline builds a builder image using the pack CLI.

Command to be executed: 
```bash
java -jar target/quarkus-app/quarkus-run.jar builder -o out/flows -c configurations/tekton/pack-builder-cfg.yaml
```
using as configuration: 
```yaml
# configurations/tekton/pack-builder-cfg.yaml

type: tekton
domain: buildpack
namespace:

job:
  # One of the supported resources: PipelineRun, Pipeline
  resourceType: PipelineRun
  name: pack-builder-push
  description: "This Pipeline builds a builder image using the pack CLI."
  params:
  - debug: true
  - git-url: "https://github.com/redhat-buildpacks/ubi-image-builder.git"
  - source-dir: "."
  - output-image: "quay.io/snowdrop/ubi-builder"
  - imageUrl: "buildpacksio/pack"
  - imageTag: "latest"
  - packCmdBuilderFlags:
    - -v
    - --publish
  # The workspaces declared here will be mounted for each action except if an action overrides it to use a different name
  workspaces:
    - name: pack-workspace
      volumeClaimTemplate:
        storage: 1Gi
        accessMode: ReadWriteOnce
    - name: source-dir
      volumeClaimTemplate:
        storage: 1Gi
        accessMode: ReadWriteOnce
    - name: data-store
      volumeSources:
        - secret: pack-config-toml
        - secret: gitea-creds # quay-creds, docker-creds, etc
  actions:
    - name: git-clone
      ref: bundle://quay.io/konflux-ci/tekton-catalog/task-git-clone:0.1@sha256:de0ca8872c791944c479231e21d68379b54877aaf42e5f766ef4a8728970f8b3
      params:
        - url: "$(params.git-url)"
        - subdirectory: "."
      workspaces:
        - name: output
          workspace: source-dir
    - name: fetch-packconfig-registrysecret
      ref: bundle://quay.io/ch007m/tekton-bundle:latest@sha256:af13b94347457df001742f8449de9edb381e90b0d174da598ddd15cf493e340f
    - name: list-source-workspace
      ref: bundle://quay.io/ch007m/tekton-bundle:latest@sha256:af13b94347457df001742f8449de9edb381e90b0d174da598ddd15cf493e340f
    - name: pack-builder
      ref: bundle://quay.io/ch007m/tekton-bundle:latest@sha256:af13b94347457df001742f8449de9edb381e90b0d174da598ddd15cf493e340f
      params:
        - PACK_SOURCE_DIR: "$(params.source-dir)"
        - PACK_CLI_IMAGE: "$(params.imageUrl)"
        - PACK_CLI_IMAGE_VERSION: "$(params.imageTag)"
        - BUILDER_IMAGE_NAME: "$(params.output-image)"
        - PACK_BUILDER_TOML: "ubi-builder.toml"
        - PACK_CMD_FLAGS:
          - "$(params.packCmdBuilderFlags)"


```
Generated file: 
```yaml
# generated/tekton/buildpack/pipelinerun-pack-builder-push.yaml

---
apiVersion: "tekton.dev/v1"
kind: "PipelineRun"
metadata:
  annotations:
    tekton.dev/displayName: "This Pipeline builds a builder image using the pack CLI."
    tekton.dev/pipelines.minVersion: "0.60.x"
    tekton.dev/platforms: "linux/amd64"
  labels:
    app.kubernetes.io/version: "0.1"
  name: "pack-builder-push"
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
      - name: "pack-workspace"
        workspace: "pack-workspace"
      - name: "data-store"
        workspace: "data-store"
      - name: "output"
        workspace: "source-dir"
    - name: "fetch-packconfig-registrysecret"
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
      - name: "pack-workspace"
        workspace: "pack-workspace"
      - name: "source-dir"
        workspace: "source-dir"
      - name: "data-store"
        workspace: "data-store"
    - name: "list-source-workspace"
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
      - name: "pack-workspace"
        workspace: "pack-workspace"
      - name: "source-dir"
        workspace: "source-dir"
      - name: "data-store"
        workspace: "data-store"
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
      - name: "pack-workspace"
        workspace: "pack-workspace"
      - name: "source-dir"
        workspace: "source-dir"
      - name: "data-store"
        workspace: "data-store"
  workspaces:
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
  - name: "data-store"
    projected:
      sources:
      - secret:
          name: "pack-config-toml"
      - secret:
          name: "gitea-creds"

```
## Provider: tekton

### Simple example of a Tekton pipeline echoing a message

Command to be executed: 
```bash
java -jar target/quarkus-app/quarkus-run.jar builder -o out/flows -c configurations/tekton/simple-job-embedded-script-cfg.yaml
```
using as configuration: 
```yaml
# configurations/tekton/simple-job-embedded-script-cfg.yaml

# The type will be used by the application to generate the resources for the selected provider: konflux, tekton
type: tekton
# The domain allows to organize the resources, tasks to be generated
domain: example

# Kubernetes namespace
namespace: user

job:
  name: simple-job-embedded-script # name of the pipeline to be created
  description: Simple example of a Tekton pipeline echoing a message

  # One of the supported resources: PipelineRun, Pipeline
  resourceType: PipelineRun

  actions:
    - name: say-hello
      # The ref or reference expressed using the uri://<task-name>:<url>
      # will fetch the code of the action to be executed
      ref:
      # The script to be executed using a linux container
      script: |
        #!/usr/bin/env bash
        
        set -e
        echo "Say Hello"
```
Generated file: 
```yaml
# generated/tekton/example/pipelinerun-simple-job-embedded-script.yaml

---
apiVersion: "tekton.dev/v1"
kind: "PipelineRun"
metadata:
  annotations:
    tekton.dev/pipelines.minVersion: "0.60.x"
    tekton.dev/displayName: "Simple example of a Tekton pipeline echoing a message"
    tekton.dev/platforms: "linux/amd64"
  labels:
    app.kubernetes.io/version: "0.1"
  name: "simple-job-embedded-script"
  namespace: "user"
spec:
  pipelineSpec:
    tasks:
    - name: "say-hello"
      taskSpec:
        steps:
        - image: "ubuntu"
          name: "run-script"
          script: |-
            #!/usr/bin/env bash

            set -e
            echo "Say Hello"

```
## Provider: tekton

### Simple example of a Tekton pipeline echoing a message

Command to be executed: 
```bash
java -jar target/quarkus-app/quarkus-run.jar builder -o out/flows -c configurations/tekton/simple-job-fetch-script-cfg.yaml
```
using as configuration: 
```yaml
# configurations/tekton/simple-job-fetch-script-cfg.yaml

# The type will be used by the application to generate the resources for the selected provider: konflux, tekton
type: tekton
# The domain allows to organize the resources, tasks to be generated
domain: example

# Kubernetes namespace
namespace:

job:
  name: simple-job-fetch-script # name of the pipeline to be created
  description: Simple example of a Tekton pipeline echoing a message

  # One of the supported resources: PipelineRun, Pipeline
  resourceType: PipelineRun

  actions:
    - name: say-hello
      # The ref or reference expressed using the uri://<task-name>:<url>
      # will fetch the code of the action to be executed
      ref:
      # The url of the script file to be executed using a linux container
      scriptFileUrl: https://raw.githubusercontent.com/ch007m/pipeline-dsl-builder/main/scripts/echo.sh
```
Generated file: 
```yaml
# generated/tekton/example/pipelinerun-simple-job-fetch-script.yaml

---
apiVersion: "tekton.dev/v1"
kind: "PipelineRun"
metadata:
  annotations:
    tekton.dev/displayName: "Simple example of a Tekton pipeline echoing a message"
    tekton.dev/pipelines.minVersion: "0.60.x"
    tekton.dev/platforms: "linux/amd64"
  labels:
    app.kubernetes.io/version: "0.1"
  name: "simple-job-fetch-script"
spec:
  pipelineSpec:
    tasks:
    - name: "say-hello"
      taskSpec:
        steps:
        - image: "ubuntu"
          name: "run-script"
          script: |
            #!/usr/bin/env bash

            set -e
            echo "Say Hello"

```
