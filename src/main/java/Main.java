import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.sun.tools.doclint.Messages;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    static DataBase database;

    static TelegramBot bot;

    enum RecyclingGroup {
        PAPER,
        CARDBOARD,
        ORGANIC,
        TEXTILE
    }

    public static void main(String[] args) {
        database = new DataBase();
        database.initDB();

        // Create your bot passing the token received from @BotFather
        bot = new TelegramBot("812487487:AAH6aULaTpT8wJeHWJ6vdI-HlUnvyttsBxE");

        Pattern commandPattern = Pattern.compile("/(\\w+)\\s(\\w+)");

        // Register for updates
        bot.setUpdatesListener(updates -> {
            for(Update update : updates) {
                System.out.println(update.message().text());

                long chatId = update.message().chat().id();

                Matcher matcher = commandPattern.matcher(update.message().text());
                if(matcher.matches()) {
                    switch (matcher.group(1)) {
                        case "sub":
                            SendResponse response = bot.execute(new SendMessage(chatId, subscribe(chatId, matcher.group(2))));
                            break;
                        case "location":
                            bot.execute(new SendMessage(chatId, location(chatId, matcher.group(2))));
                            break;
                        case "debugCheck":
                            sendNotifications();
                            break;
                    }
                }
            }

            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        new Scheduler();
    }

    private static String location(long chatID, String zip) {
        database.query(String.format("DELETE FROM locations WHERE chatID LIKE '%d'", chatID));
        boolean result = database.query(String.format("INSERT INTO  locations (chatID, zip) VALUES ('%d', '%d')", chatID, Integer.parseInt(zip)));
        if(result) {
            return String.format("You new location was updated to: %s Zurich", zip);
        } else {
            return "Fail!";
        }
    }

    private static String subscribe(long chatID, String group) {
        try {
            RecyclingGroup.valueOf(group.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Invalid recycling group chosen!";
        }

        boolean result = database.query(String.format("INSERT INTO  subscriptions (chatID, groupName) VALUES ('%d', '%s')", chatID, group.toUpperCase()));
        if(result) {
            return String.format("You are now subscribed to: %s", group);
        } else {
            return "Fail!";
        }
    }

    public static void sendNotifications() {
        HashMap<String, Boolean> cache = new HashMap<>();
        ErzApi api = new ErzApi();

        ResultSet subs = database.queryWithResult("SELECT subscriptions.chatID, subscriptions.groupName, locations.zip FROM subscriptions INNER JOIN locations ON subscriptions.chatID = locations.chatID");
        try {
            while(subs.next())
            {
                RecyclingGroup groupEnum = RecyclingGroup.valueOf(subs.getString("groupName"));
                int zip = subs.getInt("zip");

                System.out.println(String.format("User %d subbed %s in %d", subs.getLong("chatID"), groupEnum.name(), zip));

                boolean notify = false;
                if(cache.containsKey(groupEnum.name() + zip)) {
                    notify = cache.get(groupEnum.name() + zip);
                } else {
                    notify = api.checkNotify(groupEnum, zip);
                }

                System.out.println(String.format("%d will get notification? %b", subs.getLong("chatID"), notify));

                if(notify) {
                    bot.execute(new SendMessage(subs.getLong("chatID"), String.format("Tomorrow is: %s", groupEnum.name())));
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }

    }


}
