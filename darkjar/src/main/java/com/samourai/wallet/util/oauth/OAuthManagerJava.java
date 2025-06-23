package com.samourai.wallet.util.oauth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.samourai.wallet.api.backend.BackendOAuthApi;
import com.samourai.wallet.api.backend.beans.RefreshTokenResponse;

public class OAuthManagerJava implements OAuthManager {
    private OAuthImpl oAuthImpl;
    private String apiKey;
    private OAuthApi oAuthApi;

    private DecodedJWT accessToken;
    private DecodedJWT refreshToken;

    public OAuthManagerJava(String apiKey, OAuthApi oAuthApi) {
        this.oAuthImpl = new OAuthImpl();
        this.apiKey = apiKey;
        this.oAuthApi = oAuthApi;
    }

    public String getOAuthAccessToken() throws Exception {
        if (accessToken != null) {
            boolean valid = oAuthImpl.validate(accessToken);
            if (valid) {
                // accessToken is valid
                return oAuthImpl.tokenToString(accessToken);
            }
        }
        return newAccessToken();
    }

    protected String newAccessToken() throws Exception {
        if (refreshToken != null) {
            boolean valid = oAuthImpl.validate(refreshToken);
            if (valid) {
                // refreshToken is valid => refresh
                String accessTokenStr = oAuthApi.oAuthRefresh(oAuthImpl.tokenToString(refreshToken));
                this.accessToken = oAuthImpl.decode(accessTokenStr);
                return oAuthImpl.tokenToString(accessToken);
            }
        }

        // no refreshToken => authenticate
        RefreshTokenResponse.Authorization auth = oAuthApi.oAuthAuthenticate(apiKey);
        this.accessToken = oAuthImpl.decode(auth.access_token);
        this.refreshToken = oAuthImpl.decode(auth.refresh_token);
        return oAuthImpl.tokenToString(accessToken);
    }
}
