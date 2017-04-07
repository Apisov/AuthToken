package com.apisov.authtoken;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class AuthTokenActivity extends Activity {
    public static final int AUTH_REQUEST_CODE = 100;
    private static final String INTENT_FOR_RESULT = "intent_for_result";

    private static PublishSubject<GoogleApiResolutionEvent> SUBJECT;

    private static PublishSubject<GoogleApiResolutionEvent> getSubject() {
        if (SUBJECT == null) {
            SUBJECT = PublishSubject.create();
        }
        return SUBJECT;
    }

    @NonNull
    public static Observable<GoogleApiResolutionEvent> startResolutionForResult(@NonNull Context context, Intent intentForResolution) {
        Intent intent = new Intent(context, AuthTokenActivity.class);
        intent.putExtra(INTENT_FOR_RESULT, intentForResolution);
        context.startActivity(intent);
        return getSubject().ofType(GoogleApiResolutionEvent.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent().getParcelableExtra(INTENT_FOR_RESULT);
        startActivityForResult(intent, AUTH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int code, int resultCode, Intent data) {
        if (AUTH_REQUEST_CODE == code && resultCode == RESULT_OK) {
            getSubject().onNext(new GoogleApiResolutionEvent(true));
        } else {
            getSubject().onNext(new GoogleApiResolutionEvent(false));
        }
        finish();
    }

    static class GoogleApiResolutionEvent {
        private final boolean resolved;

        public GoogleApiResolutionEvent(boolean resolved) {
            this.resolved = resolved;
        }

        public boolean isResolved() {
            return resolved;
        }
    }
}
