package egghead.swish.swishcreatepayment.kafka.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * TODO listens to the kafka topic ??? which is the trigger to do a swish deposit
 *
 * @author mikael
 */
public class SwishDepositRequest {

    private String orderId;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SwishDepositRequest that = (SwishDepositRequest) o;
        return Objects.equals(orderId, that.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return "SwishDepositRequest{" +
            "orderId='" + orderId + '\'' +
            '}';
    }

    public static class RequestDeserializer implements Deserializer<SwishDepositRequest> {

        private Charset charset;

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            charset = StandardCharsets.UTF_8;
        }

        @Override
        public SwishDepositRequest deserialize(String topic, byte[] data) {
            String asString = new String(data, charset);

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.readValue(data, SwishDepositRequest.class);
            } catch (IOException e) {
                throw new RuntimeException("Unable to deserialize " + asString, e);
            }
        }

        @Override
        public void close() {
        }
    }
}
