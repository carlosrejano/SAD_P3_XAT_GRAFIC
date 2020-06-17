import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import javax.swing.*;
import java.lang.Exception;
import java.awt.event.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Client {
  public static void main(String[] args) {
    Monitor mon = new Monitor();
    GUI gui = new GUI(mon,args[2]);
    gui.createAndShowGUI();

    MySocket s = new MySocket(args[1], Integer.parseInt(args[0]));
    s.println(args[2]);
    SendData send = new SendData(s,gui);
    GetData receive = new GetData(s,gui);
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
  public GUI gui;
  public int i;

  public SendData(MySocket s, GUI gui) {
    this.s = s;
    this.gui = gui;
    this.i = 0;
  }

  public void run() {
    String line = "";
    while (true) {
      try {
        gui.mon.lock.lock();
        while (!gui.mon.messageState) {
          gui.mon.waitingToSend.await();
        }
        gui.mon.messageState = false;
        gui.mon.lock.unlock();
      } catch (InterruptedException e){e.printStackTrace();}
      line = gui.mon.getMessage();

      if (line != null){
        this.s.println(line);
      }
    }
  }
}

class GetData extends Thread {
  public MySocket s;
  public GUI gui;
  public int i = 0;

  public GetData(MySocket s, GUI gui) {
    this.s = s;
    this.gui = gui;
  }

  public void run() {
    String line;
    //int i = 0;
    while ((line = s.readLine()) != null) {
      line = removeNonPrintable(line) + "\n";
      String nick = line.split(":")[0];
      gui.printMessage(line);
      gui.addUser(nick);
    }
  }

  public String removeNonPrintable(String text){
    text = text.replaceAll("[^\\x00-\\x7F]", "");
    text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
    text = text.replaceAll("\\p{C}", "");
    return text.trim();
  }
}

class GUI implements ActionListener{
  JFrame frame;
  JPanel panel;
  JButton sendbutton;
  JTextField text;
  JTextArea messages;
  JList<String> list;
  JScrollPane scrollist;
  JScrollPane scrollmes;
  GridBagConstraints gbc;
  public String linetosend;
  Monitor mon;
  String username;
  String[] users = {""};
  Timeout timeout;

  public GUI(Monitor mon, String username){
    this.mon = mon;
    frame = new JFrame("Chat");
    panel = new JPanel();
    sendbutton = new JButton("send");
    text = new JTextField();
    messages = new JTextArea();
    list = new JList<String>();
    scrollist = new JScrollPane(list);
    scrollmes = new JScrollPane(messages);
    gbc = new GridBagConstraints();
    linetosend = null;
    this.username = username;
    users[0] = username;
    timeout =  new Timeout(this);
  }

  public void createAndShowGUI(){
    try{
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }catch(Exception e){e.printStackTrace();}
    panel.setLayout(new GridBagLayout());
    frame.setDefaultLookAndFeelDecorated(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(400,400);

    list.setListData(this.users);
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridheight = 1;
    gbc.gridwidth = 10;
    gbc.weightx = 10.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    panel.add(scrollmes,gbc);
    gbc.gridx = 10;
    gbc.gridy = 0;
    gbc.gridheight = 1;
    gbc.gridwidth = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    panel.add(scrollist,gbc);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridheight = 1;
    gbc.gridwidth = 10;
    gbc.weightx = 10.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(text,gbc);
    gbc.gridx = 10;
    gbc.gridy = 1;
    gbc.gridheight = 1;
    gbc.gridwidth = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(sendbutton,gbc);

    sendbutton.addActionListener(this);
    frame.getRootPane().setDefaultButton(sendbutton);

    frame.add(panel);
    frame.setVisible(true);
    timeout.start();
  }

  public void actionPerformed(ActionEvent event){
    mon.lock.lock();
    mon.setMessage(text.getText() + "\n");
    printMessage(mon.message);
    mon.messageState = true;
    mon.waitingToSend.signal();
    text.setText("");
    mon.lock.unlock();
  }

  public void printMessage(String mes){
    messages.append(mes);
  }

  public void addUser(String user){
    int index = -1;
    int N = this.users.length;
    for(int i=0; i<N; i++){
      if (this.users[i].equals(user)){
        index = i;
      }
    }
    if(index == -1){
      this.users = Arrays.copyOf(this.users, N + 1);
      this.users[N] = user;
      list.setListData(this.users);
    }
    timeout.addTime(index);
    System.out.println(Arrays.toString(this.users));
  }

  public void removeUser(int index){
    String[] tmp = new String[users.length -1 - index];
    System.arraycopy(users, index + 1, tmp, 0, users.length -1 - index);
    System.arraycopy(tmp, 0, users, index, users.length -1 - index);
    users = Arrays.copyOf(users,users.length -1);
    System.out.println(Arrays.toString(users));
    list.setListData(this.users);
  }
}

class Timeout extends Thread{
  long[] times = {0};
  GUI gui;

  public Timeout(GUI gui){
    this.gui = gui;
  }

  public void run(){
    while(true){
      long mintime = 60000;
      long currentime = System.currentTimeMillis();
      for(int i=0; i<this.times.length; i++){
        long resta = currentime - this.times[i];
        System.out.println(resta);
        if (resta < mintime){
          System.out.println("Calculating");
          mintime = resta;
        }
        if((resta != currentime) && (resta >= 60000)){
          System.out.println("im in");
          gui.removeUser(i);
        }
      }
      try{
        System.out.println(mintime);
        sleep(mintime);
      }catch(InterruptedException e){e.printStackTrace();}
    }
  }

  public void addTime(int index){
    int N = this.times.length;
    if(index < 0){
      this.times = Arrays.copyOf(this.times, N + 1);
      this.times[N] = System.currentTimeMillis();
    }else if (index >= 0){
      this.times[index] = System.currentTimeMillis();
    }
    System.out.println(Arrays.toString(this.times));
  }
}
