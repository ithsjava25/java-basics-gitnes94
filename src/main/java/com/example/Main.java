package com.example;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            showHelp();
            return;
        }

        String zone = null;
        int chargeHours = 0;
        boolean sorted = false;
        String date = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone" -> zone = args[++i];
                case "--charge" -> chargeHours = Integer.parseInt(args[++i]);
                case "--sorted" -> sorted = true;
                case "--date" -> date = args[++i];
                case "--help" -> {
                    showHelp();
                    return;
                }
            }
        }

        if (zone == null) {
            System.out.println("Invalid zone. Choose SE1, SE2, SE3 or SE4");
            return;
        }

        List<PriceHour> prices = getMockData();
        displayMinMax(prices);
        if (sorted) displaySorted(prices);
        if (chargeHours > 0) findOptimalCharge(prices, chargeHours);
    }

    static void showHelp() {
        System.out.println("Usage: Electricity Price Optimizer CLI");
        System.out.println("Anv?ndning:");
        System.out.println("  java -cp target/classes com.example.Main --zone SE3 --date 2025-09-29");
        System.out.println();
        System.out.println("Argument:");
        System.out.println("  --zone SE1|SE2|SE3|SE4   (obligatoriskt)");
        System.out.println("  --date YYYY-MM-DD        (valfritt, standard = idag)");
        System.out.println("  --sorted                 (sortera priser fallande)");
        System.out.println("  --charge N               (hitta billigaste N timmar f?r laddning)");
        System.out.println("  --help                   (visar denna hj?lp)");
    }

    static List<PriceHour> getMockData() {
        List<PriceHour> list = new ArrayList<>();
        list.add(new PriceHour("00-01", 50.00));
        list.add(new PriceHour("01-02", 10.00));
        list.add(new PriceHour("02-03", 5.00));
        list.add(new PriceHour("03-04", 15.00));
        list.add(new PriceHour("04-05", 8.00));
        list.add(new PriceHour("05-06", 12.00));
        list.add(new PriceHour("06-07", 6.00));
        list.add(new PriceHour("07-08", 9.00));
        list.add(new PriceHour("08-09", 25.00));
        list.add(new PriceHour("09-10", 30.00));
        list.add(new PriceHour("10-11", 35.00));
        list.add(new PriceHour("11-12", 40.00));
        list.add(new PriceHour("12-13", 45.00));
        list.add(new PriceHour("13-14", 20.00));
        list.add(new PriceHour("14-15", 15.00));
        list.add(new PriceHour("15-16", 10.00));
        list.add(new PriceHour("16-17", 5.00));
        list.add(new PriceHour("17-18", 30.00));
        list.add(new PriceHour("18-19", 35.00));
        list.add(new PriceHour("19-20", 40.00));
        list.add(new PriceHour("20-21", 20.00));
        list.add(new PriceHour("21-22", 25.00));
        list.add(new PriceHour("22-23", 30.00));
        list.add(new PriceHour("23-00", 10.00));
        return list;
    }

    static void displayMinMax(List<PriceHour> prices) {
        double min = prices.stream().mapToDouble(p -> p.price).min().orElse(0);
        double max = prices.stream().mapToDouble(p -> p.price).max().orElse(0);
        double avg = prices.stream().mapToDouble(p -> p.price).average().orElse(0);

        DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.forLanguageTag("sv-SE")));

        System.out.println("P?b?rja laddning");
        prices.forEach(p -> System.out.println(p.hour + " " + df.format(p.price) + " ?re"));
        System.out.println("L?gsta pris: " + df.format(min));
        System.out.println("H?gsta pris: " + df.format(max));
        System.out.println("Medelpris: " + df.format(avg));
    }

    static void displaySorted(List<PriceHour> prices) {
        DecimalFormat df = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.forLanguageTag("sv-SE")));
        prices.stream()
                .sorted((a, b) -> Double.compare(a.price, b.price))
                .forEach(p -> System.out.println(p.hour + " " + df.format(p.price) + " ?re"));
    }

    static void findOptimalCharge(List<PriceHour> prices, int hours) {
        PriceHour best = prices.stream().sorted((a, b) -> Double.compare(a.price, b.price)).limit(hours).toList().get(0);
        LocalTime time = LocalTime.parse(best.hour.substring(0, 2) + ":00");
        System.out.println("Kl " + String.format("%02d:%02d", time.getHour(), time.getMinute()) + " startar billigaste laddning");
    }

    static class PriceHour {
        String hour;
        double price;

        PriceHour(String hour, double price) {
            this.hour = hour;
            this.price = price;
        }
    }
}
