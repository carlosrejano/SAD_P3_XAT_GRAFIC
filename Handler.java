import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;


class Handler implements Runnable {
    final SocketChannel channel;
    final SelectionKey selKey;

    static final int READ_BUF_SIZE = 1024;
    static final int WRiTE_BUF_SIZE = 1024;
    ByteBuffer readBuf = ByteBuffer.allocate(READ_BUF_SIZE);
    ByteBuffer writeBuf = ByteBuffer.allocate(WRiTE_BUF_SIZE);
    Map<String,SocketChannel> myMap;
    String nickname;
    Handler(Selector sel, SocketChannel sc, String name, Map<String, SocketChannel> myMap) throws IOException {
        channel = sc;
        channel.configureBlocking(false);
        nickname = name;
        this.myMap = myMap;
        selKey = channel.register(sel, SelectionKey.OP_READ);
        selKey.attach(this);
        selKey.interestOps(SelectionKey.OP_READ);
        sel.wakeup();
    }

    public void run() {
        try {
            if (selKey.isReadable())
                read();
            else if (selKey.isWritable())
                write();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    synchronized void process() {
        byte[] bytes;
        byte[] byteNick = (nickname + ": ").getBytes();
        readBuf.flip();
        bytes = new byte[readBuf.remaining()];
        readBuf.get(bytes, 0, bytes.length);
        System.out.println(bytes);
        byte[] c = new byte[byteNick.length + bytes.length];
        System.arraycopy(byteNick, 0, c, 0, byteNick.length);
        System.arraycopy(bytes, 0, c, byteNick.length, bytes.length);
        String stringToSend = new String(c).replaceAll("\n", "").replaceAll("\r", "");
        System.out.print("process(): " + new String(bytes, Charset.forName("ISO-8859-1")));
        System.out.println(stringToSend);
        byte[] bytesToSend = stringToSend.getBytes();
        writeBuf = ByteBuffer.wrap(bytesToSend);

        selKey.interestOps(SelectionKey.OP_WRITE);
        selKey.selector().wakeup();
    }

    synchronized void read() throws IOException {
        int numBytes;

        try {
            numBytes = channel.read(readBuf);
            System.out.println("read(): #bytes read into 'readBuf' buffer = " + numBytes);

            if (numBytes == -1) {
                selKey.cancel();
                channel.close();
                System.out.println("read(): client connection might have been dropped!");
            }
            else {
                Reactor.workerPool.execute(new Runnable() {
                    public void run() {
                        process();
                    }
                });
            }
        }
        catch (IOException ex) {
            channel.close();
            ex.printStackTrace();
            return;
        }
    }

    void write() throws IOException {
        int numBytes = 0;

        try {
            for (String key : myMap.keySet()) {
                System.out.println(key);
                if(key != nickname){
                    System.out.println(nickname + ":" + key);
                    numBytes += myMap.get(key).write(writeBuf);
                }
            }
            if (numBytes > 0) {
                readBuf.clear();
                writeBuf.clear();
                selKey.interestOps(SelectionKey.OP_READ);
                selKey.selector().wakeup();
            }
        }
        catch (IOException ex) {
            channel.close();
            ex.printStackTrace();
        }
    }
}