package ch.bfh.ti.these.msp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import ch.bfh.ti.these.msp.models.Mission;

import java.util.List;

public class MissionViewModel extends AndroidViewModel {

    private LiveData<List<Mission>> missions;
    private Mission item;
    private final MutableLiveData<Mission> selected = new MutableLiveData<>();
    private final MissionRepository repository;


    public MissionViewModel(@NonNull Application application) {
        super(application);
        repository = new MissionRepository(application);
    }

    public LiveData<List<Mission>> getMissions() {
        if (missions == null) {
            missions = repository.loadMission();
        }
        return missions;
    }

    public void selectMission(Mission item) {
        this.item  = item;
        selected.setValue(item);
    }

    public LiveData<Mission> getSelectedMission() {
        return selected;
    }

    public LiveData<MissionRepository.Result<Mission>> downloadWayPoints() {
        if (item != null) {
            return repository.loadWayPoints(item.getId());
        }
        return null;
    }
}
