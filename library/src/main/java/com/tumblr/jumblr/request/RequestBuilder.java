package com.tumblr.jumblr.request;

import com.github.scribejava.core.oauth.OAuth20Service;
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

import org.jetbrains.annotations.NotNull;

import dev.notrobots.timeline.oauth.OAuth2Client;
import dev.notrobots.timeline.oauth.OAuth2Config;
import dev.notrobots.timeline.oauth.OAuth2Helper;
import dev.notrobots.timeline.oauth.OAuth2TokenStore;
import dev.notrobots.timeline.oauth.apis.TumblrApi20;

/**
 * Where requests are made from
 * @author jc
 */
public class RequestBuilder extends OAuth2Client {
    private String hostname = "api.tumblr.com";
    private final JumblrClient client;

    public RequestBuilder(JumblrClient client, @NotNull OAuth20Service authService, @NotNull OAuth2TokenStore tokenStore, OAuth2Config authConfig, @NotNull String clientId) {
        super(authService, tokenStore, authConfig, clientId);
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
        request.addHeader("User-Agent", getAuthConfig().getUserAgent());

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
        request.addHeader("User-Agent", getAuthConfig().getUserAgent());

        return request;
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

    public String getUserAgent() {
        return getAuthConfig().getUserAgent();
    }
}
