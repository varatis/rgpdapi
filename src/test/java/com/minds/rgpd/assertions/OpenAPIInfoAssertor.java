package com.minds.rgpd.assertions;

import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

public class OpenAPIInfoAssertor extends AbstractObjectAssert<OpenAPIInfoAssertor, JsonNode> {

    public OpenAPIInfoAssertor(JsonNode jsonNode) {
        super(jsonNode, OpenAPIInfoAssertor.class);
    }

    public static OpenAPIInfoAssertor assertThat(JsonNode jsonNode) {
        return new OpenAPIInfoAssertor(jsonNode);
    }

    public OpenAPIInfoAssertor hasTitle(String title) {
        isNotNull();
        Assertions.assertThat(actual.get("title")).isNotNull();
        Assertions.assertThat(actual.get("title").asText()).isEqualTo(title);
        return myself;
    }

    public OpenAPIInfoAssertor hasVersion(String version) {
        isNotNull();
        Assertions.assertThat(actual.get("version")).isNotNull();
        Assertions.assertThat(actual.get("version").asText()).isEqualTo(version);
        return myself;
    }

    public OpenAPIInfoAssertor hasDescription(String description) {
        isNotNull();
        Assertions.assertThat(actual.get("description")).isNotNull();
        Assertions.assertThat(actual.get("description").asText()).isEqualTo(description);
        return myself;
    }
}
