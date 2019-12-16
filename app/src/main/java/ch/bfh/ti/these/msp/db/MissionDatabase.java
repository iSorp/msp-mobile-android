package ch.bfh.ti.these.msp.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import ch.bfh.ti.these.msp.models.Action;
import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.Waypoint;

@Database(entities = {Mission.class, Waypoint.class, Action.class}, version = 1)
public abstract class MissionDatabase extends RoomDatabase {
    private static MissionDatabase INSTANCE;

    public abstract MissionDao missionDao();

    public abstract WayPointDao wayPointDao();

    public abstract ActionDao actionDao();

    private static final Object sLock = new Object();

    public static MissionDatabase getInstance(Context context) {
        synchronized (sLock) {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                        MissionDatabase.class, "MSP.db")
                        .build();
            }
            return INSTANCE;
        }
    }
}
