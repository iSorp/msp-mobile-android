package ch.bfh.ti.these.msp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import ch.bfh.ti.these.msp.models.Action;

@Dao
public interface ActionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Action a);

    @Query("DELETE FROM actions")
    void deleteAll();

    @Query("SELECT a.id from actions as a " +
            "left join waypoints as wp on (a.waypoint_id = wp.id) " +
            "where wp.seq = :seq and a.mavlink_sensor = :sensorId and a.mavlink_command = :commandId")
    String getActionId(int seq, int sensorId, int commandId);
}
