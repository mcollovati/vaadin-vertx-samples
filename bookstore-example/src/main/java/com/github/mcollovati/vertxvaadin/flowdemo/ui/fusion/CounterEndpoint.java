package com.github.mcollovati.vertxvaadin.flowdemo.ui.fusion;

import javax.annotation.security.RolesAllowed;

import com.vaadin.fusion.Endpoint;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Endpoint
@AnonymousAllowed
public class CounterEndpoint {
    /**
     * A method that adds one to the argument.
     */
    public int addOne(int number) {
        return number + 1;
    }

    @RolesAllowed("ROLE_ADMIN")
    public int addTen(int number) {
        return number + 10;
    }

    @RolesAllowed("ROLE_SQUARE")
    public int square(int number) {
        return number * number;
    }

}
