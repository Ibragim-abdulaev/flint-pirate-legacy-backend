package org.example.piratelegacy.auth;

import org.example.piratelegacy.auth.entity.UserResources;
import org.example.piratelegacy.auth.repository.UserRepository;
import org.example.piratelegacy.auth.repository.UserResourcesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserResourcesRepository userResourcesRepository;

    @BeforeEach
    void cleanDb() {
        userResourcesRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testRegisterCreatesResources() {
        Map<String, String> registerRequest = Map.of(
                "username", "testuser",
                "email", "testuser@example.com",
                "password", "password123"
        );

        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                "/api/auth/register",
                registerRequest,
                Map.class
        );

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = (String) registerResponse.getBody().get("token");
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<UserResources> resourcesResponse = restTemplate.exchange(
                "/api/resources",
                HttpMethod.GET,
                entity,
                UserResources.class
        );

        assertThat(resourcesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResources resources = resourcesResponse.getBody();
        assertThat(resources).isNotNull();
        assertThat(resources.getGold()).isEqualTo(100);
        assertThat(resources.getWood()).isEqualTo(50);
        assertThat(resources.getStone()).isEqualTo(50);
    }
}