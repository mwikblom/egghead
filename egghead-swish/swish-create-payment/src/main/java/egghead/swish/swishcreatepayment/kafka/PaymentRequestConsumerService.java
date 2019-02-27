package egghead.swish.swishcreatepayment.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PaymentRequestConsumerService {
    private final static Logger LOGGER = LoggerFactory.getLogger(PaymentRequestConsumerService.class);
    private final KafkaSender<Integer, String> kafkaSender;
    private final ReceiverOptions<Integer, String> receiverOptions;
    private final String swishProducerTopic;
    private final String swishConsumerTopic;
    private Disposable kafkaConsumer;
    private AtomicInteger counter = new AtomicInteger();

    public static class Response {
        private long userId;
        private long id;
        private String title;
        private boolean completed;

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }

    @Autowired
    public PaymentRequestConsumerService(KafkaSender<Integer, String> kafkaSender,
                                         @Value("${kafka.swish.response.topic}") String swishProducerTopic,
                                         @Value("${kafka.swish.request.topic}") String swishConsumerTopic,
                                         ReceiverOptions<Integer, String> receiverOptions) {
        this.kafkaSender = kafkaSender;
        this.swishProducerTopic = swishProducerTopic;
        this.swishConsumerTopic = swishConsumerTopic;
        this.receiverOptions = receiverOptions;
    }

    @PostConstruct
    private void initKafkaConsumer() {
        LOGGER.info("Starting Kafka Consumer For Deposit Requests");
        kafkaConsumer = createKafkaConsumer();
        LOGGER.info("Created.");
    }

    private Mono<String> callDepositService(Scheduler scheduler) {
        String path = "todos/1";
        WebClient webClient = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .build();

        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(Response.class)
            .subscribeOn(scheduler)
            .map(response -> {
                Integer currentCount = counter.getAndIncrement();
                LOGGER.info("getOrderInfo - THREAD-ID: {}, OrderId {}:", Thread.currentThread().getId(), currentCount);
                return String.valueOf(currentCount);
            });
    }

    private Mono<Tuple2<String, ReceiverOffset>> callCreateDeposit(String orderId, Scheduler scheduler, String message, ReceiverOffset offset) {
        String path = "todos/1";
        WebClient webClient = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .build();

        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(Response.class)
            .subscribeOn(scheduler)
            .map(response -> {
                LOGGER.info("CreateDeposit - THREAD.ID {}, ORDER-ID {}, MESSAGE: {}", Thread.currentThread().getId(), orderId, message);

                // TODO .. should be response Location.
                return Tuples.of(orderId, offset);
            });
    }

    private Mono<Tuple2<String, ReceiverOffset>> doPaymentChain(String message, ReceiverOffset offset) {
        Scheduler scheduler = Schedulers.elastic();
        Mono<String> metaData = callDepositService(scheduler);
        return metaData
            .flatMap(foo -> callCreateDeposit(foo, scheduler, message, offset));
    }

    private Flux<String> poll(String pollId, Scheduler scheduler) {
        String path = "todos/1";
        WebClient webClient = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .build();

        // Wait 5 seconds before starting. Then we poll every 2 seconds
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(5), Duration.ofSeconds(2));

        Mono<String> pollReq =
            webClient.get()
                .uri(path)
                .retrieve()
                .bodyToMono(Response.class)
                .subscribeOn(scheduler)
                .map(response -> {
                    LOGGER.info("Poller - THREAD-ID: {}  Response ORDER-ID: {}", Thread.currentThread().getId(), pollId);
                    return String.valueOf(pollId);
                });

        return Flux.from(interval)
            .flatMap(y -> pollReq)
            // Here we should check polling-status. Now we just say if Response == stop, then we stop.
            .takeWhile(y -> !y.equals("stop"))
            // Poll for 15 seconds.
            .take(Duration.ofSeconds(15))
            .doFinally(x -> LOGGER.info("Done polling order: {}", pollId));
    }

    private Disposable createKafkaConsumer() {
        ReceiverOptions<Integer, String> options = receiverOptions
            .subscription(Collections.singleton(swishConsumerTopic));

        Scheduler scheduler = Schedulers.elastic();

        ConnectableFlux<Tuple2<String, ReceiverOffset>> flux = KafkaReceiver.create(options)
            .receive()
            .flatMap(record -> {
                ReceiverOffset offset = record.receiverOffset();
                // HTTP-blocking callCreateDeposit-ish.
                return doPaymentChain(record.value(), offset);
            }).publish();

        // Starts poller
        Flux.from(flux)
            .flatMap(y -> {
                String pollId = y.getT1();
                return poll(pollId, scheduler);
            })
            .subscribe();

        // Produces record to producer topic..
        flux.map(value -> SenderRecord.create(new ProducerRecord<Integer, String>(swishProducerTopic, "Message: " + value.getT1()), value.getT2()))
            .as(kafkaSender::send)
            .subscribe();

        return flux.connect();
    }

    @PreDestroy
    private void disposeKafkaConsumer() {
        if (kafkaConsumer != null) {
            kafkaConsumer.dispose();
        }
    }

    @PreDestroy
    public void closeKafkaSender() {
        kafkaSender.close();
    }
}
