package org.sonarqube.auth.choerodonoauth;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.AccessTokenExtractor;
import com.github.scribejava.core.extractors.JsonTokenExtractor;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.utils.OAuthEncoder;
import com.github.scribejava.core.utils.Preconditions;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  17:08 2019/4/30
 * Description:
 */
public class ChoerodonOAuthApi extends DefaultApi20 {
    private final String url;

    public ChoerodonOAuthApi(String url) {
        super();

        this.url = url;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return url + "/oauth/oauth/token";
    }

    @Override
    public Verb getAccessTokenVerb() {
        return Verb.POST;
    }

    @Override
    public AccessTokenExtractor getAccessTokenExtractor() {
        return new JsonTokenExtractor();
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig config) {
        Preconditions.checkValidUrl(config.getCallback(), "Must provide a valid url as callback. Choerodon does not support OOB");
        String authUrl = String.format("%s/oauth/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code", this.url, config.getApiKey(), OAuthEncoder.encode(config.getCallback()));
        if (config.hasScope()) {
            authUrl += "&scope=" + OAuthEncoder.encode(config.getScope());
        }
        return authUrl;
    }
}
