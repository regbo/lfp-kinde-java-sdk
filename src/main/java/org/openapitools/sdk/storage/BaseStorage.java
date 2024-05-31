package org.openapitools.sdk.storage;

import org.openapitools.sdk.enums.StorageEnums;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
//import org.springframework.web.util.CookieUtils;

public class BaseStorage {

//    private final String PREFIX = "kinde";
//
//    public String getStorage(HttpServletRequest request) {
//        Cookie[] cookies = request.getCookies();
//        if (cookies != null) {
//            for (Cookie cookie : cookies) {
//                if (cookie.getName().equals(PREFIX)) {
//                    return cookie.getValue();
//                }
//            }
//        }
//        return null;
//    }
//
//    public String getItem(HttpServletRequest request, String key) {
//        String newKey = getKey(key);
//        Cookie[] cookies = request.getCookies();
//        if (cookies != null) {
//            for (Cookie cookie : cookies) {
//                if (cookie.getName().equals(newKey)) {
//                    return cookie.getValue();
//                }
//            }
//        }
//        return null;
//    }
//
//    public void setItem(HttpServletResponse response, String key, String value, int expiresOrOptions, String path, String domain, boolean secure, boolean httpOnly) {
//        String newKey = getKey(key);
//        Cookie cookie = new Cookie(newKey, value);
//        cookie.setMaxAge(expiresOrOptions);
//        cookie.setPath(path);
//        cookie.setDomain(domain);
//        cookie.setSecure(secure);
//        cookie.setHttpOnly(httpOnly);
//        response.addCookie(cookie);
//    }
//
//    public void removeItem(HttpServletResponse response, String key) {
//        String newKey = getKey(key);
//        Cookie cookie = new Cookie(newKey, "");
//        cookie.setMaxAge(0);
//        response.addCookie(cookie);
//    }
//
//    public void clear(HttpServletResponse response) {
//        removeItem(response, StorageEnums.TOKEN.getValue());
//        removeItem(response, StorageEnums.STATE.getValue());
//        removeItem(response, StorageEnums.CODE_VERIFIER.getValue());
//        removeItem(response, StorageEnums.USER_PROFILE.getValue());
//    }

//
//    private String getKey(String key) {
//        return PREFIX + '_' + key;
//    }
//


    private static final String DEFAULT_PREFIX = "kinde";
    private Map<String, String> storage;


    private String prefix;
//    public Map<String, String> getStorage() {
//        if (storage == null) {
//            storage = new HashMap<>();
//        }
//        return storage;
//    }

    public String getItem(HttpServletRequest request, String key) {
        String cookieName = getKey(key);
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return "";
    }

    public void setItem(HttpServletResponse response,
                        String key,
                        String value,
                        int maxAgeSeconds,
                        String path,
                        String domain,
                        boolean secure,
                        boolean httpOnly) {
        String newKey = getKey(key);
        Cookie cookie = new Cookie(newKey, value);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setPath(path);
        cookie.setDomain(domain);
        cookie.setSecure(secure);
        cookie.setHttpOnly(httpOnly);
        response.addCookie(cookie);
    }

    public void setItem(HttpServletResponse response, String key, String value) {
        setItem(response,key,value,0,"","",true,false);
    }

    public void setItem(HttpServletResponse response, String key, String value, int expiresOrOptions) {
        setItem(response,key,value,expiresOrOptions,"","",true,false);
    }

    public void removeItem(HttpServletResponse response, String key) {
        String cookieName = getKey(key);
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public void clear(HttpServletResponse response) {
        removeItem(response,StorageEnums.TOKEN.getValue());
        removeItem(response,StorageEnums.STATE.getValue());
        removeItem(response,StorageEnums.CODE_VERIFIER.getValue());
        removeItem(response,StorageEnums.USER_PROFILE.getValue());
    }


    private String getKey(String key) {
        return Optional.ofNullable(getPrefix()).orElse(DEFAULT_PREFIX)+ '_' + key;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }


//    private final String PREFIX = "kinde";
//
//    private Map<String, String> storage;
//
//    public Map<String, String> getStorage() {
//        if (storage == null) {
//            storage = new HashMap<>();
//            String kindeCookie = CookieUtils.getCookie(PREFIX);
//            if (kindeCookie != null) {
//                String[] keyValuePairs = kindeCookie.split(";");
//                for (String keyValuePair : keyValuePairs) {
//                    String[] keyAndValue = keyValuePair.split("=");
//                    storage.put(keyAndValue[0], keyAndValue[1]);
//                }
//            }
//        }
//        return storage;
//    }
//
//    public String getItem(String key) {
//        return getStorage().get(key);
//    }
//
//    public void setItem(
//            String key,
//            String value,
//            int expires_or_options,
//            String path,
//            String domain,
//            boolean secure,
//            boolean httpOnly
//    ) {
//        String newKey = getKey(key);
//        storage.put(newKey, value);
//        CookieUtils.setCookie(newKey, value, expires_or_options, path, domain, secure, httpOnly);
//    }
//
//    public void removeItem(String key) {
//        String newKey = getKey(key);
//        if (storage.containsKey(newKey)) {
//            storage.remove(newKey);
//            CookieUtils.deleteCookie(newKey);
//        }
//    }
//
//    public void clear() {
//        for (StorageEnums storageEnum : StorageEnums.values()) {
//            removeItem(storageEnum.getValue());
//        }
//    }
//
//    private String getKey(String key) {
//        return PREFIX + "_" + key;
//    }
}