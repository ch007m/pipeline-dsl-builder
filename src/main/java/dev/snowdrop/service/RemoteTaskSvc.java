package dev.snowdrop.service;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.snowdrop.model.Bundle;
import dev.snowdrop.model.Job;
import dev.snowdrop.model.oci.Index;
import dev.snowdrop.model.oci.Manifest;
import dev.snowdrop.model.oci.ManifestEntry;
import io.fabric8.tekton.pipeline.v1.Task;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RemoteTaskSvc {
    private static final Logger logger = LoggerFactory.getLogger(RemoteTaskSvc.class);

    public static final String TASK_DIGEST_ANNOTATION = "dev.tekton.image.name";
    public static String BUNDLE_PREFIX = "bundle";

    public static void fetchExtractTask(Bundle b, String taskName, String path) {
        Map<String, String> tasksMap = new HashMap<>();
        String bundlePath = Paths.get(path, BUNDLE_PREFIX, taskName).toString();
        Path tasksPath = Paths.get(bundlePath, "tasks");

        // Create a Tekton task directory to extract the content
        try {
            Files.createDirectories(Path.of(bundlePath));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        switch (b.getProtocol()) {
            case "git": {
                logger.info("Fetching the git url: https://{}", b.getUri());
                try {
                    String taskContent = FileUtilSvc.fetchUrlRawContent(b.getUri());

                    String taskFileName = Paths.get(new URI(b.getUri()).getPath()).getFileName().toString();
                    Files.write(Paths.get(bundlePath, "tasks", taskFileName), taskContent.getBytes());
                    break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }

            case "http": {
                logger.info("Fetching the url: https://{}", b.getUri());
                try {
                    String taskUri = "https://" + b.getUri();
                    String taskContent = FileUtilSvc.fetchUrlRawContent(taskUri);

                    String taskFileName = Paths.get(new URI(taskUri).getPath()).getFileName().toString();
                    Files.createDirectories(Paths.get(bundlePath, "tasks"));
                    Files.write(Paths.get(bundlePath, "tasks", taskFileName), taskContent.getBytes());
                    break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }

            case "bundle": {
                // Fetch the OCI bundle
                logger.info("Task directory path: {}", bundlePath);
                grabOCIBundle(b.getUri(), bundlePath);

                // Search about Blob layers from extracted OCI bundle
                List<Manifest.Layer> layers = searchBlobLayers(bundlePath);

                // Extract from the BLOB layer the json Task where annotation of the digest is "dev.tekton.image.name"
                // and convert it to its YAML file
                if (layers != null && !layers.isEmpty()) {
                    List<Manifest.Layer> filteredLayers = filterLayersUsingAnnotation(TASK_DIGEST_ANNOTATION, layers, bundlePath);

                    filteredLayers.stream().forEach(layer -> {
                        String blobFile = Paths.get(bundlePath, "blobs/sha256", layer.getDigest().substring(7, layer.getDigest().length())).toString();

                        // Extract from the BLOB file the task(s)
                        List<String> jsonFiles = extractTasksFromBlob(new File(blobFile));

                        // Convert json file to YAML
                        jsonFiles.stream().forEach(jsonFile -> {
                            convertJSONtoYAML(tasksPath, jsonFile);
                        });
                    });
                } else {
                    logger.info("No layers found for the oci bundle !");
                }
            }
            break;
            default: {
                logger.info("Wrong protocol provided and not supported: {}", b.getProtocol());
            }
        }
    }

    public static void convertJSONtoYAML(Path path, String jsonFileName) {
        try {
            // Create the Tekton folder to copy the files
            Files.createDirectories(path);

            // Convert JSON to YAML
            String yaml = asYaml(Files.readString(Path.of(jsonFileName)));

            // Write YAML file
            // Define the YAML file name from the json file
            String yamlFileName = Paths.get(jsonFileName).getFileName().toString().replaceFirst("[.][^.]+$", ".yaml");
            Path yamlFilePath = Paths.get(path.toString(), yamlFileName);

            logger.info("Task yaml path: " + yamlFilePath);
            Files.write(yamlFilePath, yaml.getBytes());

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void grabOCIBundle(String oci, String outputPath) {
        String orasCommand = "oras copy " + oci + " --to-oci-layout " + outputPath;

        try {
            Process process = new ProcessBuilder(orasCommand.split(" "))
                .redirectErrorStream(true)
                .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info(line);
                }
            }

            int exitCode = process.waitFor();
            logger.info("Process exited with code: " + exitCode);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static List<Manifest.Layer> filterLayersUsingAnnotation(String annotation, List<Manifest.Layer> layers, String path) {
        return layers.stream()
            .filter(layer -> layer.getAnnotations() != null && layer.getAnnotations().containsKey(annotation))
            .peek(layer -> {
                String layerDigest = layer.getDigest();
                logger.info("Found task layer: " + layerDigest + " for task: " + layer.getAnnotations().get(annotation));
            })
            .collect(Collectors.toList());
    }

    public static List<Manifest.Layer> searchBlobLayers(String path) {
        // Search about the layer packaging the Tekton task
        try {
            File indexJsonfile = new File(path + "/index.json");
            Index index = new ObjectMapper().readValue(indexJsonfile, Index.class);

            // Get the mediaType digest => json file containing the layers
            ManifestEntry manifestEntry = index.getManifests().get(0);
            String mediaType = manifestEntry.getMediaType();
            AtomicReference<String> digest = new AtomicReference<>(manifestEntry.getDigest());
            String sha256 = digest.get().substring(7, digest.get().length());

            logger.info("mediaType: " + mediaType);
            logger.info("digest: " + digest);
            logger.info("sha256: " + sha256);

            Manifest manifest = new ObjectMapper().readValue(new File(path + "/blobs/sha256" + "/" + sha256), Manifest.class);
            if (manifest != null) {
                // Searching about the layer containing the json file of the Task
                logger.info("Manifest found within blobs folder");
                return manifest.getLayers().stream()
                    .filter(layer -> "task".equals(layer.getAnnotations().get("dev.tekton.image.kind")))
                    .collect(Collectors.toList());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (StreamReadException e) {
            throw new RuntimeException(e);
        } catch (DatabindException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static List<String> extractTasksFromBlob(File blobFile) {
        List<String> extractedFiles = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(blobFile);
             GzipCompressorInputStream gis = new GzipCompressorInputStream(fis);
             TarArchiveInputStream tis = new TarArchiveInputStream(gis)) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
                File outputFile = new File(blobFile.getParentFile(), entry.getName() + ".json");
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = tis.read(buffer)) != -1) {
                            fos.write(buffer, 0, length);
                        }
                    }
                    logger.info("Path of the file extracted: " + outputFile.getAbsolutePath() + "\n");
                    extractedFiles.add(outputFile.getPath());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return extractedFiles;
    }

    public static String asYaml(String jsonString) throws IOException {
        // parse JSON
        JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
        // save it as YAML
        String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
        return jsonAsYaml;
    }

}
