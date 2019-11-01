package ch.bfh.ti.these.msp.mavlink.microservices;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.FileTransferProtocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.concurrent.CompletableFuture;

import static ch.bfh.ti.these.msp.mavlink.microservices.FtpMessage. *;


/**
 * Service for FTP functionalities
 * @see: https://mavlink.io/en/services/ftp.html
 */
public class FtpService extends BaseService {

    public FtpService(MavlinkConnection connection, int systemId, int componentId, MavlinkMaster.MavlinkListener listener) {
        super(connection, systemId, componentId, listener);
    }

    /**
     * Starts an asynchronous listing of all directories and files in a certain folder
     * @param path
     * @return
     * @throws IOException
     */
    public CompletableFuture<byte[]> listDirectory(String path) throws IOException {
        return runAsync(new ListDirectoryService(this.connection));
    }

    /**
     * Starts an asynchronous file download
     * @param filePath
     * @return file as byte[]
     * @throws IOException
     */
    public CompletableFuture<byte[]> downloadFile(String filePath) throws IOException {
        return runAsync(new FileDownloadService(this.connection, filePath));
    }

    /**
     * Starts an asynchronous file deletion
     * @param filePath
     * @return success state
     * @throws IOException
     */
    public CompletableFuture<byte[]> deletedFile(String filePath) throws IOException {
        return runAsync(new DeleteFileService(this.connection, filePath));
    }

    /**
     * ListDirectoryService
     */
    private class ListDirectoryService extends BaseMicroService<byte[]> {
        public ListDirectoryService(MavlinkConnection connection) throws IOException {
            super(connection);
        }

        // TODO ListDirectoryService states
    }

    /**
     * FileDownloadService
     */
    private class FileDownloadService extends BaseMicroService<byte[]> {

        // FTP Session handling
        private int session = 0;
        private int size    = 0;
        private String filePath = "";
        private ByteBuffer dataBuffer;


        public FileDownloadService(MavlinkConnection connection, String filePath) throws IOException {
            super(connection);
            this.filePath = filePath;
            this.state = new FileDownloadInit(this);
        }


        // region states
        public class FileDownloadInit extends ServiceState<FileDownloadService> {

            public FileDownloadInit(FileDownloadService context) { super(context); }

            @Override
            public void enter() {
                try {

                    // init session variables
                    this.getContext().session   = (byte)0;
                    this.getContext().size      = (byte)0;

                    // prepare request message
                    FtpMessage ftp = new FtpMessage.Builder()
                            .setCode(OpenFileRO)
                            .setData(this.getContext().filePath.getBytes())
                            .setSize(this.getContext().filePath.getBytes().length)
                            .build();

                    // send message: OpenFileRO( data[0]=filePath, size=len(filePath) )
                    this.getContext().send(FileTransferProtocol.builder()
                            .targetSystem(systemId)
                            .targetComponent(componentId)
                            .payload(ftp.getMessage())
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

                    FtpMessage ftp = FtpMessage.parse(message);

                    // Message received: ACK( session, size=4, data=len(file) )
                    if (ftp.getCode() == ACK && ftp.getReqcode() == OpenFileRO) {

                        // set FTP session id
                        this.getContext().session = ftp.getSess();

                        // read file size (4 Byte)
                        byte[] sizeBinary = new byte[Integer.BYTES];
                        System.arraycopy(ftp.getData(), 0, sizeBinary, 0, 4);

                        int size = ByteBuffer
                                .allocate(Long.BYTES)
                                .put(sizeBinary)
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .getInt(0);

                        this.getContext().size = size;


                        // set state for reading data chunks
                        this.getContext().setState(new FileDownloadRead(this.getContext()));
                    }
                    else if (ftp.getCode() == NAK) {
                        // TODO error handling
                    }
                }
                return true;
            }

            @Override
            public void timeout() throws IOException {
                super.timeout();

                // TODO restart initialization: OpenFileRO( data[0]=filePath, size=len(filePath) )
            }
        }

        public class FileDownloadRead extends ServiceState<FileDownloadService> {

            private long offset  = 0;

            public FileDownloadRead(FileDownloadService context) { super(context); }

            @Override
            public void enter() throws IOException {
                super.enter();

                dataBuffer = ByteBuffer.allocate(this.getContext().size);

                // send first read file request
                // send Message: ReadFile(session, size, offset)
                sendMessageReadFile(0);
            }

            @Override
            protected boolean execute() throws IOException{

                // Wait for data chunks
                if (this.getContext().message == null) return false;
                if (message.getPayload() instanceof FileTransferProtocol){

                    FtpMessage ftp = FtpMessage.parse(message);

                    // Message received: ACK(session, size=len(buffer), data[0]=buffer)
                    if (ftp.getCode() == ACK && ftp.getReqcode() == ReadFile) {

                        // add received data to the local data buffer
                        dataBuffer.put(ftp.getData());

                        // set byte offset for next chunk
                        offset = getContext().dataBuffer.position();

                        // send Message: ReadFile(session, size, offset)
                        sendMessageReadFile(offset);

                    }
                    else if (ftp.getCode() == NAK) {
                        int nakError = 0xff & ftp.getData()[NAK_EROR];

                        // NAK(session, size=1, data=EOF)
                        if (nakError == EOF)
                        {
                            // ok no more data => work done
                            this.getContext().setState(new FileDownloadEnd(this.getContext()));
                        }
                        else
                        {
                            // TODO error handling: Not Acknowledge and not end of file
                            // resend request: ReadFile(session, size, offset) ?
                        }
                    }
                }
                return true;
            }

            @Override
            public void timeout() throws IOException {
                super.timeout();

                // TODO resend request: ReadFile(session, size, offset)
            }

            private void sendMessageReadFile(long offset) throws IOException{

                // prepare request message
                FtpMessage ftp = new FtpMessage.Builder()
                        .setSess(this.getContext().session)
                        .setCode(ReadFile)
                        .setSize(DATA_SIZE)
                        .setOffset(offset)
                        .build();

                // send Message: ReadFile(session, size, offset)
                this.getContext().send(FileTransferProtocol.builder()
                        .targetSystem(systemId)
                        .targetComponent(componentId)
                        .payload(ftp.getMessage())
                        .build());
            }

        }

        public class FileDownloadEnd extends ServiceState<FileDownloadService> {
            public FileDownloadEnd(FileDownloadService context) { super(context); }

            @Override
            public void enter() throws IOException {
                try {

                    // send Message: TerminateSession(session)
                    FtpMessage ftp = new FtpMessage.Builder()
                            .setSess(this.getContext().session)
                            .setCode(TERM)
                            .build();

                    this.getContext().send(FileTransferProtocol.builder()
                            .targetSystem(systemId)
                            .targetComponent(componentId)
                            .payload(ftp.getMessage())
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
                    FtpMessage ftp = FtpMessage.parse(message);

                    if (ftp.getCode() == ACK && ftp.getReqcode() == TERM) {
                        this.getContext().exit(getContext().dataBuffer.array());
                    }
                    else {
                        // TODO error handling
                    }
                }
                return true;
            }

            @Override
            public void timeout() throws IOException {
                super.timeout();

                // TODO resend request: TerminateSession(session)
            }
        }
    }

    /**
     * DeleteFileService
     */
    private class DeleteFileService extends BaseMicroService<Boolean> {
        public DeleteFileService(MavlinkConnection connection, String filePath) throws IOException {
            super(connection);
        }

        // TODO DeleteFileService states
    }
}