package ch.bfh.ti.these.msp.mavlink.microservices;

import ch.bfh.ti.these.msp.mavlink.MavlinkMaster;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.common.FileTransferProtocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static ch.bfh.ti.these.msp.mavlink.microservices.FtpMessage. *;


/**
 * Service for FTP functionalities
 * @see: https://mavlink.io/en/services/ftp.html
 */
public class FtpService extends BaseService {

    public FtpService(MavlinkConnection connection, int systemId, int componentId, MavlinkMaster.ServiceTask listener) {
        super(connection, systemId, componentId, listener);
    }

    /**
     * Starts an asynchronous listing of all directories and files in a certain folder
     * @param path
     * @return
     * @throws IOException
     */
    public <T> CompletableFuture<T> listDirectory(String path) throws IOException {
        return runAsync(new ListDirectoryService(this.connection, path));
    }

    /**
     * Starts an asynchronous file download
     * @param filePath
     * @return file as byte[]
     * @throws IOException
     */
    public <T> CompletableFuture<T> downloadFile(String filePath) throws IOException {
        return runAsync(new FileDownloadService(this.connection, filePath));
    }

    /**
     * Starts an asynchronous file deletion
     * @param filePath
     * @return success state
     * @throws IOException
     */
    public <T> CompletableFuture<T> deletedFile(String filePath) throws IOException {
        return runAsync(new DeleteFileService(this.connection, filePath));
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

                    // prepare FTP message
                    FtpMessage ftp = new FtpMessage.Builder()
                            .setSeq(0)
                            .setCode(OpenFileRO)
                            .setData(this.getContext().filePath.getBytes())
                            .setSize(this.getContext().filePath.getBytes().length)
                            .build();

                    // send message: OpenFileRO( data[0]=path, size=len(path) )
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
            public boolean execute() throws IOException, StateException {

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
                        throw new StateException(this, "NAK received: " + ftp.getNakEror());
                    }
                }
                return true;
            }

            @Override
            public void timeout() throws IOException {
                super.timeout();

                // restart initialization: OpenFileRO( data[0]=path, size=len(path) )
                this.getContext().setState(new FileDownloadInit(this.getContext()));
            }
        }

        public class FileDownloadRead extends ServiceState<FileDownloadService> {

            private long offset  = 0;
            private int seq  = 0;

            public FileDownloadRead(FileDownloadService context) { super(context); }

            @Override
            public void timeout() throws IOException {
                super.timeout();

                // send Message: ReadFile(session, size, offset)
                sendMessageReadFile(offset, seq);
            }

            @Override
            public void enter() throws IOException {
                super.enter();

                dataBuffer = ByteBuffer.allocate(this.getContext().size);

                // send Message: ReadFile(session, size, offset)
                // sequence number starts with 0
                sendMessageReadFile(0, seq);
            }

            @Override
            public boolean execute() throws IOException, StateException{

                // Wait for data chunks
                if (this.getContext().message == null) return false;
                if (message.getPayload() instanceof FileTransferProtocol) {

                    FtpMessage ftp = FtpMessage.parse(message);

                    // Message received: ACK(session, size=len(buffer), data[0]=buffer)
                    if (ftp.getCode() == ACK && ftp.getReqcode() == ReadFile) {

                        // Only accept messages with the previous sent number
                        if (ftp.getSeq() == seq) {

                            ++seq;

                            // add received data to the local data buffer
                            dataBuffer.put(ftp.getData());

                            // set byte offset for next chunk
                            offset = getContext().dataBuffer.position();

                            // send Message: ReadFile(session, size, offset)
                            sendMessageReadFile(offset, seq);
                        }
                        else {
                            // resend send Message: ReadFile(session, size, offset)
                            sendMessageReadFile(offset, seq);
                        }
                    }
                    else if (ftp.getCode() == NAK) {

                        // NAK(session, size=1, data=EOF)
                        if (ftp.getNakEror() == EOF)
                        {
                            // ok no more data => work done
                            this.getContext().setState(new FileDownloadEnd(this.getContext()));
                        }
                        else
                        {
                            throw new StateException(this, "NAK received: " + ftp.getNakEror());
                        }
                    }
                }
                return true;
            }

            private void sendMessageReadFile(long offset, int seq) throws IOException{

                // prepare request message
                FtpMessage ftp = new FtpMessage.Builder()
                        .setSeq(seq)
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
            public void timeout() throws IOException {
                super.timeout();

                // send Message: TerminateSession(session)
                sendMessageTerminate();
            }

            @Override
            public void enter() throws IOException {
                // send Message: TerminateSession(session)
                sendMessageTerminate();
            }

            @Override
            public boolean execute() throws IOException, InterruptedException, StateException {

                // Message received: ACK( )
                if (this.getContext().message == null) return false;
                if (message.getPayload() instanceof FileTransferProtocol) {
                    FtpMessage ftp = FtpMessage.parse(message);

                    if (ftp.getCode() == ACK && ftp.getReqcode() == TERM) {
                        this.getContext().exit(getContext().dataBuffer.array());
                    }
                    else {
                        throw new StateException(this, "NAK received: " + ftp.getNakEror());
                    }
                }
                return true;
            }

            private void sendMessageTerminate() throws IOException{

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

    /**
     * ListDirectoryService
     */
    private class ListDirectoryService extends BaseMicroService<String> {

        // FTP Session handling
        private int offset = 0;
        private String path = "";
        private StringBuilder stringBuilder = new StringBuilder();


        public ListDirectoryService(MavlinkConnection connection, String path) throws IOException {
            super(connection);
            this.path = path;
            this.state = new ListDirectoryRead(this);
        }

        public class ListDirectoryRead extends ServiceState<ListDirectoryService> {

            public ListDirectoryRead(ListDirectoryService context) { super(context); }

            @Override
            public void timeout() throws IOException {
                super.timeout();

                // send Message: ReadFile(session, size, offset)
                sendMessageListDirectory(offset);
            }

            @Override
            public void enter() throws IOException {
                super.enter();

                offset = 0;

                // send Message: ListDirectory( data[0]=path, size=len(path), offset=0 )
                sendMessageListDirectory(offset);
            }

            @Override
            public boolean execute() throws IOException, StateException {

                // wait for Response
                if (this.getContext().message == null) return false;
                if (message.getPayload() instanceof FileTransferProtocol) {

                    FtpMessage ftp = FtpMessage.parse(message);

                    // Message received: ACK(size, data=entries_at_offset_...)
                    if (ftp.getCode() == ACK) {

                        byte[] buf = new byte[ftp.getSize()];
                        System.arraycopy(ftp.getData(), 0, buf, 0, ftp.getSize());
                        String str = new String(buf, StandardCharsets.UTF_8);
                        stringBuilder.append(str);

                        ++offset;

                        sendMessageListDirectory(offset);

                    } else if (ftp.getCode() == NAK) {
                        // NACK(size=1, data[0]=EOF)
                        if (ftp.getNakEror() == EOF) {
                            // ok no more data => work done
                            this.getContext().exit(stringBuilder.toString());
                        } else {
                            throw new StateException(this, "NAK received: " + ftp.getNakEror());
                        }
                    }
                }
                return true;
            }

            private void sendMessageListDirectory(long offset) throws IOException{

                try {
                    // prepare FTP message
                    FtpMessage ftp = new FtpMessage.Builder()
                            .setOffset(offset)
                            .setCode(ListDirectory)
                            .setData(this.getContext().path.getBytes())
                            .setSize(this.getContext().path.getBytes().length)
                            .build();

                    // send message: ListDirectory( data[0]=path, size=len(path), offset=0 )
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
        }
    }
}