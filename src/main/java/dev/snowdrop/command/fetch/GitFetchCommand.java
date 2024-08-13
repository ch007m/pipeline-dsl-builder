package dev.snowdrop.command.fetch;

import picocli.CommandLine;

@CommandLine.Command(name = "fetchURL", description = "Fetch and extract the YAML resources from OCI bundle")
public class GitFetchCommand implements Runnable {

    @CommandLine.Option(names = {"-u", "--url"}, description = "The url of the YAML resource to fetch", required = true)
    String configuration;

    @CommandLine.Option(names = {"-p", "--path"}, description = "Path where files will be extracted", required = true)
    String path;

    @Override
    public void run() {

    }
}
