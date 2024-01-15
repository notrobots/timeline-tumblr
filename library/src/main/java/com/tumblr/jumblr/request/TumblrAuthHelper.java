package com.tumblr.jumblr.request;

import com.tumblr.jumblr.JumblrClient;

import org.jetbrains.annotations.NotNull;

import dev.notrobots.timeline.oauth.OAuth2Config;
import dev.notrobots.timeline.oauth.OAuth2Helper;
import dev.notrobots.timeline.oauth.OAuth2Token;
import dev.notrobots.timeline.oauth.OAuth2TokenStore;
import dev.notrobots.timeline.oauth.apis.TumblrApi20;

public class TumblrAuthHelper extends OAuth2Helper<JumblrClient> {
    public TumblrAuthHelper(@NotNull OAuth2Config authConfig, @NotNull OAuth2TokenStore tokenStore) {
        super(authConfig, tokenStore, TumblrApi20.instance());
    }

    @NotNull
    @Override
    public JumblrClient onCreateClient(@NotNull OAuth2Token token, @NotNull OAuth2TokenStore tokenStore, @NotNull OAuth2Config authConfig) {
        return new JumblrClient(getAuthService(), tokenStore, authConfig, getRandomUniqueID()); //XXX: This could be replace by reflections
    }
}
