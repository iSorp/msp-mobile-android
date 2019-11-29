package ch.bfh.ti.these.msp.mavlink;

public class MavlinkConnectionInfo {

    private int systemId;
    private int compId;
    private boolean connected;

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
