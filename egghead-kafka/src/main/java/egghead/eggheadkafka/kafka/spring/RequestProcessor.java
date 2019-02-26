package egghead.eggheadkafka.kafka.spring;

import egghead.eggheadkafka.kafka.processing.PaymentMethodProcessor;
import egghead.eggheadkafka.kafka.model.Request;
import egghead.eggheadkafka.kafka.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author mikael
 */
@Service
public class RequestProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestProcessor.class);

    private final ResponseProducer responseProducer;

    public RequestProcessor(ResponseProducer responseProducer) {
        this.responseProducer = responseProducer;
    }

    public void process(Request request) {
        LOGGER.info("Processing {}", request);

        Response response = PaymentMethodProcessor.Factory
            .createProcessor(request.getCountry(), request.getPaymentMethod())
            .process(request);

        LOGGER.info("Migrated to {}", response);

        responseProducer.sendMessage(response);
    }
}
