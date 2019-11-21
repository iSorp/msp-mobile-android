package ch.bfh.ti.these.msp.mavlink;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.LinkedList;

import static ch.bfh.ti.these.msp.util.Definitions.MAVLINK_TEST_HOST;

public class MavlinkUdpBridge implements MavlinkBridge {

    private byte[] buffer = new byte[2048];
    private int pos = -1;
    private int length = 0;

    private InetAddress ipAddress;
    private int sourcePort = 5000;
    private int targetPort = 5001;
    private DatagramSocket server;
    private DatagramSocket client;

    public MavlinkUdpBridge() { }

    public void connect() throws SocketException, UnknownHostException {

        ipAddress = InetAddress.getByName(MAVLINK_TEST_HOST);
        server = new DatagramSocket(sourcePort);
        client = new DatagramSocket();
        //server.setSoTimeout(10);
    }

    public void disconnect() {
        server.close();
        client.close();
    }


    @Override
    public InputStream getInputStream() {
        return is;
    }

    @Override
    public OutputStream getOutputStream() {
        return os;
    }

    private InputStream is = new InputStream() {
        @Override
        public int read() throws IOException {
            int ret = -1;
            if (pos < 0) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    server.receive(packet);
                    length = packet.getLength();
                    if (length > 0)
                        pos = 0;
                } catch (IOException e) { }
            }


            if (pos < length && length > 0)
            {
                ret = 0xff;
                ret = ret & (buffer[pos++]);
                //System.out.println(String.format("%02x", ret));
            }
            else{
                pos = -1;
            }
            return ret;
        }
    };

    private OutputStream os = new OutputStream() {
        @Override
        public void write(int data) throws IOException { }

        @Override
        public void write(byte b[]) throws IOException {
            DatagramPacket sendPacket = new DatagramPacket(b, b.length, ipAddress, targetPort);
            client.send(sendPacket);
        }
    };
}








