package io.mifos.reporting.api.v1.domain;

public class Sample {
    private Object identifier;
    private Object payload;
    public Sample(){
        super();
    }
    public static Sample create(String xxxx, String yyy) {
        return new Sample();
    }

    public void setIdentifier(Object identifier) {
        this.identifier = identifier;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
