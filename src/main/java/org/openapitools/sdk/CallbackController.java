package org.openapitools.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.sdk.enums.StorageEnums;
import org.openapitools.sdk.storage.Storage;
import org.openapitools.sdk.utils.Utils;
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
import java.util.stream.Stream;

//@Controller
public class CallbackController {




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
        String codeVerifierCookie = storage.getState(request);
        if (codeVerifierCookie != null) {
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
                    var expValues = ((Map<?, ?>) data_).values().stream().map(value -> {
                        if (value instanceof String) {
                            var jwt = Utils.parseJWT((String) value);
                            if (jwt != null) {
                                return jwt.get("exp");
                            }
                        }
                        return null;
                    });
                    expValues = Stream.concat(expValues, Stream.ofNullable(((Map<?, ?>) data_).get("expires_in")));
                    Long maxExp = null;
                    for (var expValueIterator = expValues.iterator(); expValueIterator.hasNext(); ) {
                        var expValue = expValueIterator.next();
                        if (expValue instanceof String && StringUtils.isNumeric((CharSequence) expValue)) {
                            var exp = Long.parseLong((String) expValue);
                            if (maxExp == null || exp > maxExp) {
                                maxExp = exp;
                            }
                        }
                    }
                    if(maxExp==null){
                        maxExp= System.currentTimeMillis() + 3600 * 24 * 15;
                    }
                    long ttlSeconds = maxExp - Instant.now().getEpochSecond() ;
                    var value = new ObjectMapper().writeValueAsString(data_);
                    this.kindeClientSDK.getStorage()
                            .setItem(response, StorageEnums.TOKEN.getValue(), value, (int) ttlSeconds, "/", null, true, true);
                }else{
                    System.out.println("One or more of the claims were not verified.");
                }
            } catch (Exception e) {
                System.err.println(e);
            }

//            String redirectUrl = appConfig.getPostLoginURL() != null
//                    ? appConfig.getPostLoginURL()
//                    : logoutRedirectUri;
            String redirectUrl = kindeClientSDK.getLogoutRedirectUri();
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
}
