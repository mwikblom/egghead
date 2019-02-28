package egghead.swish.swishcreatepayment.integration.model;

/**
 * @author mikael
 */
public class CreatePaymentRequestResponse {
    private final String location;
    private final String paymentRequestToken;

    public CreatePaymentRequestResponse(String location, String paymentRequestToken) {
        this.location = location;
        this.paymentRequestToken = paymentRequestToken;
    }

    public String getLocation() {
        return location;
    }

    public String getPaymentRequestToken() {
        return paymentRequestToken;
    }

    @Override
    public String toString() {
        return "CreatePaymentRequestResponse{" +
            "location='" + location + '\'' +
            ", paymentRequestToken='" + paymentRequestToken + '\'' +
            '}';
    }
}
