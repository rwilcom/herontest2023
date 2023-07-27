package com.nextcentury.locationsimulator;

import java.io.IOException;
import java.util.Date;

/**
 *
 *
 * http://www.mobility-services.in.tum.de/?p=2335
 *
 */
public class Location {

    public String name;
    public double latitude;
    public double longitude;

    public Location(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public String toString(){
        return this.latitude +","+ this.longitude;
    }
}

