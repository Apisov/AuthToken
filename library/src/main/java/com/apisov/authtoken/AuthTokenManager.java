package com.apisov.authtoken;


import io.reactivex.Single;

public interface AuthTokenManager {

    String WAIT_FOR_RESOLVING_MESSAGE = "Wait for resolving";

    Single<String> getToken(String accountType, String accountName, String scope);

    Single<String> invalidateToken(String accountType, String accountName, String scope, String token);

    Single<String> invalidateToken(String accountType, String accountName, String scope);
}