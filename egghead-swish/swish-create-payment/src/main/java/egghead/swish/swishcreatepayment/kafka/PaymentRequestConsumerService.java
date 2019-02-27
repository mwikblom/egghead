package egghead.swish.swishcreatepayment.kafka;

import egghead.swish.swishcreatepayment.kafka.model.SwishDepositRequest;
import egghead.swish.swishcreatepayment.kafka.model.UiCreatePaymentResponse;
import egghead.swish.swishcreatepayment.kafka.model.WorkflowDepositFinalizedResponse;
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

    private final KafkaSender<Integer, UiCreatePaymentResponse> kafkaSenderForUiCreatePaymentResponse;
    private final KafkaSender<Integer, WorkflowDepositFinalizedResponse> kafkaSenderForWorkflowDepositFinalizedResponse;
    private final String swishDepositRequestTopic;
    private final String uiCreatePaymentResponseTopic;
    private final String workflowDepositFinalizedResponseTopic;
    private final ReceiverOptions<Integer, SwishDepositRequest> receiverOptionsForSwishDepositRequest;

    private Disposable kafkaConsumerForSwishDepositRequest;
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
    public PaymentRequestConsumerService(KafkaSender<Integer, UiCreatePaymentResponse> kafkaSenderForUiCreatePaymentResponse,
                                         KafkaSender<Integer, WorkflowDepositFinalizedResponse> kafkaSenderForWorkflowDepositFinalizedResponse,
                                         @Value("${trustly.swish.SwishDepositRequest.topic}") String swishDepositRequestTopic,
                                         @Value("${trustly.swish.UiCreatePaymentResponse.topic}") String uiCreatePaymentResponseTopic,
                                         @Value("${trustly.swish.WorkflowDepositFinalizedResponse.topic}") String workflowDepositFinalizedResponseTopic,
                                         ReceiverOptions<Integer, SwishDepositRequest> receiverOptionsForSwishDepositRequest) {
        this.kafkaSenderForUiCreatePaymentResponse = kafkaSenderForUiCreatePaymentResponse;
        this.kafkaSenderForWorkflowDepositFinalizedResponse = kafkaSenderForWorkflowDepositFinalizedResponse;
        this.swishDepositRequestTopic = swishDepositRequestTopic;
        this.uiCreatePaymentResponseTopic = uiCreatePaymentResponseTopic;
        this.workflowDepositFinalizedResponseTopic = workflowDepositFinalizedResponseTopic;
        this.receiverOptionsForSwishDepositRequest = receiverOptionsForSwishDepositRequest;
    }

    @PostConstruct
    private void initKafkaConsumer() {
        LOGGER.info("Starting Kafka Consumer For Deposit Requests");
        kafkaConsumerForSwishDepositRequest = createKafkaConsumer();
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
        ReceiverOptions<Integer, SwishDepositRequest> options = receiverOptionsForSwishDepositRequest
            .subscription(Collections.singleton(swishDepositRequestTopic));

        Scheduler scheduler = Schedulers.elastic();

        ConnectableFlux<Tuple2<String, ReceiverOffset>> flux = KafkaReceiver.create(options)
            .receive()
            .flatMap(record -> {
                ReceiverOffset offset = record.receiverOffset();
                SwishDepositRequest swishDepositRequest = record.value();

                LOGGER.info("Received Swish deposit request: {}", swishDepositRequest);

                // HTTP-blocking callCreateDeposit-ish.
                return doPaymentChain(swishDepositRequest.getOrderId(), offset);
            }).publish();

        // Starts poller
        Flux.from(flux).flatMap(y -> {
            String pollId = y.getT1();
            return poll(pollId, scheduler);
        }).subscribe();

        // Produces record to producer topic..
        flux.map(value -> {
            UiCreatePaymentResponse uiCreatePaymentResponse = new UiCreatePaymentResponse();
            uiCreatePaymentResponse.setOrderId(value.getT1());

            return SenderRecord.create(new ProducerRecord<Integer, UiCreatePaymentResponse>(uiCreatePaymentResponseTopic, uiCreatePaymentResponse), value.getT2());
        })
            .as(kafkaSenderForUiCreatePaymentResponse::send)
            .subscribe();

        return flux.connect();
    }

    @PreDestroy
    private void disposeKafkaConsumer() {
        if (kafkaConsumerForSwishDepositRequest != null) {
            kafkaConsumerForSwishDepositRequest.dispose();
        }
    }

    @PreDestroy
    public void closeKafkaSender() {
        kafkaSenderForUiCreatePaymentResponse.close();
    }
}
