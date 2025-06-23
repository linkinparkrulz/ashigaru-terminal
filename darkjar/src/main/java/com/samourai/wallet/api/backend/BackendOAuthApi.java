package com.samourai.wallet.api.backend;

import com.samourai.wallet.api.backend.beans.RefreshTokenResponse;
import com.samourai.wallet.util.oauth.OAuthApi;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BackendOAuthApi implements OAuthApi {
    private Logger log = LoggerFactory.getLogger(BackendApi.class);

    private static final String URL_GET_AUTH_LOGIN = "/auth/login";
    private static final String URL_GET_AUTH_REFRESH = "/auth/refresh";

    private IBackendClient httpClient;
    private String urlBackend;

    public BackendOAuthApi(IBackendClient httpClient, String urlBackend) {
        this.httpClient = httpClient;
        this.urlBackend = urlBackend;
    }

    @Override
    public RefreshTokenResponse.Authorization oAuthAuthenticate(String apiKey) throws Exception {
        String url = urlBackend + URL_GET_AUTH_LOGIN;
        if (log.isDebugEnabled()) {
            log.debug("tokenAuthenticate");
        }
        Map<String, String> postBody = new HashMap<String, String>();
        postBody.put("apikey", apiKey);
        RefreshTokenResponse response =
                httpClient.postUrlEncoded(url, RefreshTokenResponse.class, null, postBody);

        if (response.authorizations == null|| StringUtils.isEmpty(response.authorizations.access_token)) {
            throw new Exception("Authorization refused. Invalid apiKey?");
        }
        return response.authorizations;
    }

    @Override
    public String oAuthRefresh(String refreshTokenStr) throws Exception {
        String url = urlBackend + URL_GET_AUTH_REFRESH;
        if (log.isDebugEnabled()) {
            log.debug("tokenRefresh");
        }
        Map<String, String> postBody = new HashMap<String, String>();
        postBody.put("rt", refreshTokenStr);
        RefreshTokenResponse response =
                httpClient.postUrlEncoded(url, RefreshTokenResponse.class, null, postBody);

        if (response.authorizations == null || StringUtils.isEmpty(response.authorizations.access_token)) {
            throw new Exception("Authorization refused. Invalid apiKey?");
        }
        return response.authorizations.access_token;
    }
}
