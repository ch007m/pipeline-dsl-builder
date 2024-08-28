package dev.snowdrop.model.jackson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import dev.snowdrop.model.Bundle;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BundleDeserializer extends StdDeserializer<Bundle> {

    public BundleDeserializer() {
        this(null);
    }

    public BundleDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Bundle deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        String uri = node.asText();

        String resolver = uri.substring(0,uri.lastIndexOf("//") -1);
        String registry = uri.substring(uri.indexOf("//") + 2, uri.lastIndexOf("/"));

        String nameVersion = uri.substring(uri.lastIndexOf("/") + 1, uri.indexOf("@"));
        String[] splitResult = nameVersion.split(":");
        String name = splitResult[0];
        String version = splitResult[1];

        String sha256 = uri.substring(uri.indexOf("sha256:") + 7);

        Bundle bundle = new Bundle();
        bundle.setUri(uri);
        bundle.setResolver(resolver);
        bundle.setRegistry(registry);
        bundle.setName(name);
        bundle.setVersion(version);
        bundle.setSha256(sha256);

        /*
        String regex = "^(?<resolver>[^:]+)://(?<registry>[^/]+(?:/[^/]+)*)(?<name>[^:/]+):(?<version>[^@]+)@sha256:(?<sha256>[a-fA-F0-9]+)$";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(uri);

        if (matcher.find()) {
          Bundle bundle = new Bundle();
          bundle.setResolver(matcher.group("resolver"));
          bundle.setRegistry(matcher.group("registry"));
          bundle.setName(matcher.group("name"));
          bundle.setVersion(matcher.group("version"));
          bundle.setSha256(matcher.group("sha256"));
        }
         */
        return bundle;

    }
}
