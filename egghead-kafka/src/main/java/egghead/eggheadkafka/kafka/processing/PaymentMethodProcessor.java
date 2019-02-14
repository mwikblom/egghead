package egghead.eggheadkafka.kafka.processing;

import egghead.eggheadkafka.kafka.model.Request;
import egghead.eggheadkafka.kafka.model.Response;
import egghead.eggheadkafka.kafka.processing.swish.SwishProcessor;

/**
 * @author mikael
 */
public interface PaymentMethodProcessor {

    Response process(Request request);

    class Factory {
        static PaymentMethodProcessor createProcessor(Request.Country country, Request.PaymentMethod paymentMethod) {
            switch (country) {
                case SWEDEN:
                    switch (paymentMethod) {
                        case SWISH:
                            return new SwishProcessor();
                        case NORDEA:
                        default:
                            throw new UnsupportedOperationException("Unsupported payment method " + paymentMethod + " for country " + country);
                    }
                case FINLAND:
                default:
                    throw new UnsupportedOperationException("Unsupported country " + country);
            }
        }
    }
}
