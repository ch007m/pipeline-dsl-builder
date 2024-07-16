package dev.snowdrop;

import java.io.*;
import java.util.Optional;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.snowdrop.model.oci.Index;
import dev.snowdrop.model.oci.Manifest;
import dev.snowdrop.model.oci.ManifestEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class OCIBundleGrabber {

    private static final String OUTPUT_OCI_PATH = "temp/oci";
    private static final String INDEX_OCI_FILE_PATH = OUTPUT_OCI_PATH + "/index.json";
    private static final String BLOBS_DIRECTORY = OUTPUT_OCI_PATH + "/blobs/sha256/";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Please provide as argument the OCI url: ");
            System.err.println("OCIBundleGrabber quay.io/konflux-ci/tekton-catalog/task-git-clone:0.1");
            System.exit(1);
        }

        // Fetch the OCI bundle
        grabOCIBundle(args[0], OUTPUT_OCI_PATH);

        // Search about the layer packaging the Tekton task
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            File indexJsonfile = new File(INDEX_OCI_FILE_PATH);
            Index index = new ObjectMapper().readValue(indexJsonfile, Index.class);

            // Get the mediaType digest => json file containing the layers
            ManifestEntry manifestEntry = index.getManifests().get(0);
            String mediaType = manifestEntry.getMediaType();
            String digest = manifestEntry.getDigest();
            String sha256 = digest.substring(7, digest.length());

            System.out.println("mediaType: " + mediaType);
            System.out.println("digest: " + digest);
            System.out.println("sha256: " + sha256);

            Manifest manifest = new ObjectMapper().readValue(new File(BLOBS_DIRECTORY + "/" + sha256), Manifest.class);
            if (manifest != null) {
                // Searching about the layer containing the json file of the Task
                System.out.println("Manifest found within blobs folder");
                Optional<Manifest.Layer> taskLayer = manifest.getLayers().stream()
                    .filter(layer -> "task".equals(layer.getAnnotations().get("dev.tekton.image.kind")))
                    .findFirst();

                if (taskLayer.isPresent()) {
                    digest = taskLayer.get().getDigest();
                    System.out.println("Found task layer: " + digest);
                    // Process the found task layer as needed
                    extractAndProcessBlob(new File(BLOBS_DIRECTORY + "/" + digest.substring(7, digest.length())));
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
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);
        } catch (Exception e) {
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
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
