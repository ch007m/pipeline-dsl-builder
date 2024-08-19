package dev.snowdrop;

import dev.snowdrop.model.Configurator;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.nodes.Node;

public class ConstructConfigurator extends AbstractConstruct {
    public Object construct(Node node) {
        Configurator cfg = new Configurator();
        // You can add custom initialization here
        return cfg;
    }
}
