package ch.bfh.ti.these.msp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import ch.bfh.ti.these.msp.db.ActionDao;
import ch.bfh.ti.these.msp.db.MissionDao;
import ch.bfh.ti.these.msp.db.MissionDatabase;
import ch.bfh.ti.these.msp.db.WayPointDao;
import ch.bfh.ti.these.msp.http.MissionClient;
import ch.bfh.ti.these.msp.models.Action;
import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.WayPoint;

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

    MissionRepository(@NonNull Application application) {
        missionDao = MissionDatabase.getInstance(application).missionDao();
        wayPointDao = MissionDatabase.getInstance(application).wayPointDao();
        actionDao = MissionDatabase.getInstance(application).actionDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    LiveData<List<Mission>> loadMission() {
        final MutableLiveData<List<Mission>> result = new MutableLiveData<>();
        executorService.execute(() -> {
            actionDao.deleteAll();
            wayPointDao.deleteAll();
            missionDao.deleteAll();
            List<Mission> missions =  MissionClient.getInstance(BACKEND_HOST, BACKEND_PORT).getMissionList();
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
            Mission m = missionDao.findOneById(missionId);
            if (m != null) {
                Mission backendMission = MissionClient.getInstance(BACKEND_HOST, BACKEND_PORT).getMission(missionId);
                for (WayPoint wp: backendMission.getWayPoints()) {
                    wayPointDao.insert(wp);
                    for (Action a: wp.getActions()) {
                        actionDao.insert(a);
                    }
                }
                Result<Mission> result = new Result<>();
                if (backendMission.getWayPoints().size() > 0) {
                    result.status = true;
                    result.payload = backendMission;
                    liveData.postValue(result);
                } else {
                    result.status = false;
                    liveData.postValue(result);
                }
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
