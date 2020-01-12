import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ErzApi {
    private final String API_URL = "http://openerz.metaodi.ch/api/";
    private final String USER_AGENT = "Mozilla/5.0";

    public ErzApi() {

    }

    // HTTP GET request
    private String sendGet(String endpoint) throws Exception {
        URL obj = new URL(API_URL + endpoint);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");

        // add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = con.getResponseCode();
        System.out.println("Sending 'GET'");
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        System.out.println(response.toString());

        // return result
        return response.toString();
    }

    private String getTomorrowDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date date = calendar.getTime();
        return dateFormat.format(date);
    }

    public boolean queryEndpoint(String group, int zip) {
        String date = getTomorrowDate();
        String endpoint = String.format("calendar/%s.json?zip=%d&start=%s&end=%s&offset=0&limit=0", group, zip, date, date);
        String response = "";
        try {
            response = sendGet(endpoint);
        } catch(Exception e) {
            e.printStackTrace();
        }
        if(response.contains("{\"total_count\":1}")) {
            return true;
        }
        return false;
    }

    public boolean checkNotify(Main.RecyclingGroup groupEnum, int zip) {
        return queryEndpoint(groupEnum.name().toLowerCase(), zip);
    }
}
