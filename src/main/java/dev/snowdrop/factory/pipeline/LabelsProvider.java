package dev.snowdrop.factory.pipeline;

import java.util.Map;

public interface LabelsProvider {
   Map<String, String> getPipelineLabels();
}
