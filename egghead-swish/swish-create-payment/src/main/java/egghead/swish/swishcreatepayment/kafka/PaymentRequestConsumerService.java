package egghead.swish.swishcreatepayment.kafka;

import egghead.swish.swishcreatepayment.depositservice.DepositServiceApi;
import egghead.swish.swishcreatepayment.depositservice.model.DepositOrderResponse;
import egghead.swish.swishcreatepayment.kafka.model.SwishDepositKafkaRequest;
import egghead.swish.swishcreatepayment.kafka.model.UiCreatePaymentKafkaResponse;
import egghead.swish.swishcreatepayment.kafka.model.WorkflowDepositFinalizedKafkaResponse;
import egghead.swish.swishcreatepayment.swish.SwishApi;
import egghead.swish.swishcreatepayment.swish.model.CreatePaymentRequestResponse;
import egghead.swish.swishcreatepayment.swish.model.RetrievePaymentResponse;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collections;

@Service
public class PaymentRequestConsumerService {
    private final static Logger LOGGER = LoggerFactory.getLogger(PaymentRequestConsumerService.class);

    private final KafkaSender<Integer, UiCreatePaymentKafkaResponse> kafkaSenderForUiCreatePaymentResponse;
    private final KafkaSender<Integer, WorkflowDepositFinalizedKafkaResponse> kafkaSenderForWorkflowDepositFinalizedResponse;
    private final String swishDepositRequestTopic;
    private final String uiCreatePaymentResponseTopic;
    private final String workflowDepositFinalizedResponseTopic;
    private final ReceiverOptions<Integer, SwishDepositKafkaRequest> receiverOptionsForSwishDepositRequest;
    private final SwishApi swishApi;
    private final DepositServiceApi depositServiceApi;

    private Disposable kafkaConsumerForSwishDepositRequest;

    @Autowired
    public PaymentRequestConsumerService(KafkaSender<Integer, UiCreatePaymentKafkaResponse> kafkaSenderForUiCreatePaymentResponse,
                                         KafkaSender<Integer, WorkflowDepositFinalizedKafkaResponse> kafkaSenderForWorkflowDepositFinalizedResponse,
                                         @Value("${trustly.swish.SwishDepositRequest.topic}") String swishDepositRequestTopic,
                                         @Value("${trustly.swish.UiCreatePaymentResponse.topic}") String uiCreatePaymentResponseTopic,
                                         @Value("${trustly.swish.WorkflowDepositFinalizedResponse.topic}") String workflowDepositFinalizedResponseTopic,
                                         ReceiverOptions<Integer, SwishDepositKafkaRequest> receiverOptionsForSwishDepositRequest,
                                         SwishApi swishApi,
                                         DepositServiceApi depositServiceApi) {
        this.kafkaSenderForUiCreatePaymentResponse = kafkaSenderForUiCreatePaymentResponse;
        this.kafkaSenderForWorkflowDepositFinalizedResponse = kafkaSenderForWorkflowDepositFinalizedResponse;
        this.swishDepositRequestTopic = swishDepositRequestTopic;
        this.uiCreatePaymentResponseTopic = uiCreatePaymentResponseTopic;
        this.workflowDepositFinalizedResponseTopic = workflowDepositFinalizedResponseTopic;
        this.receiverOptionsForSwishDepositRequest = receiverOptionsForSwishDepositRequest;
        this.swishApi = swishApi;
        this.depositServiceApi = depositServiceApi;
    }

    @PostConstruct
    private void initKafkaConsumer() {
        LOGGER.info("Starting Kafka Consumer For Deposit Requests");
        kafkaConsumerForSwishDepositRequest = createKafkaConsumer();
        LOGGER.info("Created.");
    }

