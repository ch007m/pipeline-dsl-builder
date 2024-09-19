package dev.snowdrop.factory;

public class JobFactory {
    public static JobBuilder withProvider(Type providerType) {
        JobProvider provider = createProvider(providerType.toString()); // convert string to enum if needed
        return new JobBuilder(provider);
    }

    private static JobProvider createProvider(String provider) {
        switch (provider.toUpperCase()) {
            case "KONFLUX":
                return new dev.snowdrop.factory.konflux.pipeline.Pipelines();
            case "TEKTON":
                return new dev.snowdrop.factory.tekton.pipeline.Pipelines();
            default:
                throw new IllegalArgumentException("Unknown provider type: " + provider);
        }
    }
}
