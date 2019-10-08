package ch.bfh.ti.these.msp;

import io.dronefleet.mavlink.MavlinkConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

interface MavlinkSlave {
    boolean execute(MavlinkConnection connection);
}

class MavlinkTcpServer {

    private MavlinkSlave mavlinkSlave;

    public MavlinkTcpServer(MavlinkSlave mavlinkSlave) {
        this.mavlinkSlave = mavlinkSlave;
        Executors.newSingleThreadExecutor().submit(this::run);
    }

    public void run() {
        ServerSocket server = null;
        Socket client = null;
        try {
            server = new ServerSocket(5001);

            client = server.accept();
            MavlinkConnection connection = MavlinkConnection.create(
                    client.getInputStream(),
                    client.getOutputStream());
            while (mavlinkSlave.execute(connection));
        } catch (IOException eio) {
            // not a test case
            System.out.println(eio.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                // not a test case
                System.out.println(e.getMessage());
            }
        }
    }
}