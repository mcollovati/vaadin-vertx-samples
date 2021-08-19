package com.github.mcollovati.vertxvaadin.flowdemo.authentication;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.auth.authorization.AuthorizationProvider;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;

public class ExampleAuthProvider implements AuthenticationProvider, AuthorizationProvider {

    @Override
    public void authenticate(Credentials credentials, Handler<AsyncResult<User>> resultHandler) {
        try {
            UsernamePasswordCredentials authInfo = (UsernamePasswordCredentials) credentials;
            authInfo.checkValid(null);

            if (authInfo.getUsername().equals(authInfo.getPassword())) {
                resultHandler.handle(Future.succeededFuture(User.fromName(authInfo.getUsername())));
            } else {
                resultHandler.handle(Future.failedFuture("invalid username/password"));
            }
        } catch (RuntimeException e) {
            resultHandler.handle(Future.failedFuture(e));
        }
    }

    @Override
    public void authenticate(JsonObject jsonObject, Handler<AsyncResult<User>> handler) {
        authenticate(new UsernamePasswordCredentials(jsonObject), handler);
    }

    @Override
    public String getId() {
        return "vaadin-example";
    }

    @Override
    public void getAuthorizations(User user, Handler<AsyncResult<Void>> handler) {
        if ("admin".equals(user.principal().getString("username"))) {
            user.authorizations().add(getId(), RoleBasedAuthorization.create("admin"));
        }
    }
}
