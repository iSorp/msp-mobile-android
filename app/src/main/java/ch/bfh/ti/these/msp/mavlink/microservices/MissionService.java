package ch.bfh.ti.these.msp.mavlink.microservices;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.mavlink.model.MavlinkMission;
import ch.bfh.ti.these.msp.mavlink.model.MavlinkMissionUploadItem;
import ch.bfh.ti.these.msp.models.Mission;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static io.dronefleet.mavlink.common.MavCmd.*;

/**
 * Defines microservices for mission management, such as mission upload or mission download.
 */
public class MissionService extends BaseService {

    public MissionService(MavlinkConnection connection, int systemId, int componentId, MavlinkMaster.ServiceTask listener) {
        super(connection, systemId, componentId, listener);
    }

    /**
     * Starts an asynchronous mission upload
     * @param mission
     * @return CompletableFuture
     */
    public CompletableFuture<Boolean> uploadMission(MavlinkMission mission) throws IOException {
        return runAsync(new MissionUploadService(this.connection, mission));
    }

    /**
     * Starts an asynchronous mission clearing
     * @return CompletableFuture
     */
    public CompletableFuture<Boolean> clearMission() throws IOException {
        return runAsync(new MissionClearService(this.connection));
    }

    /**
     * Sends a mission start command
     * @return
     * @throws IOException
     */
    public CompletableFuture<Boolean> startMission() throws IOException {
        return runAsync(new CommandLongSendService(this.connection, MAV_CMD_MISSION_START));
    }

    /**
     * Sends a mission pause command
     * @return
     * @throws IOException
     */
    public CompletableFuture<Boolean> pauseMission() throws IOException {
        return runAsync(new CommandLongSendService(this.connection, MAV_CMD_DO_PAUSE_CONTINUE));
    }

    /**
     * Gets the current mission item sequence number
     * @return
     * @throws IOException
     */
    public CompletableFuture<Short> getCurrent() throws IOException {
        return runAsync(new BaseMicroService<Boolean>(this.connection, new ServiceState() {

            @Override
            public void timeout() throws IOException {
                super.timeout();
                this.getContext().send(MissionCurrent.builder().build());
            }

            @Override
            public void enter() throws IOException {
                getContext().setResult(false);
                this.getContext().send(MissionCurrent.builder().build());
            }

            @Override
            public boolean execute() {

                if (getContext().message == null) return false;

                if (getContext().message.getPayload() instanceof MissionCurrent) {
                    MavlinkMessage<MissionCurrent> msg =  (MavlinkMessage<MissionCurrent>)getContext().message;
                    this.getContext().exit((short)msg.getPayload().seq());
                }
                return true;
            }
        }));
    }

    /**
     * Microservice for Mission upload
     */
    private class MissionUploadService extends BaseMicroService<Boolean> {

        MavlinkMission mission;

        public MissionUploadService(MavlinkConnection connection, MavlinkMission mission) throws IOException {
            super(connection);
            this.mission = mission;
            this.state = new MissionUploadInit(this);
        }

        public MavlinkMission getMission() {
            return this.mission;
        }

        public class MissionUploadInit extends ServiceState<MissionUploadService> {

            public MissionUploadInit(MissionUploadService context) {
                super(context);
            }

            @Override
            public boolean execute() throws IOException {

                this.getContext().send(MissionCount.builder()
                        .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                        .targetSystem(systemId)
                        .targetComponent(componentId)
                        .count(getContext().getMission().size())
                        .build());

                System.out.println("Mission service : send MissionCount");

                this.getContext().setState(new MissionUploadItem(this.getContext()));
                return true;
            }
        }

        public class MissionUploadItem extends ServiceState<MissionUploadService> {

            int sequence = 0;

            public MissionUploadItem(MissionUploadService context) {
                super(context);
            }

            @Override
            public void timeout() throws IOException {
                super.timeout();

                if (sequence <= 0)
                {
                    // resend MissionCount request
                    this.getContext().setState(new MissionUploadInit(this.getContext()));
                }
                else {
                    // resend mission item
                    handleMissionItem();
                }
            }

            @Override
            public boolean execute() throws IOException, StateException {

                if (message == null) return false;

                if (message.getPayload() instanceof MissionRequest) {

                    MavlinkMessage<MissionRequest> mess = (MavlinkMessage<MissionRequest>) message;
                    sequence = mess.getPayload().seq();

                    System.out.println("Mission service : received MissionRequest = " + sequence);

                    // throw error on wrong sequence number
                    if (sequence > getContext().getMission().size()) {
                        throw new StateException(this, "Wrong sequence number");
                    }

                    handleMissionItem();
                }

                if (message.getPayload() instanceof MissionAck) {
                    System.out.println("Mission service : received MissionAck");

                    MavlinkMessage<MissionAck> mess = (MavlinkMessage<MissionAck>) message;

                    if (mess.getPayload().type() == null || mess.getPayload().type().entry() == MavMissionResult.MAV_MISSION_ACCEPTED)
                        this.getContext().setState(new MissionUploadEnd(this.getContext()));
                    else
                        throw new StateException(getContext().state, mess.getPayload().type().toString());
                }
                return true;
            }

            /**
             * Uploads a certain mission item depending on the received sequence number
             * @throws IOException
             */
            private void handleMissionItem() throws IOException {

                // upload next item
                MavlinkMissionUploadItem i = getContext().getMission().get(sequence);
                getContext().send(MissionItem.builder()
                        .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                        .x(i.getX())
                        .y(i.getY())
                        .z(i.getZ())
                        .command(i.getCommand())
                        .param1(i.getParam1())
                        .param2(i.getParam2())
                        .param3(i.getParam3())
                        .param4(i.getParam4())
                        .frame(MavFrame.MAV_FRAME_GLOBAL)
                        /* TODO action of the waypoint  .command(MavCmd.MAV_CMD_ACCELCAL_VEHICLE_POS)*/
                        .seq(sequence)
                        .build());

                System.out.println("Mission service : send MissionItem = " + sequence);
            }
        }

        public class MissionUploadEnd extends ServiceState<MissionUploadService> {
            public MissionUploadEnd(MissionUploadService context) {
                super(context);
            }

            @Override
            public void enter() throws IOException{
                getContext().exit(true);
                this.getContext().setState(new MissionUploadInit(this.getContext()));
            }

            @Override
            public boolean execute() {
                return true;
            }
        }

    }

    /**
     * Microservice for Mission upload
     */
    private class MissionClearService extends BaseMicroService<Boolean> {

        Mission mission;

        public MissionClearService(MavlinkConnection connection) throws IOException {
            super(connection);
            this.mission = mission;
            this.state = new MissionClear(this);
        }

        public class MissionClear extends ServiceState<MissionClearService> {

            public MissionClear(MissionClearService context) {
                super(context);
            }

            @Override
            public void enter()  throws IOException {
                this.getContext().send(MissionClearAll.builder()
                        .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                        .targetSystem(systemId)
                        .targetComponent(componentId)
                        .build());
            }

            @Override
            public boolean execute() throws IOException, StateException {
                if (message == null) return false;

                if (message.getPayload() instanceof MissionAck) {
                    System.out.println("MissionClearService: received MissionAck");

                    MavlinkMessage<MissionAck> mess = (MavlinkMessage<MissionAck>) message;
                    if (mess.getPayload().type().entry() == MavMissionResult.MAV_MISSION_ACCEPTED)
                        this.getContext().exit(true);
                    else
                        throw new StateException(getContext().state, mess.getPayload().type().toString());
                }
                return true;
            }
        }
    }

}
