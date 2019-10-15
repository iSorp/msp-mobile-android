package ch.bfh.ti.these.msp.mavlink;

import java.io.InputStream;
import java.io.OutputStream;

public interface MavlinkBridge {
    InputStream getInputStream();
    OutputStream getOutputStream();
}
