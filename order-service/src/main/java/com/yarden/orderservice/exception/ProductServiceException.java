package com.yarden.orderservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

@Getter
public class ProductServiceException extends RuntimeException {

    private final HttpStatusCode status;
    private final ProblemDetail problem;

    public ProductServiceException(HttpStatusCode status, ProblemDetail problem) {
        super(problem != null ? problem.getDetail() : "Product service error");
        this.status = status;
        this.problem = problem;
    }
}