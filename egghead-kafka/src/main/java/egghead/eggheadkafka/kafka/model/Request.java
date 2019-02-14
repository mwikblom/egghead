package egghead.eggheadkafka.kafka.model;

import egghead.eggheadkafka.util.Json;
import org.apache.kafka.common.serialization.Deserializer;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Currency;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * request to call a service
 *
 * @author mikael
 */
public class Request {

    public enum Country {
        SWEDEN,
        FINLAND
    }

    public enum PaymentMethod {
        SWISH,
        NORDEA
    }

    private int version;
    private UUID id;
    private Country country;
    private PaymentMethod paymentMethod;
    private Currency currency;
    private BigDecimal amount;
    private String messageOnPayerStatement;
    private String messageOnReceiverStatement;

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

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMessageOnPayerStatement() {
        return messageOnPayerStatement;
    }

    public void setMessageOnPayerStatement(String messageOnPayerStatement) {
        this.messageOnPayerStatement = messageOnPayerStatement;
    }

    public String getMessageOnReceiverStatement() {
        return messageOnReceiverStatement;
    }

    public void setMessageOnReceiverStatement(String messageOnReceiverStatement) {
        this.messageOnReceiverStatement = messageOnReceiverStatement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Request request = (Request) o;
        return version == request.version &&
            Objects.equals(id, request.id) &&
            country == request.country &&
            paymentMethod == request.paymentMethod &&
            Objects.equals(currency, request.currency) &&
            Objects.equals(amount, request.amount) &&
            Objects.equals(messageOnPayerStatement, request.messageOnPayerStatement) &&
            Objects.equals(messageOnReceiverStatement, request.messageOnReceiverStatement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, id, country, paymentMethod, currency, amount, messageOnPayerStatement, messageOnReceiverStatement);
    }

    @Override
    public String toString() {
        return "Request{" +
            "version=" + version +
            ", id=" + id +
            ", country=" + country +
            ", paymentMethod=" + paymentMethod +
            ", currency=" + currency +
            ", amount=" + amount +
            ", messageOnPayerStatement='" + messageOnPayerStatement + '\'' +
            ", messageOnReceiverStatement='" + messageOnReceiverStatement + '\'' +
            '}';
    }

    public static class RequestDeserializer implements Deserializer<Request> {

        private Charset charset;

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            charset = StandardCharsets.UTF_8;
        }

        @Override
        public Request deserialize(String topic, byte[] data) {
            String asString = new String(data, charset);
            return Json.deserialize(asString, Request.class);
        }

        @Override
        public void close() {
        }
    }
}
