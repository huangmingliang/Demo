package com.ktc.googledrive;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;

public class TestActivity extends AppCompatActivity {

    private static final String USED_INTENT = "USED_INTENT";

    AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
            Uri.parse("https://accounts.google.com/o/oauth2/auth") /* auth endpoint */,
            Uri.parse("https://oauth2.googleapis.com/token") /* token endpoint */
    );

    String clientId = "1061473829028-pe175g9so296drgesod5fuu8fiund2od.apps.googleusercontent.com";
    Uri redirectUri = Uri.parse("http://localhost");

    AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
            serviceConfiguration,
            clientId,
            AuthorizationRequest.RESPONSE_TYPE_CODE,
            redirectUri
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        builder.setScopes("profile");
        AuthorizationRequest request = builder.build();
        AuthorizationService authorizationService = new AuthorizationService(this);
        String action = "com.google.codelabs.appauth.HANDLE_AUTHORIZATION_RESPONSE";
        Intent postAuthorizationIntent = new Intent(this,WebActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, request.hashCode(), postAuthorizationIntent, 0);
        authorizationService.performAuthorizationRequest(request, pendingIntent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkIntent(intent);
    }

    private void checkIntent(@Nullable Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            Log.e("hml","user_intent="+intent.hasExtra(USED_INTENT));
            handleAuthorizationResponse(intent);
            intent.putExtra(USED_INTENT, true);
            if (TextUtils.isEmpty(action)){
                return;
            }
            switch (action) {
                case "com.google.codelabs.appauth.HANDLE_AUTHORIZATION_RESPONSE":
                    if (!intent.hasExtra(USED_INTENT)) {
                        handleAuthorizationResponse(intent);
                        intent.putExtra(USED_INTENT, true);
                    }
                    break;
                default:
                    // do nothing
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkIntent(getIntent());
    }

    private void handleAuthorizationResponse(Intent intent){
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        final AuthState authState = new AuthState(response, error);
        if (response != null) {
            Log.i("hml", String.format("Handled Authorization Response %s ", authState.toJsonString()));
            AuthorizationService service = new AuthorizationService(this);
            service.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
                @Override
                public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                    if (exception != null) {
                        Log.w("hml", "Token Exchange failed", exception);
                    } else {
                        if (tokenResponse != null) {
                            authState.update(tokenResponse, exception);
                            //persistAuthState(authState);
                            Log.i("hml", String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                        }
                    }
                }
            });
        }
    }
}
