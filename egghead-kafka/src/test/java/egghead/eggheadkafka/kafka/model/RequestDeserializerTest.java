package egghead.eggheadkafka.kafka.model;

import egghead.eggheadkafka.util.Json;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Currency;
import java.util.UUID;

/**
 * @author mikael
 */
public class RequestDeserializerTest {

    @Test
    public void testDeserializer() {

        Request expected = new Request();
        expected.setVersion(0);
        expected.setId(UUID.randomUUID());
        expected.setCountry(Request.Country.SWEDEN);
        expected.setPaymentMethod(Request.PaymentMethod.SWISH);
        expected.setCurrency(Currency.getInstance("SEK"));
        expected.setAmount(new BigDecimal(10));
        expected.setMessageOnPayerStatement("aMessageOnPayerStatement");
        expected.setMessageOnReceiverStatement("aMessageOnReceiverStatement");

        String asString = Json.serialize(expected);
        byte[] asBytes = asString.getBytes(StandardCharsets.UTF_8);

        Request.RequestDeserializer requestDeserializer = new Request.RequestDeserializer();
        requestDeserializer.configure(Collections.emptyMap(), false);
        Request actual = requestDeserializer.deserialize(null, asBytes);

        Assert.assertEquals(expected, actual);
    }
}
