package dev.snowdrop;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.snowdrop.model.oci.Index;
import dev.snowdrop.model.oci.Manifest;
import dev.snowdrop.model.oci.ManifestEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@CommandLine.Command(name = "extractBundle", description = "Fetch and extract the YAML resources from OCI bundle")
public class OCIBundleCommand implements Runnable {

    private static final String OUTPUT_OCI_PATH = "temp/oci";
    private static final String OUTPUT_TEKTON_PATH = "temp/oci/tekton";
    private static final String INDEX_OCI_FILE_PATH = OUTPUT_OCI_PATH + "/index.json";
    private static final String BLOBS_DIRECTORY = OUTPUT_OCI_PATH + "/blobs/sha256/";

    @CommandLine.Option(names = {"-b", "--bundle"}, description = "The OCI bundle URL", required = true)
    String configuration;

    private static final Logger logger = LoggerFactory.getLogger(OCIBundleCommand.class);

    @Override
    public void run() {
        // Fetch the OCI bundle
        grabOCIBundle(configuration, OUTPUT_OCI_PATH);

        // Search about the layer packaging the Tekton task
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            File indexJsonfile = new File(INDEX_OCI_FILE_PATH);
            Index index = new ObjectMapper().readValue(indexJsonfile, Index.class);

            // Get the mediaType digest => json file containing the layers
            ManifestEntry manifestEntry = index.getManifests().get(0);
            String mediaType = manifestEntry.getMediaType();
            AtomicReference<String> digest = new AtomicReference<>(manifestEntry.getDigest());
            String sha256 = digest.get().substring(7, digest.get().length());

            logger.info("mediaType: " + mediaType);
            logger.info("digest: " + digest);
            logger.info("sha256: " + sha256);

            Manifest manifest = new ObjectMapper().readValue(new File(BLOBS_DIRECTORY + "/" + sha256), Manifest.class);
            if (manifest != null) {
                // Searching about the layer containing the json file of the Task
                logger.info("Manifest found within blobs folder");
                List<Manifest.Layer> taskLayers = manifest.getLayers().stream()
                    .filter(layer -> "task".equals(layer.getAnnotations().get("dev.tekton.image.kind")))
                    .collect(Collectors.toList());

                if (!taskLayers.isEmpty()) {
                    taskLayers.forEach(layer -> {
                        String layerDigest = layer.getDigest();
                        logger.info("Found task layer: " + layerDigest + " for task: " + layer.getAnnotations().get("dev.tekton.image.name"));
                        extractAndProcessBlob(new File(BLOBS_DIRECTORY + "/" + layerDigest.substring(7, layerDigest.length())));
                    });
                }
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
    }

    private static void grabOCIBundle(String oci, String outputPath) {
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

    private static void extractAndProcessBlob(File blobFile) {
        try (FileInputStream fis = new FileInputStream(blobFile);
             GzipCompressorInputStream gis = new GzipCompressorInputStream(fis);
             TarArchiveInputStream tis = new TarArchiveInputStream(gis)) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
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
                    logger.info("Path to file extracted: " + outputFile.getAbsolutePath() + "\n");

                    // Create the Tekton folder to copy the YAML files
                    Files.createDirectories(Paths.get(OUTPUT_TEKTON_PATH));

                    String yaml = asYaml(Files.readString(outputFile.toPath()));
                    String yamlFileName = OUTPUT_TEKTON_PATH + "/" + outputFile.getName().replaceFirst("[.][^.]+$", ".yaml");
                    logger.debug("yaml file name: " + yamlFileName);
                    Files.write(Paths.get(yamlFileName), yaml.getBytes());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String asYaml(String jsonString) throws IOException {
        // parse JSON
        JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
        // save it as YAML
        String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
        return jsonAsYaml;
    }
}
