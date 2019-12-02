package ch.bfh.ti.these.msp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import ch.bfh.ti.these.msp.models.WayPoint;

@Dao
public interface WayPointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WayPoint wayPoint);

    @Query("DELETE FROM way_points")
    void deleteAll();
}
