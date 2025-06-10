package com.morichal.demo.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

@Service
public class TensorFlowService {
    public String reconocerNumero(MultipartFile image) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            body.add("image", new InputStreamResource(image.getInputStream()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }

                @Override
                public long contentLength() {
                    try {
                        return image.getSize();
                    } catch (Exception e) {
                        return -1;
                    }
                }
            });
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error reading image input stream", e);
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:5001/predict", requestEntity, String.class);
        // Procesa el JSON de respuesta y extrae el número
        return response.getBody();
    }
}
