package ch.bfh.ti.these.msp.mavlink.microservices;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.*;

import java.io.IOException;


abstract class CommandSendService extends BaseMicroService {

    private Object command;

    public CommandSendService(MavlinkConnection connection, Object command) throws IOException {
        super(connection);
        this.command = command;
        this.state = new CommandSend(this);
    }

    public class CommandSend extends ServiceState<CommandSendService> {

        public CommandSend(CommandSendService context) {
            super(context);
        }

        @Override
        public void enter() throws IOException {
            this.getContext().send(command);
        }

        @Override
        public boolean execute() throws IOException, StateException {

            if (message == null) return false;

            if (message.getPayload() instanceof CommandAck) {
                MavlinkMessage<CommandAck> msg = (MavlinkMessage<CommandAck>)message;

                // Command not done yet
                if (msg.getPayload().result().entry() == MavResult.MAV_RESULT_IN_PROGRESS) {

                    // Change to the progress handling state
                    this.getContext().setState(new CommandProgress(this.getContext()));
                }
                // Command successsful
                else if (msg.getPayload().result().entry() == MavResult.MAV_RESULT_ACCEPTED) {
                    // Exit current state instance
                    this.getContext().exit(true);
                }
                else {
                    throw new StateException(this, "CommandSend error: " + msg.getPayload().result().entry());
                }
            }

            return true;
        }
    }

    public class CommandProgress extends ServiceState<CommandSendService> {

        public CommandProgress(CommandSendService context) {
            super(context);
        }

        @Override
        public void enter() throws IOException {
            // TODO inform listener about 1. progress
            // msg.getPayload().progress()
        }

        @Override
        public boolean execute() throws IOException, StateException {

            if (message == null) return false;

            if (message.getPayload() instanceof CommandAck) {
                MavlinkMessage<CommandAck> msg = (MavlinkMessage<CommandAck>)message;

                // Command not done yet
                if (msg.getPayload().result().entry() == MavResult.MAV_RESULT_IN_PROGRESS) {

                    // TODO inform listener about progress
                    // msg.getPayload().progress()
                }
                // Command successsful
                else if (msg.getPayload().result().entry() == MavResult.MAV_RESULT_ACCEPTED) {
                    // Exit current state instance
                    this.getContext().exit(true);
                }
                else {
                    throw new StateException(this, "CommandProgress error: " + msg.getPayload().result().entry());
                }
            }

            return true;
        }
    }
}

class CommandIntSendService extends CommandSendService {

    public CommandIntSendService(MavlinkConnection connection, MavCmd command) throws IOException {
        super(connection, CommandInt.builder().command(command).build());
    }

    public CommandIntSendService(MavlinkConnection connection, CommandInt command) throws IOException {
        super(connection, command);
    }
}

class CommandLongSendService extends CommandSendService {

    public CommandLongSendService(MavlinkConnection connection, MavCmd command) throws IOException {
        super(connection, CommandLong.builder().command(command).build());
    }

    public CommandLongSendService(MavlinkConnection connection, CommandLong command) throws IOException {
        super(connection, command);
    }
}