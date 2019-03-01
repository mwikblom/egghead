package egghead.swish.swishcreatepayment.swish.model;

/**
 * @author mikael
 */
public enum SwishErrorCode {

    ACMT01("Counterpart is not activated"),
    ACMT03("Payer not Enrolled"),
    ACMT07("Payee not Enrolled"),

    AM02("Amount value is too large"),
    AM03("Invalid or missing Currency"),
    AM06("Specified transaction amount is less than agreed minimum"),

    BANKIDCL("Payer cancelled bankid signing"),
    BANKIDONGOING("BankID already in use"),
    BANKIDUNKN("BankID are not able to authorize the payment"),

    BE18("Payer alias is invalid"),

    DS24("Swish timed out waiting for an answer from the banks after payment was started"),

    FF08("PaymentReference is invalid"),
    FF10("Bank system processing error"),

    PA02("Amount value is missing or not a valid number"),

    RF02("Original Payment not found or original payment is more than 13 months old"),
    RF03("Payer alias in the refund does not match the payee alias in the original payment"),
    RF04("Payer organization number do not match original payment payee organization number"),
    RF06("The Payer SSN in the original payment is not the same as the SSN for the current Payee"),
    RF07("Transaction declined"),
    RF08("Amount value is too large or amount exceeds the amount of the original payment minus any previous refunds"),

    RP01("Missing Merchant Swish Number"),
    RP02("Wrong formated message"),
    RP03("Callback URL is missing or does not use Https"),
    RP06("A payment request already exist for that payer"),

    TM01("Swish timed out before the payment was started"),

    UNKNOWN("Unknown error code");

    private final String message;

    SwishErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return this.name() + ": " + message;
    }
}
