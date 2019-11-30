package ch.bfh.ti.these.msp.mavlink;

public class MavlinkConnectionInfo {

    private int systemId;
    private int compId;
    private boolean connected;

    public MavlinkConnectionInfo() {
        this.systemId = -1;
        this.compId = -1;
        this.connected = false;
    }

    public MavlinkConnectionInfo(int systemId, int compId, boolean connected) {
        this.systemId = systemId;
        this.compId = compId;
        this.connected = connected;
    }

    public int getSystemId() {
        return systemId;
    }

    public int getCompId() {
        return compId;
    }

    public boolean isConnected() {
        return connected;
    }

}
