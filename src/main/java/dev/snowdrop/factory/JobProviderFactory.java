package dev.snowdrop.factory;

public class JobProviderFactory {
    public static JobProvider getProvider(Type type) {
        switch (type) {
            case KONFLUX:
                return new dev.snowdrop.factory.konflux.pipeline.Pipelines();
            case TEKTON:
                return new dev.snowdrop.factory.tekton.pipeline.Pipelines();
            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    }
}
