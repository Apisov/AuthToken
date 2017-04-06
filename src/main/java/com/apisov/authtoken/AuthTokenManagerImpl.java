package com.apisov.authtoken;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;


import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.SingleSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class AuthTokenManagerImpl implements AuthTokenManager {

    AccountManager accountManager;
    final Activity activity;

    public static AuthTokenManagerImpl newInstance(@NonNull Activity activity) {
        return new AuthTokenManagerImpl(activity);
    }

    private AuthTokenManagerImpl(Activity activity) {
        accountManager = AccountManager.get(activity);
        this.activity = activity;
    }

    @Override
    public Single<String> getToken(final String accountType, final String accountName, final String scope) {
        return Single.defer(new Callable<SingleSource<? extends String>>() {
            @Override
            public SingleSource<? extends String> call() throws Exception {
                return Single.create(new SingleOnSubscribe<String>() {
                    @Override
                    public void subscribe(final SingleEmitter<String> e) throws Exception {
                        accountManager.getAuthToken(
                                new Account(accountName, accountType),
                                scope,
                                null,
                                activity,
                                new AccountManagerCallback<Bundle>() {
                                    @Override
                                    public void run(AccountManagerFuture<Bundle> future) {
                                        handleToken(future, accountType, accountName, scope, e);
                                    }
                                },
                                null);
                    }
                });
            }
        });
    }

    void handleToken(AccountManagerFuture<Bundle> future, final String accountType, final String accountName, final String scope, final SingleEmitter<String> e) {
        Bundle bundle;
        try {
            bundle = future.getResult();
            Intent launch = (Intent) bundle.get(AccountManager.KEY_INTENT);
            if (launch != null) {
                AuthTokenActivity.startResolutionForResult(activity, launch)
                        .subscribe(new Consumer<AuthTokenActivity.GoogleApiResolutionEvent>() {
                            @Override
                            public void accept(AuthTokenActivity.GoogleApiResolutionEvent googleApiResolutionEvent) throws Exception {
                                if (googleApiResolutionEvent.isResolved()) {
                                    getToken(accountType, accountName, scope)
                                            .subscribe(new StringConsumer(e), new ThrowableConsumer(e));
                                } else {
                                    e.onError(new Exception(WAIT_FOR_RESOLVING_MESSAGE));
                                }
                            }
                        });
                return;
            }

            String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            e.onSuccess(token);
        } catch (Exception exception) {
            e.onError(exception);
        }
    }

    @Override
    public Single<String> invalidateToken(String accountType, String accountName, String scope, String token) {
        accountManager.invalidateAuthToken(accountType, token);
        return getToken(accountType, accountName, scope);
    }

    @Override
    public Single<String> invalidateToken(final String accountType, final String accountName, final String scope) {
        return getToken(accountType, accountName, scope).flatMap(new Function<String, SingleSource<? extends String>>() {
            @Override
            public SingleSource<? extends String> apply(String token) throws Exception {
                return invalidateToken(accountType, accountName, scope, token);
            }
        });
    }

    private static class StringConsumer implements Consumer<String> {
        private final SingleEmitter<String> e;

        public StringConsumer(SingleEmitter<String> e) {
            this.e = e;
        }

        @Override
        public void accept(String t) throws Exception {
            e.onSuccess(t);
        }
    }

    private static class ThrowableConsumer implements Consumer<Throwable> {
        private final SingleEmitter<String> e;

        public ThrowableConsumer(SingleEmitter<String> e) {
            this.e = e;
        }

        @Override
        public void accept(Throwable t1) throws Exception {
            e.onError(t1);
        }
    }
}