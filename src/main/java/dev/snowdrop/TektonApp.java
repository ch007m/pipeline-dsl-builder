package dev.snowdrop;

import dev.snowdrop.command.BuilderCommand;
import dev.snowdrop.command.OCIBundleCommand;
import dev.snowdrop.command.YamlToJavaCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;

@QuarkusMain
@TopCommand
@CommandLine.Command(
    subcommands = {
        BuilderCommand.class,
        OCIBundleCommand.class,
        YamlToJavaCommand.class
    },
    mixinStandardHelpOptions = true,
    description = "Tekton commands")
public class TektonApp {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new TektonApp()).execute(args);
        System.exit(exitCode);
    }
}
