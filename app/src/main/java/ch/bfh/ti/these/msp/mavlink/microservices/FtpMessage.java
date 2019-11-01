package ch.bfh.ti.these.msp.mavlink.microservices;

import androidx.annotation.Size;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.FileTransferProtocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class FtpMessage {

    /**
     * @see: //mavlink.io/en/messages/common.html#FILE_TRANSFER_PROTOCOL
     */
    public final static int MESS_SIZE       = 251;
    public final static int INFO_SIZE       = 12;
    public final static int DATA_SIZE       = 239; // 251-12


    // FTP Message header indexes
    public final static int SEQ         = 0x00; // 0->1
    public final static int SESS        = 0x02;
    public final static int CODE        = 0x03;
    public final static int SIZE        = 0x04;
    public final static int REQCODE     = 0x05;
    public final static int BURST       = 0x06;
    public final static int PAD         = 0x07;
    public final static int OFFSET      = 0x08;  // 8->11
    public final static int DATA        = 0x0C;  // 12->251

    // NAK Error on data index
    public final static int NAK_EROR    = 0x00;

    // NAK Error Information
    public final static int FAIL        = 0x01;
    public final static int EOF         = 0x06;

    // OpCodes/Command
    public final static int TERM        = 0x01;
    public final static int OpenFileRO  = 0x04;
    public final static int ReadFile    = 0x05;

    public final static int ACK         = 0x80;    // 128
    public final static int NAK         = 0x81;    // 129




    private int seq, sess, code, size, reqcode, burst, pad;
    private long offset;
    private byte[] data = new byte[DATA_SIZE];
    private byte[] message = new byte[MESS_SIZE];


    private FtpMessage(int seq, int sess, int code, int size, int reqcode, int burst, int pad, long offset, byte[] data) {

        this.seq        = seq;
        this.sess       = sess;
        this.code       = code;
        this.size       = size;
        this.reqcode    = reqcode;
        this.burst      = burst;
        this.pad        = pad;
        this.offset     = offset;
        this.data       = data;

        byte[] header   = new byte[INFO_SIZE];
        header[SESS]    = (byte)this.sess;
        header[CODE]    = (byte)this.code;
        header[SIZE]    = (byte)this.size;
        header[REQCODE] = (byte)this.reqcode;
        header[BURST]   = (byte)this.burst;
        header[PAD]     = (byte)this.pad;


        // Create SEQ Byte value (size 2)
        ByteBuffer seqBuffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(seq);
        System.arraycopy(seqBuffer.array(), 0, header, SEQ, 2);

        // Create OFFSET Byte value (size 4)
        ByteBuffer ofsBuffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(offset);
        System.arraycopy(ofsBuffer.array(), 0, header, OFFSET, 4);

        // concatenate the message data array and the payload array
        System.arraycopy(header, 0, message, 0, INFO_SIZE);
        System.arraycopy(data, 0, message, INFO_SIZE, data.length);
    }

    public byte[] getMessage() {
        return message;
    }

    public int getSeq() {
        return seq;
    }

    public int getSess() {
        return sess;
    }

    public int getCode() {
        return code;
    }

    public int getSize() {
        return size;
    }

    public int getReqcode() {
        return reqcode;
    }

    public int getBurst() {
        return burst;
    }

    public int getPad() {
        return pad;
    }

    public long getOffset() {
        return offset;
    }

    public byte[] getData() {
        return data;
    }

    public static FtpMessage parse(MavlinkMessage<FileTransferProtocol> m) {

        byte[] pl = m.getPayload().payload();

        // define data array from size
        byte[] data = new byte[0xff & pl[SIZE]];
        System.arraycopy(pl, DATA, data, 0, 0xff & pl[SIZE]);

        // create offset value from 4 Bytes
        byte[] offsBinary = new byte[Long.BYTES];
        System.arraycopy(pl, DATA, offsBinary, 0, 4);
        long offs = ByteBuffer.allocate(Long.BYTES).put(offsBinary).order(ByteOrder.LITTLE_ENDIAN).getLong(0);

        return new FtpMessage.Builder()
                .setSeq(0xffff & pl[SEQ+1]*256+pl[SEQ])
                .setSess( 0xff & pl[SESS])
                .setCode(0xff & pl[CODE])
                .setSize(0xff & pl[SIZE])
                .setReqcode(0xff & pl[REQCODE])
                .setBurst(0xff & pl[BURST])
                .setPad(0xff & pl[PAD])
                .setOffset(offs)
                .setData(data)
                .build();
    }

    public static class Builder {

        private int seq, sess, code, size, reqcode, burst, pad;
        private long offset;
        private byte[] data = new byte[0];


        public Builder setSeq(int seq) {
            this.seq = seq;
            return this;
        }

        public Builder setSess(int sess) {
            this.sess = sess;
            return this;
        }

        public Builder setCode(int code) {
            this.code = code;
            return this;
        }

        public Builder setSize(int size) {
            this.size = size;
            return this;
        }

        public Builder setReqcode(int reqcode) {
            this.reqcode = reqcode;
            return this;
        }

        public Builder setBurst(int burst) {
            this.burst = burst;
            return this;
        }

        public Builder setPad(int pad) {
            this.pad = pad;
            return this;
        }

        public Builder setOffset(long offset) {
            this.offset = offset;
            return this;
        }

        public Builder setData(byte[] data) {
            this.data = data;
            return this;
        }


        public FtpMessage build() {
            return new FtpMessage(seq, sess, code, size, reqcode, burst, pad, offset, data);
        }

    }
}

