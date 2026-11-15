package com.yarden.productservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

@Getter
public class ImageServiceException extends RuntimeException {

    private final HttpStatusCode status;
    private final ProblemDetail problem;

    public ImageServiceException(HttpStatusCode status, ProblemDetail problem) {
        super("image-service returned " + status);
        this.status = status;
        this.problem = problem;
    }
}
