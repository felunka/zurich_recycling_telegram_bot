import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Scheduler {

    public Scheduler() {
        addTask();
    }

    private void addTask() {
        Date date = new Date();
        Timer timer = new Timer();

        timer.schedule(new TimerTask(){
            public void run(){
                Main.sendNotifications();
                addTask();
            }
        }, date, 24*60*60*1000);
    }
}
