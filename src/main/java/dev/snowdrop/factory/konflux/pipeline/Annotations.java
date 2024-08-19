package dev.snowdrop.factory.konflux.pipeline;

import dev.snowdrop.factory.AnnotationsProvider;
import dev.snowdrop.model.Configurator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Annotations implements AnnotationsProvider {
   @Override
   public Map<String, String> getPipelineAnnotations(Configurator cfg) {
      Map<String, String> annotations = Map.of(
        "build.appstudio.openshift.io/repo",cfg.getRepository().getName() + "?rev={{revision}}",
        "build.appstudio.redhat.com/commit_sha","{{revision}}",
        "build.appstudio.redhat.com/target_branch","{{target_branch}}",
        "pipelinesascode.tekton.dev/max-keep-runs","3",
        "pipelinesascode.tekton.dev/on-cel-expression","event == 'push' && target_branch == 'main'"
      );

      return annotations.entrySet()
          .stream()
          .sorted(Map.Entry.comparingByValue()) // Sort by value
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              Map.Entry::getValue,
              (oldValue, newValue) -> oldValue,
              LinkedHashMap::new  // Use LinkedHashMap to maintain insertion order
          ));
   }
}