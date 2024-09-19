package dev.snowdrop.factory;

import dev.snowdrop.factory.konflux.KonfluxProvider;
import dev.snowdrop.factory.tekton.TektonProvider;

public class WorkflowBuilder {
    public Builder withProvider(Type providerType) {
        Provider provider = createProvider(providerType.toString()); // convert string to enum if needed
        return new Builder(provider);
    }

    private static Provider createProvider(String provider) {
        switch (provider.toUpperCase()) {
            case "KONFLUX":
                return new KonfluxProvider();
            case "TEKTON":
                return new TektonProvider();
            default:
                throw new IllegalArgumentException("Unknown provider type: " + provider);
        }
    }
}
