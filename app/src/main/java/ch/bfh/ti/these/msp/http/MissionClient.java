package ch.bfh.ti.these.msp.http;

import ch.bfh.ti.these.msp.models.Mission;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class MissionClient {

    private Socket socket;
    private String address;
    private int port;

    public MissionClient(String address, int port) {

    }

    public void connect() throws IOException {
        socket = new Socket(address, port);
    }

    public void disconnect() throws IOException {
        socket.close();
    }

    public ArrayList<Mission> getMissions() {

        ArrayList<Mission> missions = new ArrayList<>();
        Mission m = new Mission();
        missions.add(m);

        return missions;
    }
}
