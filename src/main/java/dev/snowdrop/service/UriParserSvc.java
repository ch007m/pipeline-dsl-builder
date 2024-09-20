package dev.snowdrop.service;

import dev.snowdrop.model.Bundle;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class UriParserSvc {

    public static Bundle extract(String uriToParse) {
        Bundle b = null;
        String regex = "(bundle|git|url)://(.+)";

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
            log.info("Protocol: " + protocol);
            log.info("Url: " + url);
            b = new Bundle()
                .protocol(protocol)
                .uri(url);
        } else {
            log.warn("The input string does not match the expected format.");
        }
        return b;
    }
}