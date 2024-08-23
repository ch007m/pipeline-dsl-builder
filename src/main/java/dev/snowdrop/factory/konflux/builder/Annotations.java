package dev.snowdrop.factory.konflux.builder;

import dev.snowdrop.model.Configurator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Annotations {

    private final static String COMPONENT_ANNOTATION = "{\"visibility\":\"public\"}";

    public static Map<String, String> getApplicationAnnotations(Configurator cfg) {
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

    public static Map<String, String> getComponentAnnotations(Configurator cfg) {
        Map<String, String> annotations = Map.of(
            "build.appstudio.openshift.io/request","configure-pac",
            "image.redhat.com/generate",COMPONENT_ANNOTATION
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
