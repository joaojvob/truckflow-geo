package com.truckflow.geo.service;

public class GeoServiceException extends RuntimeException {

    public GeoServiceException(String message) {
        super(message);
    }

    public GeoServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
