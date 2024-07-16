package dev.snowdrop.factory;

import dev.snowdrop.model.Configurator;

import java.util.Map;

public interface AnnotationsProvider {
   Map<String, String> getPipelineAnnotations(Configurator cfg);
}
