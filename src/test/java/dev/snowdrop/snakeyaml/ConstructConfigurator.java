package dev.snowdrop.snakeyaml;

import dev.snowdrop.model.Configurator;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.nodes.Node;

public class ConstructConfigurator extends AbstractConstruct {
    public Object construct(Node node) {
        // TODO How can we get from the node the object configurator which has been created from the YAML parsed
        Configurator cfg = new Configurator();
        // You can add custom initialization here
        return cfg;
    }
}
