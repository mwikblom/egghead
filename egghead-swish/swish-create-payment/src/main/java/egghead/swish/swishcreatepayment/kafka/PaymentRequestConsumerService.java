package egghead.swish.swishcreatepayment.kafka;

import egghead.swish.swishcreatepayment.depositservice.DepositServiceApi;
import egghead.swish.swishcreatepayment.depositservice.model.DepositOrderResponse;
import egghead.swish.swishcreatepayment.kafka.model.SwishDepositKafkaRequest;
import egghead.swish.swishcreatepayment.kafka.model.UiCreatePaymentKafkaResponse;
import egghead.swish.swishcreatepayment.kafka.model.WorkflowDepositFinalizedKafkaResponse;
import egghead.swish.swishcreatepayment.swish.SwishApi;
import egghead.swish.swishcreatepayment.swish.model.CreatePaymentRequestResponse;
import egghead.swish.swishcreatepayment.swish.model.RetrievePaymentResponse;
import egghead.swish.swishcreatepayment.swish.model.SwishStatusCode;
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
import reactor.kafka.receiver.ReceiverRecord;
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

    private Flux<RetrievePaymentResponse> pollSwishPaymentStatus(String location) {

        // TODO correct scheduler
        Scheduler scheduler = Schedulers.elastic();

        // Wait 5 seconds before starting. Then we poll every 2 seconds
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(5), Duration.ofSeconds(2));

        return Flux.from(interval)
            .flatMap(count -> swishApi.callRetrievePayment(scheduler, location))
            .filter(retrievePaymentResponse -> {
                return retrievePaymentResponse.getStatus() != SwishStatusCode.CREATED;
            })
            // Poll for 15 seconds.
            .take(Duration.ofSeconds(15))
            .take(1)
            .doOnSubscribe(subscription -> LOGGER.info("Subscribing"))
            .doOnComplete(() -> LOGGER.info("Done with poll"));
    }

    private <K, V> Flux<ReceiverRecord<K, V>> setupReceiverListener(ReceiverOptions<K, V> receiverOptions) {
        return KafkaReceiver.create(receiverOptions)
            .receive();
    }

    private Flux<ReceiverRecord<Integer, SwishDepositKafkaRequest>> setupSwishDepositKafkaRequest() {
        return setupReceiverListener(receiverOptionsForSwishDepositRequest.subscription(Collections.singleton(swishDepositRequestTopic)));
    }

    private ConnectableFlux<Tuple4<SwishDepositKafkaRequest, ReceiverOffset, DepositOrderResponse, CreatePaymentRequestResponse>> createSwishDepositFlux() {
        return setupSwishDepositKafkaRequest()
            .flatMap(record -> {
                ReceiverOffset receiverOffset = record.receiverOffset();
                SwishDepositKafkaRequest swishDepositKafkaRequest = record.value();

                LOGGER.info("Received Swish deposit request: {}", swishDepositKafkaRequest);

                // HTTP-blocking callCreateDeposit-ish.
                return doSwishDepositChain(swishDepositKafkaRequest, receiverOffset);
            })
            .publish();
    }

    private void pollerFlux(ConnectableFlux<Tuple4<SwishDepositKafkaRequest, ReceiverOffset, DepositOrderResponse, CreatePaymentRequestResponse>> createSwishDepositFlux) {
        createSwishDepositFlux
            .flatMap(swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest -> {

                SwishDepositKafkaRequest swishDepositKafkaRequest = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT1();
                DepositOrderResponse depositOrderResponse = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT3();
                CreatePaymentRequestResponse createPaymentRequestResponse = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT4();


                Flux<RetrievePaymentResponse> retrievePaymentResponseFlux = pollSwishPaymentStatus(createPaymentRequestResponse.getLocation());
                retrievePaymentResponseFlux.subscribe();
                return retrievePaymentResponseFlux
                    .map(retrievePaymentResponse -> Tuples.of(swishDepositKafkaRequest, retrievePaymentResponse));
            })
            .map(swishDepositKafkaRequestAndRetrievePaymentResponse -> {

                SwishDepositKafkaRequest swishDepositKafkaRequest = swishDepositKafkaRequestAndRetrievePaymentResponse.getT1();
                RetrievePaymentResponse retrievePaymentResponse = swishDepositKafkaRequestAndRetrievePaymentResponse.getT2();

                WorkflowDepositFinalizedKafkaResponse workflowDepositFinalizedKafkaResponse = new WorkflowDepositFinalizedKafkaResponse();
                workflowDepositFinalizedKafkaResponse.setOrderId(swishDepositKafkaRequest.getOrderId());

                WorkflowDepositFinalizedKafkaResponse.DepositOrderState depositOrderState;
                switch (retrievePaymentResponse.getStatus()) {
                    case DECLINED:
                        depositOrderState = WorkflowDepositFinalizedKafkaResponse.DepositOrderState.CANCELED;
                        break;
                    case ERROR:
                        depositOrderState = WorkflowDepositFinalizedKafkaResponse.DepositOrderState.FAILED;
                        break;
                    case PAID:
                        depositOrderState = WorkflowDepositFinalizedKafkaResponse.DepositOrderState.SUCCESSFUL;
                        break;
                    default:
                        throw new RuntimeException("Unexpected status from Swish " + retrievePaymentResponse.getStatus());
                }

                workflowDepositFinalizedKafkaResponse.setDepositOrderState(depositOrderState);

                return SenderRecord.create(new ProducerRecord<Integer, WorkflowDepositFinalizedKafkaResponse>(workflowDepositFinalizedResponseTopic, workflowDepositFinalizedKafkaResponse), null);
            })
            .as(kafkaSenderForWorkflowDepositFinalizedResponse::send)
            .subscribe();
    }

    private void kafkaSenderFlux(ConnectableFlux<Tuple4<SwishDepositKafkaRequest, ReceiverOffset, DepositOrderResponse, CreatePaymentRequestResponse>> createSwishDepositFlux) {
        createSwishDepositFlux
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
    }

    private Disposable createKafkaConsumer() {

        ConnectableFlux<Tuple4<SwishDepositKafkaRequest, ReceiverOffset, DepositOrderResponse, CreatePaymentRequestResponse>> createSwishDepositFlux = createSwishDepositFlux();

        pollerFlux(createSwishDepositFlux);
        kafkaSenderFlux(createSwishDepositFlux);

        return createSwishDepositFlux.connect();
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
