package com.yarden.orderservice.client;

import com.yarden.orderservice.client.dto.StockDecreaseRequest;
import com.yarden.orderservice.client.dto.StockItemRequest;
import com.yarden.orderservice.client.dto.StockItemResponse;
import com.yarden.orderservice.exception.ProductServiceException;
import com.yarden.orderservice.exception.ProductServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ProductClient {

    @Autowired
    private RestClient productRestClient;

    public List<StockItemResponse> decreaseStock(List<StockItemRequest> items) {
        try {
            return productRestClient.post()
                    .uri("/api/products/stock/decrease")
                    .header(HttpHeaders.COOKIE, "ACCESS_TOKEN=" + currentToken())
                    .body(new StockDecreaseRequest(items))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<StockItemResponse>>() {});
        } catch (HttpClientErrorException ex) {
            ProblemDetail problem = ex.getResponseBodyAs(ProblemDetail.class);
            throw new ProductServiceException(ex.getStatusCode(), problem);
        } catch (HttpServerErrorException | ResourceAccessException ex) {
            throw new ProductServiceUnavailableException(ex);
        }
    }

    private String currentToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof String token) {
            return token;
        }
        return "";
    }
}
