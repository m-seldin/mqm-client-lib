/*
 * Copyright 2017 Hewlett-Packard Development Company, L.P.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.mqm.client.exception;

/**
 * Exception is thrown when MQM server returns unexpected response. Which means that intended result was not achieved.
 */
public abstract class RequestException extends RuntimeException {

    private String description;
    private String errorCode;
    private int statusCode;
    private String reason;

    public RequestException() {
    }

    public RequestException(String message) {
        super(message);
    }

    public RequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestException(Throwable cause) {
        super(cause);
    }

    public RequestException(String message, String description, String errorCode, int statusCode, String reason, Throwable cause) {
        super(message, cause);
        this.description = description;
        this.errorCode = errorCode;
        this.statusCode = statusCode;
        this.reason = reason;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDescription() {
        return description;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReason() {
        return reason;
    }
}
