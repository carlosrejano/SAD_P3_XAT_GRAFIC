import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client {

    public static void main(String[] args) {

        MySocket s = new MySocket(args[1], Integer.parseInt(args[0]));
        s.println(args[2]);
        SendData send = new SendData(s);
        GetData receive = new GetData(s);
        send.start();
        receive.start();
        try {
            send.join();
            receive.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}

class SendData extends Thread {
    public MySocket s;

    public SendData(MySocket s) {
        this.s = s;
    }

    public void run() {
        String line = "";
        BufferedReader bc = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                if (((line = bc.readLine()) == null)) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.s.println(line);
        }
    }
}

class GetData extends Thread {
    public MySocket s;

    public GetData(MySocket s) {
        this.s = s;
    }

    public void run() {
        String line;
        int i = 0;
        while ((line = s.readLine()) != null) {
            if (i%2 == 0){
                System.out.print(line + ": ");
            }else {
                System.out.println(line);
            }
            i++;

        }
    }
}
