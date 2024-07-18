package dev.snowdrop.factory;

import dev.snowdrop.model.Configurator;

import java.util.Map;

public interface LabelsProvider {
   Map<String, String> getPipelineLabels(Configurator cfg);
}
