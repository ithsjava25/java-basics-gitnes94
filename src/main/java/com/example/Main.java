package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        String zone = null;
        String dateStr = null;
        boolean sorted = false;
        int chargingHours = 0;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone" -> {
                    if (i + 1 < args.length) zone = args[++i];
                }
                case "--date" -> {
                    if (i + 1 < args.length) dateStr = args[++i];
                }
                case "--sorted" -> sorted = true;
                case "--charge" -> {
                    if (i + 1 < args.length) chargingHours = Integer.parseInt(args[++i]);
                }
                case "--help" -> {
                    printHelp();
                    return;
                }
            }
        }

        if (zone == null) {
            printHelp();
            return;
        }

        Prisklass prisklass;
        try {
            prisklass = Prisklass.valueOf(zone.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("fel zon");
            return;
        }

        LocalDate datum;
        if (dateStr == null) {
            datum = LocalDate.now();
        } else {
            try {
                datum = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                System.out.println("fel: ogiltigt datumformat, använd YYYY-MM-DD");
                return;
            }
        }

        ElpriserAPI api = new ElpriserAPI();
        List<Elpris> priser = api.getPriser(datum, prisklass);

        if (priser.isEmpty()) {
            System.out.println("ingen data tillgänglig för zon: " + zone + " datum: " + datum);
            return;
        }

        if (chargingHours > 0) {
            if (chargingHours > priser.size()) {
                System.out.println("fel: kan inte ladda längre än " + priser.size() + " timmar");
                return;
            }

            double minSum = Double.MAX_VALUE;
            int bestStart = 0;
            for (int i = 0; i <= priser.size() - chargingHours; i++) {
                double sum = 0;
                for (int j = 0; j < chargingHours; j++) sum += priser.get(i + j).sekPerKWh() * 100;
                if (sum < minSum) {
                    minSum = sum;
                    bestStart = i;
                }
            }

            Elpris start = priser.get(bestStart);
            Elpris end = priser.get(bestStart + chargingHours - 1);
            double avg = minSum / chargingHours;

            System.out.println("påbörja laddning: " + start.timeStart().getHour() + "-" + end.timeEnd().getHour());
            System.out.printf("Total kostnad: %.2f öre%n", minSum);
            System.out.printf("Genomsnitt: %.2f öre/kWh%n", avg);
            return;
        }

        if (sorted) {
            priser.sort(Comparator.comparingDouble(Elpris::sekPerKWh).reversed());
        }

        double min = priser.stream().mapToDouble(p -> p.sekPerKWh() * 100).min().orElse(0);
        double max = priser.stream().mapToDouble(p -> p.sekPerKWh() * 100).max().orElse(0);
        double avg = priser.stream().mapToDouble(p -> p.sekPerKWh() * 100).average().orElse(0);

        System.out.println("ElpriserAPI initialiserat. Cachning: Av");
        System.out.println("!!! ANVÄNDER MOCK-DATA FÖR TEST !!!");
        for (Elpris pris : priser) {
            int start = pris.timeStart().getHour();
            int end = pris.timeEnd().getHour();
            double ore = pris.sekPerKWh() * 100;
            System.out.printf("%02d-%02d %.2f öre%n", start, end, ore);
        }
        System.out.printf("Lägsta pris: %.2f%n", min);
        System.out.printf("Högsta pris: %.2f%n", max);
        System.out.printf("Medelpris: %.2f%n", avg);
    }

    private static void printHelp() {
        System.out.println("""
                ⚡ Electricity Price Optimizer CLI
                usage:
                  java -cp target/classes com.example.Main --zone SE3 --date 2025-09-29

                Argument:
                  --zone SE1|SE2|SE3|SE4   (obligatoriskt)
                  --date YYYY-MM-DD        (valfritt, standard = idag)
                  --sorted                 (sortera priser fallande)
                  --charge N               (hitta billigaste N timmar för laddning)
                  --help                   (visar denna hjälp)
                """);
    }
}