    private Mono<Tuple4<SwishDepositKafkaRequest, ReceiverOffset, DepositOrderResponse, CreatePaymentRequestResponse>> doSwishDepositChain(SwishDepositKafkaRequest swishDepositKafkaRequest, ReceiverOffset receiverOffset) {

        // TODO correct scheduler
        Scheduler scheduler = Schedulers.elastic();

        Mono<DepositOrderResponse> callDepositService = depositServiceApi.callDepositOrder(scheduler, swishDepositKafkaRequest);
        return callDepositService
            .zipWhen(depositOrder -> swishApi.callCreatePaymentRequest(scheduler, depositOrder), (depositOrder, createPaymentRequestResponse) ->
                Tuples.of(swishDepositKafkaRequest, receiverOffset, depositOrder, createPaymentRequestResponse));
    }

    private Flux<RetrievePaymentResponse> pollSwishPaymentStatus(Scheduler scheduler, SwishDepositKafkaRequest swishDepositKafkaRequest, DepositOrderResponse depositOrder, CreatePaymentRequestResponse swishPaymentRequest) {

        // Wait 5 seconds before starting. Then we poll every 2 seconds
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(5), Duration.ofSeconds(2));

        return Flux.from(interval)
            .flatMap(count -> swishApi.callRetrievePayment(scheduler, swishPaymentRequest.getLocation()))
            // Here we should check polling-status. Now we just say if Response == stop, then we stop.
            .takeWhile(swishPaymentStatus -> swishPaymentStatus.getStatus().equals("PENDING"))
            // Poll for 15 seconds.
            .take(Duration.ofSeconds(15))
            .doFinally(signalType -> LOGGER.info("Done polling order: {}", depositOrder.getOrderId()))
            .doOnSubscribe(subscription -> LOGGER.info("Subscribing"));
    }

    private Disposable createKafkaConsumer() {
        ReceiverOptions<Integer, SwishDepositKafkaRequest> options = receiverOptionsForSwishDepositRequest
            .subscription(Collections.singleton(swishDepositRequestTopic));

        // TODO correct scheduler
        Scheduler scheduler = Schedulers.elastic();

        ConnectableFlux<Tuple4<SwishDepositKafkaRequest, ReceiverOffset, DepositOrderResponse, CreatePaymentRequestResponse>> flux = KafkaReceiver.create(options)
            .receive()
            .flatMap(record -> {
                ReceiverOffset receiverOffset = record.receiverOffset();
                SwishDepositKafkaRequest swishDepositKafkaRequest = record.value();

                LOGGER.info("Received Swish deposit request: {}", swishDepositKafkaRequest);

                // HTTP-blocking callCreateDeposit-ish.
                return doSwishDepositChain(swishDepositKafkaRequest, receiverOffset);
            }).publish();

        // Starts poller
        Flux.from(flux)
            .flatMap(swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest -> {
                SwishDepositKafkaRequest swishDepositKafkaRequest = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT1();
                DepositOrderResponse depositOrder = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT3();
                CreatePaymentRequestResponse swishPaymentRequest = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT4();

                return pollSwishPaymentStatus(scheduler, swishDepositKafkaRequest, depositOrder, swishPaymentRequest);
            })
            .subscribe();

        // Produces record to producer topic..
        flux
            .map(swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest -> {
                CreatePaymentRequestResponse swishPaymentRequest = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT4();
                DepositOrderResponse depositOrder = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT3();
                ReceiverOffset receiverOffset = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT2();

                UiCreatePaymentKafkaResponse uiCreatePaymentResponse = new UiCreatePaymentKafkaResponse();
                uiCreatePaymentResponse.setOrderId(depositOrder.getOrderId());
                if (swishPaymentRequest.getPaymentRequestToken() != null) {
                    uiCreatePaymentResponse.setAutoStartToken(swishPaymentRequest.getPaymentRequestToken());
                    uiCreatePaymentResponse.setOpenSwishUrl("swish://" + swishPaymentRequest.getPaymentRequestToken()); // TODO correct
                }

                return SenderRecord.create(new ProducerRecord<Integer, UiCreatePaymentKafkaResponse>(uiCreatePaymentResponseTopic, uiCreatePaymentResponse), receiverOffset);
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
