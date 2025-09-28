package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("--help")) {
            System.out.println("Usage: java -jar app.jar --zone <SE1|SE2|SE3|SE4> [--date YYYY-MM-DD] [--sorted]");
            return;
        }

        String zoneArg = null;
        String dateArg = null;
        boolean sorted = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone":
                    if (i + 1 < args.length) {
                        zoneArg = args[i + 1].toUpperCase();
                        i++;
                    }
                    break;
                case "--date":
                    if (i + 1 < args.length) {
                        dateArg = args[i + 1];
                        i++;
                    }
                    break;
                case "--sorted":
                    sorted = true;
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

        ElpriserAPI api = new ElpriserAPI(false);
        List<Elpris> prices = api.getPriser(date, zone);

        if (prices.isEmpty()) {
            System.out.println("Inga priser hittades för " + zone + " på " + date);
            return;
        }

        System.out.println("Zon: " + zone);
        System.out.println("Datum: " + date);

        if (sorted) {
            prices.sort(Comparator.comparingDouble(Elpris::sekPerKWh));
        }

        double sum = 0;
        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;

        for (Elpris e : prices) {
            System.out.printf("%02d:00 - %.4f SEK/kWh%n", e.timeStart().getHour(), e.sekPerKWh());
            sum += e.sekPerKWh();
            if (e.sekPerKWh() < minPrice) minPrice = e.sekPerKWh();
            if (e.sekPerKWh() > maxPrice) maxPrice = e.sekPerKWh();
        }

        double avg = sum / prices.size();
        System.out.printf("Lägsta pris: %.4f%n", minPrice);
        System.out.printf("Högsta pris: %.4f%n", maxPrice);
        System.out.printf("Medelpris: %.4f%n", avg);

        int chargingHours = 8;
        double minAvg = Double.MAX_VALUE;
        int startHour = 0;
        for (int i = 0; i <= prices.size() - chargingHours; i++) {
            double sumPeriod = 0;
            for (int j = 0; j < chargingHours; j++) {
                sumPeriod += prices.get(i + j).sekPerKWh();
            }
            double avgPeriod = sumPeriod / chargingHours;
            if (avgPeriod < minAvg) {
                minAvg = avgPeriod;
                startHour = i;
            }
        }
        System.out.printf("Kl %02d:00 - %02d:00 (%.4f SEK/kWh i snitt)%n",
                prices.get(startHour).timeStart().getHour(),
                prices.get(startHour + chargingHours - 1).timeEnd().getHour(),
                minAvg);
    }
}
