package ch.bfh.ti.these.msp.models;

import java.util.ArrayList;

public class Mission {

    private String name;
    private ArrayList<WayPoint> wayPoints = new ArrayList<>();


    public String getName() {
        return name;
    }

    public ArrayList<WayPoint> getWayPoints() {
        return wayPoints;
    }

    public void addWayPoint(WayPoint wayPoint) {
        this.wayPoints.add(wayPoint);
    }
}
