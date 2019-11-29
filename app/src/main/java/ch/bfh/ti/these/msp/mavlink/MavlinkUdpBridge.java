package ch.bfh.ti.these.msp.mavlink;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;


public class MavlinkUdpBridge implements MavlinkBridge {

    private byte[] buffer = new byte[2048];
    private int pos = -1;
    private int length = 0;

    private boolean connected = false;

    private InetAddress ipAddress;
    private int sourcePort = 5000;
    private int targetPort = 5001;
    private String targetAddress = "127.0.0.1";
    private DatagramSocket server;
    private DatagramSocket client;


    public MavlinkUdpBridge(int sourcePort, String targetAddress, int targetPort) {
        this.sourcePort = sourcePort;
        this.targetAddress = targetAddress;
        this.targetPort = targetPort;
    }

    public void connect() throws SocketException, UnknownHostException {
        synchronized (this) {
            if (!connected) {
                ipAddress = InetAddress.getByName(targetAddress);
                server = new DatagramSocket(sourcePort);
                client = new DatagramSocket();
                connected = true;
            }
        }
    }

    public void disconnect() {
        synchronized (this) {
            connected = false;
            if (server != null){
                server.disconnect();
                server.close();
            }

            if (client != null){
                server.disconnect();
                client.close();
            }

        }
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
            if (!connected) return -1;
            int ret = -1;

            synchronized (this) {
                if (pos < 0) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        server.receive(packet);
                        length = packet.getLength();
                        if (length > 0)
                            pos = 0;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (pos >= 0 && pos < length && length > 0)
                {
                    ret = 0xff;
                    ret = ret & (buffer[pos++]);
                    //System.out.println(String.format("%02x", ret));
                }
                else{
                    pos = -1;
                }
            }
            return ret;
        }
    };

    private OutputStream os = new OutputStream() {
        @Override
        public void write(int data) throws IOException { }

        @Override
        public void write(byte b[]) throws IOException {
            if (connected){
                DatagramPacket sendPacket = new DatagramPacket(b, b.length, ipAddress, targetPort);
                client.send(sendPacket);
            }

        }
    };
}








