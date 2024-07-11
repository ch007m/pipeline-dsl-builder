package dev.snowdrop.factory;

import java.util.Map;

public interface LabelsProvider {
   Map<String, String> getPipelineLabels();
}
