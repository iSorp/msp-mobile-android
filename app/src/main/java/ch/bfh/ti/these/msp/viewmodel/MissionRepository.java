package ch.bfh.ti.these.msp.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import ch.bfh.ti.these.msp.db.ActionDao;
import ch.bfh.ti.these.msp.db.MissionDao;
import ch.bfh.ti.these.msp.db.MissionDatabase;
import ch.bfh.ti.these.msp.db.WayPointDao;
import ch.bfh.ti.these.msp.http.MissionClient;
import ch.bfh.ti.these.msp.models.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ch.bfh.ti.these.msp.util.Definitions.BACKEND_HOST;
import static ch.bfh.ti.these.msp.util.Definitions.BACKEND_PORT;


public class MissionRepository {

    private final MissionDao missionDao;
    private final WayPointDao wayPointDao;
    private final ActionDao actionDao;
    private final ExecutorService executorService;

    public String host;
    public int port;

    MissionRepository(@NonNull Application application) {
        missionDao = MissionDatabase.getInstance(application).missionDao();
        wayPointDao = MissionDatabase.getInstance(application).wayPointDao();
        actionDao = MissionDatabase.getInstance(application).actionDao();
        executorService = Executors.newSingleThreadExecutor();


        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application);
            host = prefs.getString("backendAddress", BACKEND_HOST);
            port = Integer.parseInt(prefs.getString("backendPort", ""+BACKEND_PORT));
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    LiveData<List<Mission>> loadMission() {
        final MutableLiveData<List<Mission>> result = new MutableLiveData<>();
        executorService.execute(() -> {
            actionDao.deleteAll();
            wayPointDao.deleteAll();
            missionDao.deleteAll();
            List<Mission> missions =  MissionClient.getInstance(host, port).getMissionList();
            for (Mission m: missions) {
                missionDao.insert(m);
            }
            result.postValue(missions);
        });
        return result;
    }

    LiveData<Result<Mission>> loadWayPoints(String missionId) {
        final MutableLiveData<Result<Mission>> liveData = new MutableLiveData<>();
        executorService.execute(() -> {
            try {
                Mission m = missionDao.findOneById(missionId);
                if (m != null) {
                    Mission backendMission = MissionClient.getInstance(host, port).getMission(missionId);
                    for (Waypoint wp : backendMission.getWaypoints()) {
                        wayPointDao.insert(wp);
                        for (Action a : wp.getActions()) {
                            actionDao.insert(a);
                        }
                    }
                    Result<Mission> result = new Result<>();
                    if (backendMission.getWaypoints().size() > 0) {
                        result.status = true;
                        result.payload = backendMission;
                        liveData.postValue(result);
                    } else {
                        result.status = false;
                        liveData.postValue(result);
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        });
        return liveData;
    }


    public class Result<T> {
        private boolean status;

        private T payload;

        public boolean getStatus() {
            return status;
        }

        public T getPayload() {
            return payload;
        }
    }
}
