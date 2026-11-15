package com.yarden.productservice.client;

import com.yarden.productservice.dto.ImageUploadResponse;
import com.yarden.productservice.exception.ImageServiceException;
import com.yarden.productservice.exception.ImageServiceUnavailableException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;


@Component
public class ImageClient {

    private static final String TOKEN_COOKIE = "ACCESS_TOKEN";

    @Autowired
    private RestClient imageRestClient;

    public ImageUploadResponse uploadProductImage(Long productId, MultipartFile file) {
        HttpHeaders partHeaders = new HttpHeaders();
        if (file.getContentType() != null) {
            partHeaders.setContentType(MediaType.parseMediaType(file.getContentType()));
        }
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(file.getResource(), partHeaders));

        try {
            return imageRestClient.post()
                    .uri("/api/images/products/{productId}", productId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header(HttpHeaders.COOKIE, TOKEN_COOKIE + "=" + currentToken())
                    .body(body)
                    .retrieve()
                    .body(ImageUploadResponse.class);
        } catch (HttpClientErrorException e) {
            throw new ImageServiceException(e.getStatusCode(), e.getResponseBodyAs(ProblemDetail.class));
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new ImageServiceUnavailableException(e);
        }
    }

    private String currentToken() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "";
        }
        HttpServletRequest request = attrs.getRequest();
        if (request.getCookies() == null) {
            return "";
        }
        for (Cookie cookie : request.getCookies()) {
            if (TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return "";
    }
}