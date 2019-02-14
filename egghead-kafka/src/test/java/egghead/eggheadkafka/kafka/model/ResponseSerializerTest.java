package egghead.eggheadkafka.kafka.model;

import com.google.common.collect.ImmutableMap;
import egghead.eggheadkafka.util.Json;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

/**
 * @author mikael
 */
public class ResponseSerializerTest {

    @Test
    public void testSerializer() {

        Response response = new Response();
        response.setVersion(0);
        response.setId(UUID.randomUUID());
        response.setMetaInfo(ImmutableMap.of("aKey", "aValue"));

        String asString = Json.serialize(response);
        byte[] expected = asString.getBytes(StandardCharsets.UTF_8);

        Response.ResponseSerializer responseSerializer = new Response.ResponseSerializer();
        responseSerializer.configure(Collections.emptyMap(), false);
        byte[] actual = responseSerializer.serialize(null, response);

        Assert.assertArrayEquals(expected, actual);
    }
}
