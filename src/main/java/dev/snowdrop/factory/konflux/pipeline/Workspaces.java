package dev.snowdrop.factory.konflux.pipeline;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.VolumeResourceRequirementsBuilder;
import io.fabric8.tekton.pipeline.v1.PipelineWorkspaceDeclaration;
import io.fabric8.tekton.pipeline.v1.PipelineWorkspaceDeclarationBuilder;
import io.fabric8.tekton.pipeline.v1.WorkspaceBinding;
import io.fabric8.tekton.pipeline.v1.WorkspaceBindingBuilder;

import java.util.ArrayList;
import java.util.List;

public class Workspaces {

   @Deprecated
   public static List<PipelineWorkspaceDeclaration> KONFLUX_PIPELINE_WORKSPACES() {
      List<PipelineWorkspaceDeclaration> wks = new ArrayList<>();
      wks.add(new PipelineWorkspaceDeclarationBuilder().withName("workspace").build());
      wks.add(new PipelineWorkspaceDeclarationBuilder().withName("git-auth").withOptional(true).build());
      wks.add(new PipelineWorkspaceDeclarationBuilder().withName("netrc").withOptional(true).build());
      return wks;
   }

   public static List<WorkspaceBinding> KONFLUX_PIPELINERUN_WORKSPACES() {
      List<WorkspaceBinding> wks = new ArrayList<>();
      wks.add(new WorkspaceBindingBuilder()
          .withName("workspace")
          .withVolumeClaimTemplate(
              new PersistentVolumeClaimBuilder()
                  .editOrNewSpec().withResources(
                      new VolumeResourceRequirementsBuilder()
                          .addToRequests("storage",new Quantity("1Gi"))
                          .build())
                  .addToAccessModes("ReadWriteOnce").endSpec()
                  .build()
          )
          .build());
      wks.add(new WorkspaceBindingBuilder()
          .withName("git-auth")
          .withSecret(new SecretVolumeSourceBuilder()
              .withSecretName("{{ git_auth_secret }}")
              .build())
          .build()
          );
      return wks;
   }
}
