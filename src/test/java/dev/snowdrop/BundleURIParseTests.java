package dev.snowdrop;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

public class BundleURIParseTests {

    @Test
    public void parseURItoBundleObject() {
        String uri = "bundle://quay.io/konflux-ci/tekton-catalog/task-init:1.0@sha256:092c113b614f6551113f17605ae9cb7e822aa704d07f0e37ed209da23ce392cc";

        var regex = "^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?";
        var pattern = Pattern.compile(regex);
        var matcher = pattern.matcher(uri);
        if (matcher.matches()) {
            System.out.println("resolver: " + matcher.group(2));

            System.out.println("authority: " + matcher.group(4));
            System.out.println("path: " + matcher.group(5));
            System.out.println("query: " + matcher.group(7));
            System.out.println("fragment: " + matcher.group(9));
        }
    }

    @Test
    public void parseHTTPUrl() {
        String uri = "http://example.com:80/docs/books/tutorial/index.html?name=networking#DOWNLOADING";

        var regex = "^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?";
        var pattern = Pattern.compile(regex);
        var matcher = pattern.matcher(uri);
        if (matcher.matches()) {
            System.out.println("scheme: " + matcher.group(2));
            System.out.println("authority: " + matcher.group(4));
            System.out.println("path: " + matcher.group(5));
            System.out.println("query: " + matcher.group(7));
            System.out.println("fragment: " + matcher.group(9));
        }
    }

}
