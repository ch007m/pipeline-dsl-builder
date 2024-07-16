package dev.snowdrop;

import static dev.snowdrop.service.CodeGeneratorSvc.*;

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

      if (type.equals("workspaces")) {
         generateWorkspacesFromYAML(args[1]);
      }
   }
}
