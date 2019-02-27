package egghead.swish.swishcreatepayment.integration;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.tuple.Tuple2;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.concurrent.Future;

public class Certificate {
    private final static Logger LOGGER = LoggerFactory.getLogger(Certificate.class);

    private static Tuple2<KeyManagerFactory, TrustManagerFactory> getSSLContextPKCS12(String file, String password) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            keyStore.load(fileInputStream, password.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, password.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(keyStore);

            return Tuple2.of(keyManagerFactory, tmf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static SslContext createSslContext(String filePath) {
        try {

            Tuple2<KeyManagerFactory, TrustManagerFactory> certs = getSSLContextPKCS12(filePath, "swish");
            return SslContextBuilder.forServer(certs.getT1())
                .startTls(true)
                .trustManager(certs.getT2())
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSlContext.", e);
        }
    }
    public static void doSwishCall() {
        new Thread() {
            public void run() {
                SwishPaymentRequest swishPaymentRequest = new SwishPaymentRequest();
                swishPaymentRequest.setAmount("100");
                swishPaymentRequest.setCallbackUrl("www.youtube.com");
                swishPaymentRequest.setMessage("Britta");
                swishPaymentRequest.setCurrency("SEK");
                swishPaymentRequest.setPayeePaymentReference("ONETWOTHREE");
                swishPaymentRequest.setPayeeAlias("1231181189");

                String baseUrl = "HTTPS://mss.cpc.getswish.net/swish-cpcapi/api/v1/";
                String path = "paymentrequests";
                String certificateFilePath = Certificate.class.getClassLoader().getResource("swish.p12").getPath();
                SslContext sslContext = createSslContext(certificateFilePath);
                HttpClient httpClient = HttpClient.create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

                WebClient webClient = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .baseUrl(baseUrl)
                    .build();

                Scheduler scheduler = Schedulers.elastic();

                webClient
                    .post()
                    .uri(path)
                    .accept( MediaType.APPLICATION_JSON )
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(BodyInserters.fromObject(swishPaymentRequest))
                    .exchange()
                    .subscribeOn(scheduler)
                    .map(response -> {
                        HttpHeaders headers = response.headers().asHttpHeaders();
                        headers.get("Location");
                        LOGGER.info("Got respose {}", headers.get("Location"));

                        return headers.get("Location");
                        // TODO .. should be response Location.
                    }).doOnError(err -> {
                    LOGGER.warn("Got error {}", err.toString());
                }).subscribe();

            }
        }.start();
    }

    public static void main(String[] args) throws InterruptedException {
        doSwishCall();
        Thread.sleep(5000);
    }


    public static class SwishPaymentRequest {
        private String payeePaymentReference;
        private String callbackUrl;
        private String payeeAlias;
        private String amount;
        private String message;
        private String currency;

        public String getPayeePaymentReference() {
            return payeePaymentReference;
        }

        public void setPayeePaymentReference(String payeePaymentReference) {
            this.payeePaymentReference = payeePaymentReference;
        }

        public String getCallbackUrl() {
            return callbackUrl;
        }

        public void setCallbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
        }

        public String getPayeeAlias() {
            return payeeAlias;
        }

        public void setPayeeAlias(String payeeAlias) {
            this.payeeAlias = payeeAlias;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }

}
