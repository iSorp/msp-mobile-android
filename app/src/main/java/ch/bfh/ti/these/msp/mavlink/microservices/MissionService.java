package ch.bfh.ti.these.msp.mavlink.microservices;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import ch.bfh.ti.these.msp.models.Mission;
import ch.bfh.ti.these.msp.models.WayPoint;
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

    public MissionService(MavlinkConnection connection, int systemId, int componentId, MavlinkMaster.MavlinkListener listener) {
        super(connection, systemId, componentId, listener);
    }

    /**
     * Starts an asynchronous mission upload
     * @param mission
     * @return CompletableFuture
     */
    public CompletableFuture<Boolean> uploadMission(Mission mission) throws IOException {
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
        return runAsync(new CommandIntSendService(this.connection, MAV_CMD_DO_PAUSE_CONTINUE));
    }

    /**
     * Microservice for Mission upload
     */
    private class MissionUploadService extends BaseMicroService<Boolean> {

        Mission mission;

        public MissionUploadService(MavlinkConnection connection, Mission mission) throws IOException {
            super(connection);
            this.mission = mission;
            this.state = new MissionUploadInit(this);
        }

        public Mission getMission() {
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
                        .count(getContext().getMission().getWayPoints().size())
                        .build());

                System.out.println("Mission service : send MissionCount");

                this.getContext().setState(new MissionUploadItem(this.getContext()));
                return true;
            }
        }

        public class MissionUploadItem extends ServiceState<MissionUploadService> {

            int sequence = 1;

            public MissionUploadItem(MissionUploadService context) {
                super(context);
            }

            @Override
            public void timeout() throws IOException {
                super.timeout();

                if (sequence <= 1)
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
                    if (sequence > getContext().getMission().getWayPoints().size()) {
                        throw new StateException(this, "Wrong sequence number");
                    }

                    handleMissionItem();
                }

                if (message.getPayload() instanceof MissionAck) {
                    System.out.println("Mission service : received MissionAck");

                    MavlinkMessage<MissionAck> mess = (MavlinkMessage<MissionAck>) message;

                    if (mess.getPayload().type().entry() == MavMissionResult.MAV_MISSION_ACCEPTED)
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
                WayPoint p = getContext().getMission().getWayPoints().get(sequence-1);
                getContext().send(MissionItem.builder()
                        .missionType(MavMissionType.MAV_MISSION_TYPE_MISSION)
                        .x(p.getLatitude())
                        .y(p.getLongitude())
                        .z(p.getAltitude())
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
