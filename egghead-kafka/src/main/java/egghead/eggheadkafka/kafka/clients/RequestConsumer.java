package egghead.eggheadkafka.kafka.clients;

import egghead.eggheadkafka.kafka.model.Request;
import egghead.eggheadkafka.kafka.processing.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * listens to any API requests
 *
 * @author mikael
 */
@Service
public class RequestConsumer {

    private final static Logger LOGGER = LoggerFactory.getLogger(RequestConsumer.class);

    private final RequestProcessor requestProcessor;

    public RequestConsumer(RequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;
    }

    @KafkaListener(topics = KafkaClientsConfiguration.REQUEST_TOPIC, groupId = KafkaClientsConfiguration.GROUP_ID)
    public void listenToRequests(Request request) {

        LOGGER.info("Received {}", request);

        requestProcessor.process(request);
    }
}
