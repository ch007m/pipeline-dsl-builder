package dev.snowdrop.factory.pipeline.konflux;

import io.fabric8.tekton.pipeline.v1.PipelineWorkspaceDeclaration;
import io.fabric8.tekton.pipeline.v1.PipelineWorkspaceDeclarationBuilder;

import java.util.ArrayList;
import java.util.List;

public class Workspaces {

   public static List<PipelineWorkspaceDeclaration> KONFLUX_PIPELINE_WORKSPACES() {
      List<PipelineWorkspaceDeclaration> wks = new ArrayList<>();
      wks.add(new PipelineWorkspaceDeclarationBuilder().withName("workspace").build());
      wks.add(new PipelineWorkspaceDeclarationBuilder().withName("git-auth").withOptional(true).build());
      wks.add(new PipelineWorkspaceDeclarationBuilder().withName("netrc").withOptional(true).build());
      return wks;
   }
}
