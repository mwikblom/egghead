package egghead.swish.swishcreatepayment.kafka.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * TODO produces a Kafka topic ??? which is a trigger for the workflow that the deposit order has been finalized
 *
 * @author mikael
 */
public class WorkflowDepositFinalizedKafkaResponse {

    // TODO
    public enum DepositOrderState {
        CANCELED,
        SUCCESSFUL,
        FAILED
    }

    private String orderId;
    private DepositOrderState depositOrderState;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public DepositOrderState getDepositOrderState() {
        return depositOrderState;
    }

    public void setDepositOrderState(DepositOrderState depositOrderState) {
        this.depositOrderState = depositOrderState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkflowDepositFinalizedKafkaResponse that = (WorkflowDepositFinalizedKafkaResponse) o;
        return Objects.equals(orderId, that.orderId) &&
            depositOrderState == that.depositOrderState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, depositOrderState);
    }

    @Override
    public String toString() {
        return "WorkflowDepositFinalizedResponse{" +
            "orderId='" + orderId + '\'' +
            ", depositOrderState=" + depositOrderState +
            '}';
    }

    // TODO replaced by JsonSerializer??

    public static class ResponseSerializer implements Serializer<WorkflowDepositFinalizedKafkaResponse> {

        private Charset charset;

        @Override
        public void configure(Map configs, boolean isKey) {
            charset = StandardCharsets.UTF_8;
        }

        @Override
        public byte[] serialize(String topic, WorkflowDepositFinalizedKafkaResponse data) {
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
