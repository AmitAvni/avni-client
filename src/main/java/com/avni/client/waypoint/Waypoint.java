package com.avni.client.waypoint;

/** A named world location with a color. */
public class Waypoint {
    public String name;
    public int x;
    public int y;
    public int z;
    public int color; // ARGB
    public boolean visible = true;

    public Waypoint() {
    }

    public Waypoint(String name, int x, int y, int z, int color) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.visible = true;
    }
}
