package egghead.swish.swishcreatepayment.swish.model;

import java.util.Objects;

/**
 * @author mikael
 */
public class SwishPaymentStatus {

    // TODO correct values
    public enum Status {
        PENDING,
        SUCCESS,
        CANCELED,
        FAILED
    }

    public enum FailureCode {
        A,
        B,
        C
    }

    private Status status;
    private FailureCode failureCode;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public FailureCode getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(FailureCode failureCode) {
        this.failureCode = failureCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SwishPaymentStatus that = (SwishPaymentStatus) o;
        return status == that.status &&
            failureCode == that.failureCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, failureCode);
    }

    @Override
    public String toString() {
        return "SwishPaymentStatus{" +
            "status=" + status +
            ", failureCode=" + failureCode +
            '}';
    }
}
