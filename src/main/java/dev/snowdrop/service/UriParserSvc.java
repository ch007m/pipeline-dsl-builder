package dev.snowdrop.service;

import dev.snowdrop.PipeBuilderApp;
import dev.snowdrop.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UriParserSvc {
    private static final Logger logger = LoggerFactory.getLogger(PipeBuilderApp.class);

    public static Bundle extract(String uriToParse) {
        Bundle b = null;
        String regex = "(bundles|git)://([^:]+):(.+)";

        // Compile the regex pattern
        Pattern pattern = Pattern.compile(regex);

        // Create a matcher for the input string
        Matcher matcher = pattern.matcher(uriToParse);

        // Check if the pattern matches the input string
        if (matcher.matches()) {
            // Extract the URI, task name, and URL
            String uri = matcher.group(1);
            String taskName = matcher.group(2);
            String url = matcher.group(3);

            // Print the extracted values
            logger.info("URI: " + uri);
            logger.info("Task Name: " + taskName);
            logger.info("URL: " + url);
            b = new Bundle(url, taskName);
        } else {
            logger.warn("The input string does not match the expected format.");
        }
        return b;
    }
}