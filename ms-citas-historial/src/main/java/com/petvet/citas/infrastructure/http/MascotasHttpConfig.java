package com.petvet.citas.infrastructure.http;

import com.petvet.citas.domain.MascotaClient;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MascotasHttpConfig {

    @Bean
    public MascotaClient mascotaClient(
            RestClient.Builder builder,
            @Value("${petvet.mascotas.url}") String baseUrl,
            Clock clock) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        RestClient restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        return new MascotasRestClient(restClient, clock);
    }
}
