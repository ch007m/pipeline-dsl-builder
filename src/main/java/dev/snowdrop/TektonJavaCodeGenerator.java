package dev.snowdrop;

import static dev.snowdrop.service.CodeGeneratorSvc.generateParamsFromYAML;
import static dev.snowdrop.service.CodeGeneratorSvc.generateTaskFromYAML;

public class TektonJavaCodeGenerator {
   public static void main(String[] args) {
      if (args.length < 2) {
         System.err.println("Please provide as arguments; the type and the YAML file path: ");
         System.err.println("TektonJavaCodeGenerator params files/konflux_template/params.yaml");
         System.exit(1);
      }

      String type = args[0];
      if (type.equals("params")) {
         generateParamsFromYAML(args[1]);
      }

      if (type.equals("task")) {
         generateTaskFromYAML(args[1]);
      }

      // apiVersion: tekton.dev/v1
      //kind: Task
      //metadata:
      //  name: git-clone
      //spec:
      //  params:
      //    - name: GIT_PROJECT_URL
      //      description: The git repository url (e.g. http://github.com/myorg/myrepo.git)
      //      type: string
      //  workspaces:
      //    - name: source-dir
      //      description: A workspace for the task
      //      optional: true
      //      mountPath: /mnt/workspace
      //  steps:
      //    - name: clone
      //      image: quay.io/redhat-cop/ubi8-git:v1.0
      //      workingDir: $(workspaces.source-dir.path)
      //      command:
      //        - sh
      //        - -c
      //      args:
      //        - git clone $(params.GIT_PROJECT_URL) .
   }
}
