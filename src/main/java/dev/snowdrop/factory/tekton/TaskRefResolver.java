package dev.snowdrop.factory.tekton;

import dev.snowdrop.model.Bundle;
import io.fabric8.tekton.pipeline.v1.ParamValue;
import io.fabric8.tekton.pipeline.v1.TaskRef;
import io.fabric8.tekton.pipeline.v1.TaskRefBuilder;

public class TaskRefResolver {
    // TODO: Create a factory to populate the correct TaskRef :
    // Bundles: https://tekton.dev/docs/pipelines/bundle-resolver/
    // Git: https://tekton.dev/docs/pipelines/git-resolver/
    // HTTP: https://tekton.dev/docs/pipelines/http-resolver/
    // Others: https://tekton.dev/search/?q=resolver

    public static TaskRef withReference(Bundle b, String taskName) {
        String refType = b.getProtocol();
        switch (refType) {
            case "bundle":
                return withBundle(b, taskName);
            case "url":
                return withHttp(b, taskName);
            case "git":
                return withGit(b, taskName);
            default:
                throw new IllegalArgumentException("Unsupported reference type: " + b.getProtocol());
        }
    }

    private static TaskRef withHttp(Bundle bundle, String taskName) {
        return new TaskRefBuilder()
            // @formatter:off
            .withResolver("http")
            .withParams()
               .addNewParam()
                  .withName("url")
                  .withValue(new ParamValue(bundle.getUri())).endParam()
            .build();
            // @formatter:on
    }

    public static TaskRef withBundle(Bundle bundle, String taskName) {
        return new TaskRefBuilder()
            // @formatter:off
            .withResolver("bundles")
            .withParams()
               .addNewParam().withName(bundle.getProtocol()).withValue(new ParamValue(bundle.getUri())).endParam()
               // The name of the task to be fetched should be equal to the name of the Action's name !!
               .addNewParam().withName("name").withValue(new ParamValue(taskName)).endParam()
               .addNewParam().withName("kind").withValue(new ParamValue("task")).endParam()
            .build();
            // @formatter:on
    }

    public static TaskRef withGit(Bundle bundle, String taskName) {
        return new TaskRefBuilder()
            // @formatter:off
            .withResolver("git")
            .withParams()
               .addNewParam().withName("url").withValue(new ParamValue(bundle.getUri())).endParam()
               // TODO: Find a way to get the revision/branch
               .addNewParam().withName("revision").withValue(new ParamValue("main")).endParam()
               // TODO: Find a way to get the pathInRepo
               .addNewParam().withName("pathInRepo").withValue(new ParamValue(".")).endParam()
            .build();
            // @formatter:on
    }
}
