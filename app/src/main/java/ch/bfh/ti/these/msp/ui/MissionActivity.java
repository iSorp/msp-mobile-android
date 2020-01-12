package ch.bfh.ti.these.msp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import ch.bfh.ti.these.msp.R;

public class MissionActivity extends FragmentActivity {

    private TextView statusBarText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.nav_mission_text);
        }

        statusBarText = findViewById(R.id.toolbar_status);
        statusBarText.setVisibility(View.INVISIBLE);
    }
}
