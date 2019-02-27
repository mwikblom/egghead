package egghead.swish.swishcreatepayment.integration.model;

import java.util.Objects;

/**
 * @author mikael
 */
public class SwishPaymentRequestResponse {

    private String autoStartToken;
    private String location;

    public String getAutoStartToken() {
        return autoStartToken;
    }

    public void setAutoStartToken(String autoStartToken) {
        this.autoStartToken = autoStartToken;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SwishPaymentRequestResponse that = (SwishPaymentRequestResponse) o;
        return Objects.equals(autoStartToken, that.autoStartToken) &&
            Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(autoStartToken, location);
    }

    @Override
    public String toString() {
        return "SwishPaymentRequestResponse{" +
            "autoStartToken='" + autoStartToken + '\'' +
            ", location='" + location + '\'' +
            '}';
    }
}
