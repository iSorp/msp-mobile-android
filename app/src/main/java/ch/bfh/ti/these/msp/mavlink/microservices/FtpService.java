package ch.bfh.ti.these.msp.mavlink.microservices;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;
import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.FileTransferProtocol;
import org.bouncycastle.util.Arrays;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Service for FTP functionalities
 * @see: https://mavlink.io/en/services/ftp.html
 */
public class FtpService extends BaseService {

    /**
     * The byte count of a FileTransferProtocol without payload
     * @see: //mavlink.io/en/messages/common.html#FILE_TRANSFER_PROTOCOL
     */
    public final static int INFO_LENGTH     = 12;
    public final static int MAX_DATA_SIZE   = 239; // 251-12

    // FTP Message header indexes
    public final static int SEQ         = 0x00; // 0->1
    public final static int SESS        = 0x02;
    public final static int CODE        = 0x03;
    public final static int SIZE        = 0x04;
    public final static int REQCODE     = 0x05;
    public final static int BURST       = 0x06;
    public final static int PAD         = 0x07;
    public final static int OFFSET      = 0x08;  // 8->11
    public final static int DATA        = 0x0B;  // 12->251


    // NAK Error on payload index
    public final static int NAK_EROR = 0x00;

    // NAK Error Information payload data[0]
    public final static int EOF = 0x06;

    // OpCodes/Command
    public final static int TERM    = 0x01;
    public final static int ACK     = 0x80;    // 128
    public final static int NAK     = 0x81;    // 129


    public FtpService(MavlinkConnection connection, int systemId, int componentId, MavlinkMaster.MavlinkListener listener) {
        super(connection, systemId, componentId, listener);
    }

    /***
     * Starts an asynchronous file download
     * @param path
     * @return
     * @throws IOException
     */
    public CompletableFuture<byte[]> downloadFile(String path) throws IOException {
        return runAsync(new FileDownloadService(this.connection, path));
    }

    /**
     * FileDownloadService
     */
    public class FileDownloadService extends BaseMicroService<byte[]> {

        // FTP Session handling
        private int session = 0x00;
        private int size    = 0x00;
        private int offset  = 0x00;

        private String path = "";
        private byte[] file;

        public String getPath() {
            return this.path;
        }
        public void setSession(byte session) {
            this.session = session;
        }
        public int getSession() {
            return this.session;
        }
        public void setSize(int size){
            this.size = size;
        }
        public int getSize() {
            return this.size;
        }
        public void setOffset(int offset){
            this.offset = offset;
        }
        public int getOffset() {
            return this.offset;
        }

        public FileDownloadService(MavlinkConnection connection, String path) throws IOException {
            super(connection);
            this.path = path;
            this.state = new FileDownloadInit(this);
        }



        // region states

        public class FileDownloadInit extends ServiceState<FileDownloadService> {

            public FileDownloadInit(FileDownloadService context) { super(context); }

            @Override
            public void enter() {
                try {

                    byte[] path     = this.getContext().getPath().getBytes();
                    byte[] data     = new byte[INFO_LENGTH+path.length];
                    byte[] header   = new byte[INFO_LENGTH];

                    header[SIZE] = (byte)this.getContext().getPath().getBytes().length;

                    // concatenate the message data array and the payload array
                    //System.arraycopy(header, 0, data, 0, header.length);
                    //System.arraycopy(path, 0, data, INFO_LENGTH, path.length);

                    // Message send: OpenFileRO( data[0]=path, size=len(path) )
                    this.getContext().send(FileTransferProtocol.builder()
                            .targetSystem(systemId)
                            .targetComponent(componentId)
                            .build());

                } catch (IOException e) {
                    // TODO error handling
                    e.printStackTrace();
                }
            }

            @Override
            protected boolean execute() throws IOException{

                // wait for Response
                if (this.getContext().message == null) return false;
                if (message.getPayload() instanceof FileTransferProtocol) {

                    MavlinkMessage<FileTransferProtocol> mess = message;
                    byte code = mess.getRawBytes()[CODE];

                    // Message received: ACK( session, size=4, data=len(file) )
                    if (code == ACK) {

                        // FTP session id
                        this.getContext().setSession(mess.getRawBytes()[SESS]);

                        // size of requested file
                        ByteBuffer wrapped = ByteBuffer.wrap(mess.getPayload().payload());
                        this.getContext().setSize(wrapped.getInt());

                        // set state for reading chunks
                        this.getContext().setState(new FileDownloadRead(this.getContext()));
                    }
                    else if (code == NAK) {
                        // TODO error handling
                    }
                }
                return true;
            }
        }

        public class FileDownloadRead extends ServiceState<FileDownloadService> {

            private ByteBuffer recData;

            public FileDownloadRead(FileDownloadService context) { super(context); }

            @Override
            protected boolean execute() {
                try
                {
                    // Wait for file chunks
                    if (this.getContext().message == null) return false;
                    if (message.getPayload() instanceof FileTransferProtocol){
                        MavlinkMessage<FileTransferProtocol> mess = message;
                        int code = mess.getRawBytes()[CODE];

                        // Message received: ACK(session, size=len(buffer), data[0]=buffer)
                        if (code == ACK) {

                            // add received data to the local file buffer
                            this.recData.put(mess.getPayload().payload());

                            // set byte offset for next chunk
                            this.getContext().setOffset(this.recData.array().length-1);

                            // next chunk request
                            byte[] data = new byte[INFO_LENGTH];
                            data[SESS]      = (byte)this.getContext().getSession();
                            data[SIZE]      = (byte)MAX_DATA_SIZE;
                            data[OFFSET]    = (byte)this.getContext().getOffset();

                            // send Message: ReadFile(session, size, offset)
                            this.getContext().send(FileTransferProtocol.builder()
                                    .targetSystem(systemId)
                                    .targetComponent(componentId)
                                    .payload(data)
                                    .build());
                        }
                        else if (code == NAK) {
                            int nakError = mess.getPayload().payload()[NAK_EROR];

                            // NAK(session, size=1, data=EOF)
                            if (nakError == EOF)
                            {
                                // set the final file byte array for result
                                this.getContext().file = this.recData.array();

                                // ok no more data => work done
                                this.getContext().setState(new FileDownloadEnd(this.getContext()));
                            }
                            else
                            {
                                // TODO error handling
                            }
                        }
                    }

                } catch (IOException e) {
                    // TODO error handling
                }
                return true;
            }

        }

        public class FileDownloadEnd extends ServiceState<FileDownloadService> {
            public FileDownloadEnd(FileDownloadService context) { super(context); }

            @Override
            public void enter() throws IOException {
                try {

                    // send Message: TerminateSession(session)
                    byte[] data = new byte[INFO_LENGTH];
                    data[SESS]      = (byte)this.getContext().getSession();
                    data[CODE]      = (byte)TERM;

                    this.getContext().send(FileTransferProtocol.builder()
                            .targetSystem(systemId)
                            .targetComponent(componentId)
                            .payload(data)
                            .build());

                } catch (IOException e) {
                    // TODO error handling
                }
            }

            @Override
            protected boolean execute() throws IOException, InterruptedException, StateException {

                // Message received: ACK( )
                if (this.getContext().message == null) return false;
                if (message.getPayload() instanceof FileTransferProtocol) {
                    MavlinkMessage<FileTransferProtocol> mess = message;
                    int code = mess.getRawBytes()[CODE];
                    int nakError = mess.getPayload().payload()[CODE];
                    if (code == ACK) {
                        this.getContext().exit(file);
                    }
                    else {
                        // TODO error handling
                    }
                }

                return true;
            }


        }

    }
}