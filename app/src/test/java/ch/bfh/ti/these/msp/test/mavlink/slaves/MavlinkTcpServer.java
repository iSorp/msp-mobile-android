package ch.bfh.ti.these.msp.test.mavlink.slaves;

import io.dronefleet.mavlink.MavlinkConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ch.bfh.ti.these.msp.util.Definitions.*;


public class MavlinkTcpServer {

    public interface MavlinkSlave {
        boolean execute(MavlinkConnection connection);
    }


    private MavlinkSlave mavlinkSlave;
    private ExecutorService executor;
    private ServerSocket server;

    public MavlinkTcpServer(MavlinkSlave mavlinkSlave) throws IOException{
        server = new ServerSocket(MAVLINK_TEST_PORT);
        this.mavlinkSlave = mavlinkSlave;
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::run);
    }

    public void stop() throws IOException{
        server.close();
        executor.shutdown();
    }

    public void run() {
        Socket client = null;
        try {

            client = server.accept();
            MavlinkConnection connection = MavlinkConnection.create(
                    client.getInputStream(),
                    client.getOutputStream());

            //ArrayList<Socket> ar = new ArrayList<>();
            //System.out.println("client accepted");
            //while (true) {
            //    new Thread(new clientThread(server.accept())).start();

            while (mavlinkSlave.execute(connection));

           // while (mavlinkSlave.execute(connection));
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
    private class clientThread implements Runnable {
        Socket client;
        public clientThread(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                MavlinkConnection connection = MavlinkConnection.create(
                        client.getInputStream(),
                        client.getOutputStream());

                while (true) {
                    mavlinkSlave.execute(connection);
                }

            } catch (IOException e) {
                // not a test case
                System.out.println(e.getMessage());
            }
        }
    }
}