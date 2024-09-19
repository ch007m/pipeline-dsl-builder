package dev.snowdrop.command.fetch;

import dev.snowdrop.model.oci.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static dev.snowdrop.service.RemoteTaskSvc.*;

@CommandLine.Command(name = "extractBundle", description = "Fetch and extract the YAML resources from OCI bundle")
public class OCIBundleFetchCommand implements Runnable {

    private static final String OUTPUT_TEKTON_PATH = "temp/oci/tekton";

    @CommandLine.Option(names = {"-b", "--bundle"}, description = "The OCI bundle URL", required = true)
    String url;

    @CommandLine.Option(names = {"-p", "--path"}, description = "Path where files will be extracted", required = true)
    String path;

    private static final Logger logger = LoggerFactory.getLogger(OCIBundleFetchCommand.class);

    @Override
    public void run() {
        // Fetch the OCI bundle and extract it using oras CLI
        grabOCIBundle(url, path);

        // Search about Blob layers from extracted OCI bundle
        List<Manifest.Layer> layers = searchBlobLayers(path);

        // Extract from the BLOB layer the json Task where annotation of the digest is "dev.tekton.image.name"
        // and convert it to its YAML file
        if (layers != null && !layers.isEmpty()) {
            List<Manifest.Layer> filteredLayers = filterLayersUsingAnnotation(TASK_DIGEST_ANNOTATION, layers, path);

            filteredLayers.stream().forEach(layer -> {
                String blobFile = Paths.get(path , "blobs/sha256" , layer.getDigest().substring(7)).toString();

                // Extract from the BLOB file the task(s)
                List<String> jsonFiles = extractTasksFromBlob(new File(blobFile));

                // Convert json to YAML
                jsonFiles.stream().forEach(file -> {
                    Path tasksPath = Paths.get(path, "tasks");
                    String jsonFileName = file.substring(file.lastIndexOf('/') + 1);
                    convertJSONtoYAML(tasksPath, jsonFileName);
                });
            });
        } else {
            logger.error("No layers found for the oci bundle !");
        }
    }
}
