package egghead.swish.swishcreatepayment.kafka.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * TODO produces a Kafka topic ??? which is a trigger for the UI to show "open swish"
 *
 * @author mikael
 */
public class UiCreatePaymentResponse {

    private String orderId;
    private String autoStartToken;
    private String openSwishUrl;

    public String getAutoStartToken() {
        return autoStartToken;
    }

    public void setAutoStartToken(String autoStartToken) {
        this.autoStartToken = autoStartToken;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOpenSwishUrl() {
        return openSwishUrl;
    }

    public void setOpenSwishUrl(String openSwishUrl) {
        this.openSwishUrl = openSwishUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UiCreatePaymentResponse that = (UiCreatePaymentResponse) o;
        return Objects.equals(orderId, that.orderId) &&
            Objects.equals(autoStartToken, that.autoStartToken) &&
            Objects.equals(openSwishUrl, that.openSwishUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, autoStartToken, openSwishUrl);
    }

    @Override
    public String toString() {
        return "UiCreatePaymentResponse{" +
            "orderId='" + orderId + '\'' +
            ", autoStartToken='" + autoStartToken + '\'' +
            ", openSwishUrl='" + openSwishUrl + '\'' +
            '}';
    }

    public static class ResponseSerializer implements Serializer<UiCreatePaymentResponse> {

        private Charset charset;

        @Override
        public void configure(Map configs, boolean isKey) {
            charset = StandardCharsets.UTF_8;
        }

        @Override
        public byte[] serialize(String topic, UiCreatePaymentResponse data) {
            String asString;

            try {
                asString = new ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(data);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Unable to serialize to json " + data, e);
            }

            return asString.getBytes(charset);
        }

        @Override
        public void close() {
        }
    }
}
