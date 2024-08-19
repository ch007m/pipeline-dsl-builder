package dev.snowdrop.model.snakeyaml;

import dev.snowdrop.model.Configurator;
import dev.snowdrop.model.Repository;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;

public class CustomConstructor extends Constructor {

    public CustomConstructor() {
        super(new LoaderOptions());
    }

    @Override
    public Object newInstance(Node node) {
        if (node.getNodeId() == NodeId.mapping) {
            MappingNode mappingNode = (MappingNode) node;
            Object result = (Node) node;

            //
            if (result instanceof Repository) {
                Repository repository = (Repository) result;
                // Process `repository` as needed
                repository.setDockerfilePath(((Repository) result).getDockerfileUrl());
            }
            return result;
        }
        return new Configurator();
    }

}
