package saivenky.optionpricer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HistoricalPrices {
    private String getUrl(int daysAgo) {
        Date today = new Date();
        DateFormat df = new SimpleDateFormat("MMM+d%2'C'+yyyy");

        Calendar c = Calendar.getInstance();
        c.setTime(today);
        c.add(Calendar.DATE, -daysAgo);

        String stock = "NASDAQ%3ASBUX";
        String endDate = df.format(today);
        String startDate = df.format(c.getTime());

        String urlPath = String.format(
                "http://www.google.com/finance/historical?q=%s&startdate=%s&enddate=%s&output=csv", stock, startDate, endDate);
        return urlPath;
    }

    public double[] getClosingPrices(int daysAgo) {
        try {
            String urlPath = getUrl(30);
            URL url = new URL(urlPath);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = br.readLine();
            List<Double> closingPrices = new ArrayList<>();
            while((line = br.readLine()) != null) {
                double closingPrice = getClosingPrice(line);
                closingPrices.add(closingPrice);
            }
            br.close();

            double[] prices = new double[closingPrices.size()];
            for(int i = 0; i < prices.length; i++) {
                prices[i] = closingPrices.get(i);
            }

            return prices;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new double[0];
    }

    private double getClosingPrice(String line) {
        String[] prices = line.split(",");
        return Double.parseDouble(prices[4]);
    }

    public static void main(String[] args) {

    }
}
