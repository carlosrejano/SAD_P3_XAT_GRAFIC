import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
public class Monitor {

    public Lock lock = new ReentrantLock();
    public Condition waitingToSend  = lock.newCondition();
    public String message;
    public boolean messageState;

    public Monitor() {
        waitingToSend  = lock.newCondition();
        messageState = false;
    }
    public void setMessage(String string){
        this.message = string;
    }
    public String getMessage(){
        return this.message;
    }

}
