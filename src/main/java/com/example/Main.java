package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

        // DecimalFormat för svenska format med komma
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("sv-SE"));
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#0.00", symbols);

        // Tidsformat: HH för min/max/medel, HH:mm för laddningsfönster
        DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

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

            // HH:mm för laddningsfönster
            System.out.println("påbörja laddning: " +
                    start.timeStart().format(timeFormatter) + "-" +
                    end.timeEnd().format(timeFormatter));
            System.out.println("Total kostnad: " + df.format(minSum) + " öre");
            System.out.println("Genomsnitt: " + df.format(avg) + " öre/kWh");
            return;
        }

        if (sorted) {
            priser.sort(Comparator.comparingDouble(Elpris::sekPerKWh).reversed());
        }

        double min = priser.stream().mapToDouble(p -> p.sekPerKWh() * 100).min().orElse(0);
        double max = priser.stream().mapToDouble(p -> p.sekPerKWh() * 100).max().orElse(0);
        double avg = priser.stream().mapToDouble(p -> p.sekPerKWh() * 100).average().orElse(0);

        System.out.println("ElpriserAPI initialiserat. Cachning: Av");
        System.out.println("Påbörja laddning");

        // HH-HH för display av priser
        for (Elpris pris : priser) {
            String startHour = pris.timeStart().format(hourFormatter);
            String endHour = pris.timeEnd().format(hourFormatter);
            double ore = pris.sekPerKWh() * 100;
            System.out.println(startHour + "-" + endHour + " " + df.format(ore) + " öre");
        }
        System.out.println("Lägsta pris: " + df.format(min));
        System.out.println("Högsta pris: " + df.format(max));
        System.out.println("Medelpris: " + df.format(avg));
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
                  --charging               (hitta billigaste N timmar för laddning)
                  --help                   (visar denna hjälp)
                """);
    }
}
