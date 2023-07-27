package com.nextcentury.locationsimulator;

import java.io.IOException;
import java.util.Date;

/**
 *
 *
 * http://www.mobility-services.in.tum.de/?p=2335
 *
 */
public class Route {

    public Location[] waypoints;

    public Route(Location... waypoints) {
        this.waypoints = waypoints;
    }
}

