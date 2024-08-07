package dev.snowdrop.service;

import dev.snowdrop.PipeBuilderApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class FileUtilSvc {

   private static final Logger logger = LoggerFactory.getLogger(PipeBuilderApp.class);
   private static final String SCRIPTS_PATH = "scripts/";

   public static String loadFileAsString(String fileName) {
      String scriptPath = SCRIPTS_PATH + fileName;
      logger.debug("#### Script path to embed in a task: " + scriptPath);

      // Get the input stream for the file from the class loader
      InputStream inputStream = FileUtilSvc.class.getClassLoader().getResourceAsStream( scriptPath );

      // Handle case when the file is not found
      if (inputStream == null) {
         throw new IllegalArgumentException("File not found: " + scriptPath);
      }

      // Convert the input stream into a multi-line string
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
         return reader.lines().collect(Collectors.joining("\n"));
      } catch (IOException e) {
         e.printStackTrace();
         throw new RuntimeException("Failed to read file: " + fileName, e);
      }
   }

   public static String fetchScriptFileContent(String fileUrl) throws IOException {
      StringBuilder content = new StringBuilder();
      URL url = new URL(fileUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      connection.setRequestMethod("GET");

      try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
         String inputLine;
         while ((inputLine = in.readLine()) != null) {
            content.append(inputLine).append("\n");
         }
      } finally {
         connection.disconnect();
      }

      return content.toString();
   }

}
