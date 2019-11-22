package ch.bfh.ti.these.msp.mavlink.model;

import io.dronefleet.mavlink.common.MavCmd;

public class MavlinkMissionUploadItem {

    private float x;
    private float y;
    private float z;

    private MavCmd command;

    private float param1;
    private float param2;
    private float param3;
    private float param4;

    public MavlinkMissionUploadItem(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public MavCmd getCommand() {
        return command;
    }

    public float getParam1() {
        return param1;
    }

    public float getParam2() {
        return param2;
    }

    public float getParam3() {
        return param3;
    }

    public float getParam4() {
        return param4;
    }

    public void setBehavior(float param1, float param2) {
        command = MavCmd.MAV_CMD_USER_1;
        this.param1 = param1;
        this.param2 = param2;
    }

    public void setSensor(float param1, float param2, float param3, float param4) {
        command = MavCmd.MAV_CMD_USER_2;
        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
        this.param4 = param4;
    }
}
