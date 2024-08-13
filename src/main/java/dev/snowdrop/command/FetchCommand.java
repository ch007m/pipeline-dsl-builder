package dev.snowdrop.command;

import dev.snowdrop.command.fetch.GitFetchCommand;
import dev.snowdrop.command.fetch.OCIBundleFetchCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
    mixinStandardHelpOptions = true,
    subcommands = {
        OCIBundleFetchCommand.class,
        GitFetchCommand.class,
    })
public class FetchCommand {
}
