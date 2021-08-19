package com.github.mcollovati.vertxvaadin.flowdemo.authentication;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;

/**
 * Default mock implementation of {@link AccessControl}. This implementation
 * accepts any string as a user if the password is the same string, and
 * considers the user "admin" as the only administrator.
 */
public class BasicAccessControl implements AccessControl {

    private final ExampleAuthProvider authProvider = new ExampleAuthProvider();

    @Override
    public boolean signIn(String username, String password) {
        return authProvider.authenticate(new UsernamePasswordCredentials(username, password))
                .onSuccess(u -> CurrentUser.set(username))
                .map(u -> true)
                .otherwise(false)
                .result();
        /*
        if (username == null || username.isEmpty()) {
            return false;
        }

        if (!username.equals(password)) {
            return false;
        }

        CurrentUser.set(username);
        return true;
         */
    }

    @Override
    public boolean isUserSignedIn() {
        return !CurrentUser.get().isEmpty();
    }

    @Override
    public boolean isUserInRole(String role) {
        /*
        if ("admin".equals(role)) {
            // Only the "admin" user is in the "admin" role
            return getPrincipalName().equals("admin");
        }
        */
        if ("admin".equals(role)) {
            User user = User.fromName(getPrincipalName());
            authProvider.getAuthorizations(user);
            return RoleBasedAuthorization.create(role).match(user);
        }

        // All users are in all non-admin roles
        return true;
    }

    @Override
    public String getPrincipalName() {
        return CurrentUser.get();
    }

    @Override
    public void signOut() {
        VaadinSession.getCurrent().getSession().invalidate();
        UI.getCurrent().navigate("");
    }
}
