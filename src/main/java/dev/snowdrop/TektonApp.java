package dev.snowdrop;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;

@QuarkusMain
@TopCommand
@CommandLine.Command(
    subcommands = {
        BuilderCommand.class,
        OCIBundleCommand.class
    },
    mixinStandardHelpOptions = true,
    description = "Tekton commands")
public class TektonApp implements QuarkusApplication {
    @Inject
    CommandLine.IFactory factory;

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory).execute(args);
    }
}
