package ch.bfh.ti.these.msp.ui;

import android.app.Service;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.*;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import ch.bfh.ti.these.msp.R;
import ch.bfh.ti.these.msp.mavlink.MavlinkMessageListener;
import dji.midware.usb.P3.UsbAccessoryService;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import io.dronefleet.mavlink.MavlinkMessage;


public class FpvFragment extends Fragment implements TextureView.SurfaceTextureListener, VideoFeeder.VideoDataListener {
    private VideoFeeder.VideoDataListener videoDataListener = null;
    private DJICodecManager codecManager = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fpv, parent, false);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupView();

        if (VideoFeeder.getInstance() != null)
            VideoFeeder.getInstance().getSecondaryVideoFeed().addVideoDataListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (VideoFeeder.getInstance() != null)
            VideoFeeder.getInstance().getSecondaryVideoFeed().removeVideoDataListener(this);
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (codecManager == null) {
            codecManager = new DJICodecManager(getContext().getApplicationContext(),
                    surface,
                    width,
                    height,
                    UsbAccessoryService.VideoStreamSource.Fpv);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (codecManager != null) {
            codecManager.cleanSurface();
            codecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private void setupView() {
        TextureView mVideoSurface = getActivity().findViewById(R.id.texture_video_previewer_surface);
        mVideoSurface.setSurfaceTextureListener(this);

        mVideoSurface.setOnTouchListener((View v, MotionEvent event) -> {
            ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            if (actionBar == null) return false;

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (actionBar.isShowing()) {
                    actionBar.hide();
                } else {
                    actionBar.show();
                }
                return true;
            } else return false;
        });
    }

    @Override
    public void onReceive(byte[] bytes, int size) {
        if (null != codecManager) {
            codecManager.sendDataToDecoder(bytes,
                    size,
                    UsbAccessoryService.VideoStreamSource.Fpv.getIndex());
        }
    }

}
