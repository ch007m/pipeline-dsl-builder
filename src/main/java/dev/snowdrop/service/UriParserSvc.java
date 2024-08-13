package dev.snowdrop.service;

import dev.snowdrop.BuilderCommand;
import dev.snowdrop.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UriParserSvc {
    private static final Logger logger = LoggerFactory.getLogger(BuilderCommand.class);

    public static Bundle extract(String uriToParse) {
        Bundle b = null;
        String regex = "(bundle|git)://(.+)";

        // Compile the regex pattern
        Pattern pattern = Pattern.compile(regex);

        // Create a matcher for the input string
        Matcher matcher = pattern.matcher(uriToParse);

        // Check if the pattern matches the input string
        if (matcher.matches()) {
            // Extract the URI, task name, and URL
            String protocol = matcher.group(1);
            String url = matcher.group(2);

            // Print the extracted values
            logger.info("Protocol: " + protocol);
            logger.info("Url: " + url);
            b = new Bundle(url);
        } else {
            logger.warn("The input string does not match the expected format.");
        }
        return b;
    }
}