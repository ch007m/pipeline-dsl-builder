package dev.snowdrop.command;

import dev.snowdrop.command.fetch.GitFetchCommand;
import dev.snowdrop.command.fetch.OCIBundleFetchCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
    subcommands = {
        BuilderCommand.class,
        GitFetchCommand.class,
        OCIBundleFetchCommand.class
    },
    mixinStandardHelpOptions = true,
    description = "Tekton commands")
public class AllCommand {
}
