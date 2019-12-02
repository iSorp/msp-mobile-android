package ch.bfh.ti.these.msp.db;

import androidx.room.*;
import ch.bfh.ti.these.msp.models.Mission;

@Dao
public interface MissionDao {

    @Query("DELETE FROM missions")
    void deleteAll();

    @Query("SELECT * FROM missions where id = :id")
    Mission findOneById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Mission m);
}
