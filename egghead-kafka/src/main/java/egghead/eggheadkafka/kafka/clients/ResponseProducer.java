package egghead.eggheadkafka.kafka.clients;

import egghead.eggheadkafka.kafka.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * produces the matching Response
 *
 * @author mikael
 */
@Service
public class ResponseProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseProducer.class);

    private final KafkaTemplate<String, Response> kafkaTemplate;

    public ResponseProducer(KafkaTemplate<String, Response> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(Response response) {

        LOGGER.info("Sending {}", response);

        kafkaTemplate.send(KafkaClientsConfiguration.RESPONSE_TOPIC, response);
    }
}
