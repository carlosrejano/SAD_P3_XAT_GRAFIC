import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MySocket {
    public Socket s;
    public DataInputStream dis;
    public DataOutputStream dos;
    public MySocket(Socket s){
        this.s = s;
        try {
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public MySocket(String host, int port){
        try {
            this.s = new Socket(host, port);
            dis = new DataInputStream(s.getInputStream());
            dos = new DataOutputStream(s.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void println(String str){
        try {
            dos.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String readLine(){
        try {
            return dis.readUTF();
        } catch (IOException e) {
            return e.getMessage();
        }
    }
}
