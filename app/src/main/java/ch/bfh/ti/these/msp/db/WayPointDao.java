package ch.bfh.ti.these.msp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import ch.bfh.ti.these.msp.models.Waypoint;

@Dao
public interface WayPointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Waypoint waypoint);

    @Query("DELETE FROM waypoints")
    void deleteAll();
}
