package ch.bfh.ti.these.msp.ui;

import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import ch.bfh.ti.these.msp.R;

public class MissionActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission);
    }

    /*private class DownloadMissionResult extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {


            try {
                setStatusBusy(true);
                hasFiles = true;
                int index = 1;
                while (hasFiles) {
                    CompletableFuture cf = getMavlinkMaster().getFtpService().downloadFile("wp" + index++ + ".json").thenAccept(a -> {

                        System.out.println(a);
                        // TODO store to backend


                    }).exceptionally(throwable -> {
                        setStatusBusy(false);
                        if (throwable != null) {
                            hasFiles = false;
                            showToast(throwable.getMessage());
                        }
                        return null;
                    });

                    cf.get();
                }
                showToast("download beendet");
            } catch(Exception e){
                e.printStackTrace();
            }
            return true;
        }
    }*/
}
