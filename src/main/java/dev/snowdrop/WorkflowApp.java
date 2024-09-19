package dev.snowdrop;

import dev.snowdrop.command.AllCommand;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;

@QuarkusMain
public class WorkflowApp {

    public static void main(String[] args) {
        Quarkus.run(AppCommands.class, args);
    }

    public static class AppCommands implements QuarkusApplication {
        @Inject
        CommandLine.IFactory factory;

        @Override

        public int run(String... args) throws Exception {
            return new CommandLine(new AllCommand(), factory).execute(args);
        }

    }
}
