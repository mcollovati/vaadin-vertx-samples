package org.atmosphere.vertx;

/**
 * Created by marco on 19/07/16.
 */
public interface ExposeAtmosphere {

    static AtmosphereCoordinator newCoordinator(VertxAtmosphere.Builder builder) {
        AtmosphereCoordinator coordinator = new AtmosphereCoordinator();
        builder.resource(ExposeAtmosphere.class);
        coordinator.configure(builder);
        return coordinator;
    }

}
