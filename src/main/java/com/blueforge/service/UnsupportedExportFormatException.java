package com.blueforge.service;

public class UnsupportedExportFormatException extends RuntimeException {

    public UnsupportedExportFormatException(String format) {
        super("Unsupported export format: " + format + ". Supported formats: markdown");
    }
}
