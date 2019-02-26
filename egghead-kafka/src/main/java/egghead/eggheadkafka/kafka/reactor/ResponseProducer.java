package egghead.eggheadkafka.kafka.reactor;

import egghead.eggheadkafka.kafka.model.Response;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import javax.annotation.PreDestroy;

/**
 * @author mikael
 */
@Service
public class ResponseProducer {

    private final KafkaSender<String, Response> kafkaSender;

    public ResponseProducer(KafkaSender<String, Response> kafkaSender) {
        this.kafkaSender = kafkaSender;
    }

    public void sendMessage(Response response) {

        /*Flux<SenderRecord<String, Response, Object>> flux = Flux.range(1, 1)
            .map(i -> SenderRecord.create(KafkaReactorConfiguration.RESPONSE_TOPIC, null, null, null, response, null));
*/

        Mono<SenderRecord<String, Response, Object>> mono = Mono.just(
            SenderRecord.create(KafkaReactorConfiguration.RESPONSE_TOPIC, null, null, null, response, null)
        );

        kafkaSender.send(mono)
            .subscribe(senderResult -> {
                RecordMetadata metadata = senderResult.recordMetadata();

            });
    }

    @PreDestroy
    public void closeSender() {
        kafkaSender.close();
    }
}
