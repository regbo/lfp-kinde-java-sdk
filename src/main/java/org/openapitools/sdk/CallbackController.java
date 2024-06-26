package org.openapitools.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.sdk.enums.StorageEnums;
import org.openapitools.sdk.storage.Storage;
import org.openapitools.sdk.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

//@Controller
public class CallbackController {

    private static final Logger log = LoggerFactory.getLogger(CallbackController.class);

    private KindeClientSDK kindeClientSDK;

    private Storage storage;

    public CallbackController(KindeClientSDK kindeClientSDK) {
        this.kindeClientSDK=kindeClientSDK;
        this.storage = this.kindeClientSDK.getStorage();
    }

//    @GetMapping("/api/auth/kinde_callback")
    public RedirectView callback(
            String code,
            String state,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        String storageState = storage.getState(request);
        if (StringUtils.isNotEmpty(storageState) && storageState.equals(state)) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.add("Kinde-SDK", "Java/1.2.0");

                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("client_id", kindeClientSDK.getClientId());
                body.add("client_secret", kindeClientSDK.getClientSecret());
                body.add("code", code);
                if(kindeClientSDK.getGrantType().equals("authorization_code_flow_pkce")){
//                  body.add("code_verifier", codeVerifierCookie);
                    String codeVerifier = storage.getCodeVerifier(request);
                    body.add("grant_type", "authorization_code");
                    body.add("code_verifier",codeVerifier);
                }else{
                    body.add("scope",kindeClientSDK.getScopes());
                    body.add("grant_type", kindeClientSDK.getGrantType());
                }
                body.add("redirect_uri", kindeClientSDK.getRedirectUri());

                HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

                ResponseEntity<Object> responseEntity = restTemplate.exchange(
                        kindeClientSDK.getDomain() + "/oauth2/token",
                        HttpMethod.POST,
                        requestEntity,
                        Object.class
                );

                Object data_ = responseEntity.getBody();
                Map<String, Object> data=(Map<String, Object>) data_;
                String accessToken = (String) data.get("access_token");

                Map<String, Object> payload=Utils.parseJWT(accessToken);
                boolean isAudienceValid = true;
//                if (audience != null && !audience.toString().equals("") && !audience.toString().equals("[]")) {
//                    String payloadAudience = payload.get("aud").toString();
//                    isAudienceValid = audience.toString().equals(payloadAudience);
//                }
                if (
                        payload.get("iss").equals(kindeClientSDK.getDomain()) &&
//                        payload.get("alg").equals("RS256") &&
                        isAudienceValid &&
                        (long) (Integer) payload.get("exp") > System.currentTimeMillis() / 1000L
                ) {
                    Function<Object, Long> toLong = obj -> {
                        if (obj instanceof Number) {
                            return ((Number) obj).longValue();
                        } else if (obj instanceof String str && StringUtils.isNumeric(str)) {
                            return Long.parseLong(str);
                        }
                        return null;
                    };
                    var exp = toLong.apply(((Map<?, ?>) data_).get("expires_in"));
                    if (exp == null) {
                        exp = ((Map<?, ?>) data_).values()
                                .stream()
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .map(Utils::parseJWT)
                                .filter(Objects::nonNull)
                                .map(v -> v.get("exp"))
                                .map(toLong).filter(Objects::nonNull).mapToLong(v -> v)
                                .max()
                                .orElseGet(() -> {
                                    return System.currentTimeMillis() + 3600 * 24 * 15;
                                });
                    }
                    long ttlSeconds = exp - Instant.now().getEpochSecond();
                    var value = new ObjectMapper().writeValueAsString(data_);
                    this.kindeClientSDK.getStorage()
                            .setItem(response, StorageEnums.TOKEN.getValue(), value, (int) ttlSeconds, "/", null, true, true);
                }else{
                    log.info("One or more of the claims were not verified.");
                }
            } catch (Exception e) {
                log.error("unexpected callback error",e);
            }

//            String redirectUrl = appConfig.getPostLoginURL() != null
//                    ? appConfig.getPostLoginURL()
//                    : logoutRedirectUri;
            String redirectUrl = getPostLoginUrl(request, response, code, state);
            return new RedirectView(redirectUrl);
//            return new RedirectView("");
        } else {

            String logoutUrl=UriComponentsBuilder.fromHttpUrl(kindeClientSDK.getLogoutEndpoint())
                    .queryParam("redirect", kindeClientSDK.getLogoutRedirectUri())
                    .build()
                    .toUriString();
            return new RedirectView(logoutUrl);
//            String logoutURL = kindeClientSDK.getDomain() + "/logout";
////            logoutURL += "?redirect=" + appConfig.getPostLogoutRedirectURL();
//            logoutURL += "?redirect=" + kindeClientSDK.getDomain() + "/logout";
//            return new RedirectView(logoutURL);
////            return new RedirectView("");
        }
    }

    protected String getPostLoginUrl(HttpServletRequest request, HttpServletResponse response, String code,
                                     String state) {
        return this.kindeClientSDK.getLogoutRedirectUri();
    }
}
