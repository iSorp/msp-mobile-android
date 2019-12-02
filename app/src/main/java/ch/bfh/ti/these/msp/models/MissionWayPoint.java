package ch.bfh.ti.these.msp.models;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class MissionWayPoint {

    @Embedded
    Mission mission;

    @Relation(parentColumn = "id", entityColumn = "mission_id", entity = WayPoint.class)
    List<WayPointAction> wayPoints;

}
