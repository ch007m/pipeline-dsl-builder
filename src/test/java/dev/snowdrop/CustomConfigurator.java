package dev.snowdrop;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;

class CustomConfigurator extends Constructor {
    public CustomConfigurator(LoaderOptions loaderOptions) {
        super(loaderOptions);

        /*
        TypeDescription configuratorDescription = new TypeDescription(Configurator.class);
        configuratorDescription.addPropertyParameters("application", Application.class);
        configuratorDescription.addPropertyParameters("component", Component.class);
        configuratorDescription.addPropertyParameters("repository", Repository.class);

        this.addTypeDescription(configuratorDescription);
        */
        this.yamlConstructors.put(new Tag("!dev.snowdrop.Configurator"), new ConstructConfigurator());
    }
}
