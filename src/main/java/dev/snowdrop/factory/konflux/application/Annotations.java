package dev.snowdrop.factory.konflux.application;

import dev.snowdrop.model.Configurator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Annotations {
    public Map<String, String> get(Configurator cfg) {
        Map<String, String> annotations = Map.of("application.thumbnail","10");

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
