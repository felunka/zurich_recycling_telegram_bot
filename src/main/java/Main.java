import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

    static DataBase database;

    static TelegramBot bot;

    enum RecyclingGroup {
        WASTE,
        PAPER,
        CARDBOARD,
        CARGOTRAM,
        ORGANIC,
        TEXTILE
    }

    public static void main(String[] args) {
        database = new DataBase();
        database.initDB();

        // Create your bot passing the token received from @BotFather
        bot = new TelegramBot(getApiKey());

        HashMap<Long, String> session = new HashMap<Long, String>();

        Pattern commandPattern = Pattern.compile("/(\\w+)");

        // Register for updates
        bot.setUpdatesListener(updates -> {
            for(Update update : updates) {
                if(update.message() == null) {
                    continue;
                }

                System.out.println(update.message().text());

                long chatId = update.message().chat().id();

                if(update.message().text() == null) {
                    bot.execute(new SendMessage(chatId, "Sorry, I can only handle text... :("));
                    continue;
                }

                Matcher matcher = commandPattern.matcher(update.message().text());
                if(matcher.matches()) {
                    switch (matcher.group(1)) {
                        case "sub":
                            session.put(chatId, matcher.group(1));
                            bot.execute(new SendMessage(chatId, "Tell me, what you want to subscribe to. Use /list to see all possibilities."));
                            break;
                        case "unsub":
                            session.put(chatId, matcher.group(1));
                            bot.execute(new SendMessage(chatId, "What group do you want to unsubscribe?"));
                            break;
                        case "mysubs":
                            bot.execute(new SendMessage(chatId, showSubs(chatId)));
                            break;
                        case "location":
                            session.put(chatId, matcher.group(1));
                            bot.execute(new SendMessage(chatId, "Tell me you zip code."));
                            break;
                        case "list":
                            String list = Arrays.stream(RecyclingGroup.values()).map(t -> t.name().toLowerCase()).collect(Collectors.joining(", "));
                            bot.execute(new SendMessage(chatId, String.format("You can subscribe to: %s.", list)));
                            break;
                        case "debugCheck":
                            sendNotifications();
                            break;
                    }
                } else {
                    if(session.containsKey(chatId)) {
                        switch (session.get(chatId)) {
                            case "sub":
                                bot.execute(new SendMessage(chatId, subscribe(chatId, update.message().text())));
                                break;
                            case "unsub":
                                bot.execute(new SendMessage(chatId, unsubscribe(chatId, update.message().text())));
                                break;
                            case "location":
                                bot.execute(new SendMessage(chatId, location(chatId, update.message().text())));
                        }
                        session.remove(chatId);
                    }
                }
            }

            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        new Scheduler();
    }

    private static String showSubs(long chatId) {
        StringBuilder sb = new StringBuilder();

        ResultSet subs = database.queryWithResult(String.format("SELECT subscriptions.groupName FROM subscriptions WHERE chatID LIKE '%d'", chatId));
        try {
            while(subs.next())
            {
                RecyclingGroup groupEnum = RecyclingGroup.valueOf(subs.getString("groupName"));

                if(sb.length() != 0)
                    sb.append(", ");

                sb.append(groupEnum.name().toLowerCase());
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return String.format("You are subscribed to: %s", sb.toString());
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

    private static String unsubscribe(long chatID, String group) {
        try {
            RecyclingGroup.valueOf(group.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Invalid recycling group chosen!";
        }

        boolean result = database.query(String.format("DELETE FROM subscriptions WHERE groupName LIKE '%s' AND chatID LIKE '%d'", group, chatID));
        if(result) {
            return String.format("You are now on your own, so don't blame me if you forget to bring out %s...", group);
        } else {
            return "Fail!";
        }
    }

    private static String getApiKey() {
        try (InputStream input = new FileInputStream("config.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            return prop.getProperty("apikey");

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return "error";
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
                    cache.put(groupEnum.name() + zip, notify);
                }

                System.out.println(String.format("%d will get notification? %b", subs.getLong("chatID"), notify));

                if(notify) {
                    bot.execute(new SendMessage(subs.getLong("chatID"), String.format("Quick reminder! %s will be picked up tomorrow.", groupEnum.name())));
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }
}
