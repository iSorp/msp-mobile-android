package ch.bfh.ti.these.msp.models;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class WayPointAction {

    @Embedded
    public WayPoint wayPoint;

    @Relation(parentColumn = "id", entityColumn = "way_point_id", entity = Action.class)
    public List<Action> actions;

}
