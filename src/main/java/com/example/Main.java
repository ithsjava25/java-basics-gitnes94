package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0 || Arrays.asList(args).contains("--help")) {
            printHelp();
            return;
        }

        String zoneArg = null;
        String dateArg = null;
        boolean sorted = false;
        int chargingHours = 0;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone":
                    if (i + 1 < args.length) zoneArg = args[++i].toUpperCase();
                    break;
                case "--date":
                    if (i + 1 < args.length) dateArg = args[++i];
                    break;
                case "--sorted":
                    sorted = true;
                    break;
                case "--charging":
                    if (i + 1 < args.length) {
                        String val = args[++i].toLowerCase();
                        if (val.equals("2h")) chargingHours = 2;
                        else if (val.equals("4h")) chargingHours = 4;
                        else if (val.equals("8h")) chargingHours = 8;
                    }
                    break;
            }
        }

        if (zoneArg == null) {
            System.out.println("Fel: --zone är obligatoriskt (SE1|SE2|SE3|SE4)");
            return;
        }

        Prisklass zone;
        try {
            zone = Prisklass.valueOf(zoneArg);
        } catch (IllegalArgumentException e) {
            System.out.println("Fel: okänd zon. Välj SE1, SE2, SE3 eller SE4");
            return;
        }

        LocalDate date;
        if (dateArg == null) {
            date = LocalDate.now();
        } else {
            try {
                date = LocalDate.parse(dateArg, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                System.out.println("Fel: Ogiltigt datum. Använd format YYYY-MM-DD");
                return;
            }
        }

        ElpriserAPI api = new ElpriserAPI(false); // stänger cache för tydlighet i CLI
        List<Elpris> priser = api.getPriser(date, zone);

        if (priser.isEmpty()) {
            System.out.println("Inga priser hittades för " + zone + " på " + date);
            return;
        }

        System.out.println("Zon: " + zone);
        System.out.println("Datum: " + date);

        if (sorted) {
            priser.sort(Comparator.comparingDouble(Elpris::sekPerKWh));
        }

        double sum = 0;
        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;
        int minIndex = 0;
        int maxIndex = 0;

        for (int i = 0; i < priser.size(); i++) {
            Elpris e = priser.get(i);
            System.out.printf("%02d-%02d %.2f ?re%n", e.timeStart().getHour(), e.timeEnd().getHour(), e.sekPerKWh() * 100);
            sum += e.sekPerKWh();
            if (e.sekPerKWh() < minPrice) {
                minPrice = e.sekPerKWh();
                minIndex = i;
            }
            if (e.sekPerKWh() > maxPrice) {
                maxPrice = e.sekPerKWh();
                maxIndex = i;
            }
        }

        double avg = sum / priser.size();
        System.out.printf("Lägsta pris: %.4f%n", minPrice);
        System.out.printf("Högsta pris: %.4f%n", maxPrice);
        System.out.printf("Medelpris: %.4f%n", avg);

        if (chargingHours > 0 && priser.size() >= chargingHours) {
            int bestStart = 0;
            double bestAvg = Double.MAX_VALUE;
            for (int i = 0; i <= priser.size() - chargingHours; i++) {
                double windowSum = 0;
                for (int j = 0; j < chargingHours; j++) {
                    windowSum += priser.get(i + j).sekPerKWh();
                }
                double windowAvg = windowSum / chargingHours;
                if (windowAvg < bestAvg) {
                    bestAvg = windowAvg;
                    bestStart = i;
                }
            }
            Elpris start = priser.get(bestStart);
            Elpris end = priser.get(bestStart + chargingHours - 1);
            System.out.printf("Påbörja laddning: %02d:00 - %02d:00 (%.4f SEK/kWh i snitt)%n",
                    start.timeStart().getHour(),
                    end.timeEnd().getHour(),
                    bestAvg);
        }
    }

    private static void printHelp() {
        System.out.println("Usage: java -jar app.jar --zone <SE1|SE2|SE3|SE4> [--date YYYY-MM-DD] [--sorted] [--charging 2h|4h|8h]");
        System.out.println("  --zone     SE1, SE2, SE3, SE4 (required)");
        System.out.println("  --date     YYYY-MM-DD (optional, defaults to current date)");
        System.out.println("  --sorted   Display prices in ascending order (optional)");
        System.out.println("  --charging 2h|4h|8h (optional, find optimal charging window)");
        System.out.println("  --help     Show this help");
    }
}
