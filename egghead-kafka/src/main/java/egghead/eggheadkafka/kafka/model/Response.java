package egghead.eggheadkafka.kafka.model;

import egghead.eggheadkafka.util.Json;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * response after call to an external service
 *
 * @author mikael
 */
public class Response {

    private int version;
    private UUID id;
    private Map<String, Object> metaInfo;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Map<String, Object> getMetaInfo() {
        return metaInfo;
    }

    public void setMetaInfo(Map<String, Object> metaInfo) {
        this.metaInfo = metaInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Response response = (Response) o;
        return version == response.version &&
            Objects.equals(id, response.id) &&
            Objects.equals(metaInfo, response.metaInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, id, metaInfo);
    }

    @Override
    public String toString() {
        return "Response{" +
            "version=" + version +
            ", id=" + id +
            ", metaInfo=" + metaInfo +
            '}';
    }

    public static class ResponseSerializer implements Serializer<Response> {

        private Charset charset;

        @Override
        public void configure(Map configs, boolean isKey) {
            charset = StandardCharsets.UTF_8;
        }

        @Override
        public byte[] serialize(String topic, Response data) {
            String asString = Json.serialize(data);
            return asString.getBytes(charset);
        }

        @Override
        public void close() {
        }
    }
}
