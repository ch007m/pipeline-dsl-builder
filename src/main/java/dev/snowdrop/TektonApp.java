package dev.snowdrop;

import dev.snowdrop.command.BuilderCommand;
// import dev.snowdrop.command.FetchCommand;
import dev.snowdrop.command.FetchCommand;
import dev.snowdrop.command.fetch.GitFetchCommand;
import dev.snowdrop.command.fetch.OCIBundleFetchCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;

@QuarkusMain
@TopCommand
@CommandLine.Command(
    subcommands = {
        BuilderCommand.class,
        //FetchCommand.class
        GitFetchCommand.class,
        OCIBundleFetchCommand.class
        // TODO: Verify with Quarkus team if we can use nested TopCommands to group them => FetchCommand.class
    },
    mixinStandardHelpOptions = true,
    description = "Tekton commands")
public class TektonApp {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new TektonApp()).execute(args);
        System.exit(exitCode);
    }
}
