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
}
