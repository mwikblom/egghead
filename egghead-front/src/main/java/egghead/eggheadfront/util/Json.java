package egghead.eggheadfront.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * @author mikael
 */
public class Json {

    public static String serialize(Object data) {
        try {
            return new ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to serialize to json", e);
        }
    }

    public static <T> T deserialize(String data, Class<T> tClass) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(data, tClass);
        } catch (IOException e) {
            throw new RuntimeException("Unable to deserialize", e);
        }
    }
}
