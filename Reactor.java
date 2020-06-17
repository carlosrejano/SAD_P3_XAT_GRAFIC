import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Reactor extends Thread {
    final Selector selector;
    final ServerSocketChannel serverChannel;
    static final int WORKER_POOL_SIZE = 10;
    static ExecutorService workerPool;
    Map<String,SocketChannel> myMap = new ConcurrentHashMap<String,SocketChannel>();

    Reactor(int port) throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        SelectionKey sk = serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        sk.attach(new Acceptor());
    }

    public void run() {
        try {
            while (true) {

                selector.select();
                Iterator it = selector.selectedKeys().iterator();

                while (it.hasNext()) {
                    SelectionKey sk = (SelectionKey) it.next();
                    it.remove();
                    Runnable r = (Runnable) sk.attachment();
                    if (r != null)
                        r.run();
                }
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    class Acceptor implements Runnable {
        Map<String,SocketChannel> myMap = new ConcurrentHashMap<String,SocketChannel>();

        public void run() {
            try {
                int nbytes = 0;
                ByteBuffer nameBuffer = ByteBuffer.allocate(1024);
                SocketChannel channel = serverChannel.accept();
                nbytes = channel.read(nameBuffer);
                String name = bytesBufferToString(nameBuffer);
                if (channel != null && nbytes != -1)
                    if (myMap.containsKey(name)){
                        myMap.remove(name);
                    }
                    myMap.put(name, channel);
                    new Handler(selector, channel, name, myMap);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        public String bytesBufferToString(ByteBuffer buffer){
            byte[] bytes;
            buffer.flip();
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes, 0, bytes.length);
            return new String(bytes, Charset.forName("ISO-8859-1"));
        }
    }

    public static void main(String[] args) {
        workerPool = Executors.newFixedThreadPool(WORKER_POOL_SIZE);

        try {
            new Thread(new Reactor(Integer.parseInt(args[0]))).start();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
