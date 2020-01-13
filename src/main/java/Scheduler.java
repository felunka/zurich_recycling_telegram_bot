import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Scheduler {

    public Scheduler() {
        addTask();
    }

    private void addTask() {
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.HOUR_OF_DAY, 20);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date date = cal.getTime();
        Timer timer = new Timer();

        timer.schedule(new TimerTask(){
            public void run(){
                Main.sendNotifications();
            }
        }, date, 24*60*60*1000);
    }
}
