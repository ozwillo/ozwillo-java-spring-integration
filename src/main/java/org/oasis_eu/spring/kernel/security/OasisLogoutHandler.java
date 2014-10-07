package org.oasis_eu.spring.kernel.security;

import com.nimbusds.jose.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User: schambon
 * Date: 1/8/14
 */
public class OasisLogoutHandler implements LogoutSuccessHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OasisLogoutHandler.class);

    private RestTemplate restTemplate;

    @Autowired
    private OpenIdCConfiguration configuration;


    @Value("${kernel.auth.logout_endpoint}")
    private String logoutEndpoint;

    private String afterLogoutUrl;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        request.getSession().invalidate();

        // Okay, at this stage we have logged out from CK, we want to log out from OASIS too (otherwise it doesn't make sense to log out from CK)
        if (authentication instanceof OpenIdCAuthentication) {
            OpenIdCAuthentication token = (OpenIdCAuthentication) authentication;

            MultiValueMap<String, String> revocationRequest = new LinkedMultiValueMap<>();
            revocationRequest.add("token", token.getAccessToken());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", String.format("Basic %s", Base64.encode(String.format("%s:%s", configuration.getClientId(), configuration.getClientSecret()))));

            ResponseEntity<String> entity = restTemplate.exchange(configuration.getRevocationEndpoint(), HttpMethod.POST, new HttpEntity<>(revocationRequest, headers), String.class);

            if (entity.getStatusCode().value() != 200) { // RFC 7009 says the response should be 200 unless we provided an unknown token type
                LOGGER.error("Cannot handle revocation response with code: " + entity.getStatusCode());
                LOGGER.error("Payload is: " + entity.getBody());
            }

            response.sendRedirect(UriComponentsBuilder.fromHttpUrl(logoutEndpoint)
                    .queryParam("id_token_hint", token.getIdToken())
                    .queryParam("post_logout_redirect_uri", afterLogoutUrl)
                    .build()
                    .toUriString());

        } else {
            String s = authentication != null ? authentication.getClass().toString() : "null";
            LOGGER.error("Authentication token " + s + " is not an OIDCAuthenticationToken; I don't know what to do with it");

            response.sendRedirect(logoutEndpoint);
        }
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setAfterLogoutUrl(String afterLogoutUrl) {
        this.afterLogoutUrl = afterLogoutUrl;
    }

}