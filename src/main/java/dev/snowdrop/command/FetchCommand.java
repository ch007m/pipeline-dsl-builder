package dev.snowdrop.command;

import dev.snowdrop.command.fetch.GitFetchCommand;
import dev.snowdrop.command.fetch.OCIBundleFetchCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
    mixinStandardHelpOptions = true,
    name = "fetch",
    description = "Fetch the task from OCI bundle, git repository",
    subcommands = {
        OCIBundleFetchCommand.class,
        GitFetchCommand.class,
    })
public class FetchCommand {
}
