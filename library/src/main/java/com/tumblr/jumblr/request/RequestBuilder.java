package com.tumblr.jumblr.request;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.responses.JsonElementDeserializer;
import com.tumblr.jumblr.responses.ResponseWrapper;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import dev.notrobots.timeline.oauth.OAuth2TokenStore;
import dev.notrobots.timeline.oauth.OAuth2Token;
import dev.notrobots.timeline.oauth.apis.TumblrApi20;

/**
 * Where requests are made from
 * @author jc
 */
public class RequestBuilder {
    private OAuth20Service service;
    private String hostname = "api.tumblr.com";
    private String version = "0.0.13";
    private final JumblrClient client;
    private String userAgent = "jumblr/" + this.version;
    private String callbackUrl;
    private OAuth2TokenStore tokenStore;    //TODO: This should use a generic TokenStore
    private OAuth2Token lastToken;

    public RequestBuilder(JumblrClient client) {
        this.client = client;
    }

    public String getRedirectUrl(String path) {
        OAuthRequest request = this.constructGet(path, null);
        boolean presetVal = HttpURLConnection.getFollowRedirects();
        HttpURLConnection.setFollowRedirects(false);
        Response response = null;

        try {
            response = sendRequest(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        HttpURLConnection.setFollowRedirects(presetVal);
        if (response.getCode() == 301 || response.getCode() == 302) {
            return response.getHeader("Location");
        } else {
            throw new JumblrException(response);
        }
    }

    public ResponseWrapper postMultipart(String path, Map<String, ?> bodyMap) {
        OAuthRequest request = this.constructPost(path, bodyMap);
        OAuthRequest newRequest;
        Response response;
        try {
            newRequest = RequestBuilder.convertToMultipart(request, bodyMap);
            response = sendRequest(newRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return clear(response);
    }

    public ResponseWrapper post(String path, Map<String, ?> bodyMap) {
        OAuthRequest request = this.constructPost(path, bodyMap);

        try {
            return clear(sendRequest(request));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseWrapper get(String path, Map<String, ?> map) {
        OAuthRequest request = this.constructGet(path, map);

        try {
            return clear(sendRequest(request));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public OAuthRequest constructGet(String path, Map<String, ?> queryParams) {
        String url = "https://" + hostname + "/v2" + path;
        OAuthRequest request = new OAuthRequest(Verb.GET, url);
        if (queryParams != null) {
            for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
                request.addQuerystringParameter(entry.getKey(), entry.getValue().toString());
            }
        }
        request.addHeader("User-Agent", userAgent);

        return request;
    }

    private OAuthRequest constructPost(String path, Map<String, ?> bodyMap) {
        String url = "https://" + hostname + "/v2" + path;
        OAuthRequest request = new OAuthRequest(Verb.POST, url);

        for (Map.Entry<String, ?> entry : bodyMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null || value instanceof File) {
                continue;
            }
            request.addBodyParameter(key, value.toString());
        }
        request.addHeader("User-Agent", userAgent);

        return request;
    }

    public void setConsumer(String consumerKey, String consumerSecret) {
        service = new ServiceBuilder(consumerKey)
                .apiKey(consumerKey)
                .apiSecret(consumerSecret)
                .userAgent(userAgent)
                .build(new TumblrApi20());
    }

    /* package-visible for testing */ ResponseWrapper clear(Response response) {
        if (response.getCode() == 200 || response.getCode() == 201) {
            String json = null;
            try {
                json = response.getBody();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                Gson gson = new GsonBuilder().
                        registerTypeAdapter(JsonElement.class, new JsonElementDeserializer()).
                        create();
                ResponseWrapper wrapper = gson.fromJson(json, ResponseWrapper.class);
                if (wrapper == null) {
                    throw new JumblrException(response);
                }
                wrapper.setClient(client);
                return wrapper;
            } catch (JsonSyntaxException ex) {
                throw new JumblrException(response);
            }
        } else {
            throw new JumblrException(response);
        }
    }

    private void sign(OAuthRequest request) {
        if (tokenStore == null) {
            throw new RuntimeException("TokenStore was not provided");
        }

        OAuth2Token token = tokenStore.fetch(client.getClientId());

        if (token == null) {
            throw new RuntimeException("Cannot sign request. Token is null");
        }

        request.addHeader("Authorization", "Bearer " + token.getAccessToken());
    }

    public static OAuthRequest convertToMultipart(OAuthRequest request, Map<String, ?> bodyMap) throws IOException {
        return new MultipartConverter(request, bodyMap).getRequest();
    }

    public String getHostname() {
        return hostname;
    }

    /**
     * Set hostname without protocol
     *
     * @param host such as "api.tumblr.com"
     */
    public void setHostname(String host) {
        this.hostname = host;
    }

    /**
     * Executes the given request and signs it with the most recent oauth2 access token.
     *
     * If the current token is expired it will be refreshed.
     *
     * @param request The request to send
     * @return Request response
     */
    private Response sendRequest(OAuthRequest request) {
        if (tokenStore == null) {
            throw new RuntimeException("Token store was not provided");
        }

        if (lastToken == null) {
            lastToken = tokenStore.fetch(client.getClientId());
        }

        if (lastToken.isExpired()) {
            try {
                lastToken = new OAuth2Token(service.refreshAccessToken(lastToken.getRefreshToken()));
                tokenStore.store(client.getClientId(), lastToken);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        try {
            sign(request);
            return service.execute(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public OAuth2TokenStore getTokenStore() {
        return tokenStore;
    }

    public void setTokenStore(OAuth2TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }
}
