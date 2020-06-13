package allen.gong;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Hello world!
 *
 */
public class App 
{
    private static String error = "{\"error\":\"waiting for connecting SLD\", \"code\":500}";
    private static volatile boolean stop = false;
    private static final String htmlStr = "HTTP/1.1 500 Internal Server Error\n" +
            "Server: guard-server\n" +
            "Content-Type: application/json;charset=utf-8\n" +
            "Content-Length: "+ error.getBytes().length +"\n\n" +
            error +
            "\n";
    static ServerSocketChannel serverSocketChannel = null;
    static Selector selector = null;

    public static void startServer(int port) {
        stop = false;
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(port));
            //设置为非阻塞
            serverSocketChannel.configureBlocking(false);
            //注册接收连接的事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("服务器启动监听 " + port + " 端口...");
            //持续select事件
            constantSelect(selector);

        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            if(serverSocketChannel != null){
                try {
                    serverSocketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("guard server exit");
    }

    synchronized public static void stopServer(){
        stop = true;
        if(selector != null){
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void constantSelect(Selector selector) {
        while (!stop) {
            try {
                if (selector.select() > 0) {
                    System.out.println("当前连接数:" + selector.keys().size());
                    System.out.println("检测到活跃的连接数:" + selector.selectedKeys().size());
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> it = keys.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        if (key.isAcceptable()) {
                            //处理接收连接时间
                            boolean succ = dealAccept(selector, key);
                            if (succ)
                                it.remove();
                        }
                        if (key.isReadable()) {
                            //处理读事件
                            boolean succ = dealRead(key);
                            if (succ)
                                it.remove();
                        }

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("stop handling http call");
    }

    private static boolean dealRead(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        //判定读取一个字节,就返回固定页面
        try {
            int len = channel.read(buf);
            if (len <= 0) {
                //处理连接已经断开的情况
                return false;
            }
            ByteBuffer htmlBuf = ByteBuffer.allocate(htmlStr.getBytes().length);
            htmlBuf.put(htmlStr.getBytes());

            htmlBuf.flip();
            int outLen = channel.write(htmlBuf);


        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                channel.shutdownInput();
                channel.shutdownOutput();
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return true;
    }


    private static boolean dealAccept(Selector selector, SelectionKey key) {

        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            //接收连接后,注册读事件到selector
            socketChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void main( String[] args )
    {
        new Thread(){
            @Override
            public void run(){
                startServer(8080);
                System.out.println("stop worker thread");
            }
        }.start();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("server quiting");
        stopServer();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DemoApplication.main(args);
        System.out.println("server quit");
    }
}
