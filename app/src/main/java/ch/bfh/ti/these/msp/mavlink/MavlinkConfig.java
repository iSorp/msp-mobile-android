package ch.bfh.ti.these.msp.mavlink;


public class MavlinkConfig {

    private final int systemId, componentId, timeout;
    private final MavlinkBridge mavlinCommWrapper;

    public MavlinkConfig(
            int systemId,
            int componentId,
            int timeout,
            MavlinkBridge mavlinCommWrapper
    ) {

        this.systemId = systemId;
        this.componentId = componentId;
        this.timeout = timeout;
        this.mavlinCommWrapper = mavlinCommWrapper;
    }

    public int getSystemId() {
        return systemId;
    }

    public int getComponentId() {
        return componentId;
    }

    public int getTimeout() {
        return timeout;
    }

    public MavlinkBridge getCommWrapper() {
        return mavlinCommWrapper;
    }

    public static class Builder {

        private int systemId = 1;
        private int componentId = 1;
        private int timeout = 5000;
        private MavlinkBridge mavlinkCommWrapper;

        public Builder(int systemId, MavlinkBridge mavlinkCommWrapper) {
            this.systemId = systemId;
            this.mavlinkCommWrapper = mavlinkCommWrapper;
        }

        public Builder setSystemId(int systemId) {
            this.systemId = systemId;
            return this;
        }

        public Builder setComponentId(int componentId) {
            this.componentId = componentId;
            return this;
        }

        public Builder setSystemId(MavlinkBridge mavlinkCommWrapper) {
            this.mavlinkCommWrapper = mavlinkCommWrapper;
            return this;
        }

        public Builder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder setCommWrapper(MavlinkBridge mavlinkCommWrapper) {
            this.mavlinkCommWrapper = mavlinkCommWrapper;
            return this;
        }

        public MavlinkConfig build() {
            return new MavlinkConfig(
                    systemId,
                    componentId,
                    timeout,
                    mavlinkCommWrapper
            );
        }

    }
}