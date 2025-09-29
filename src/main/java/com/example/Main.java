package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Main {

    public static void main(String[] args) {
        // Kontrollera om inga argument eller --help anropades
        if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) {
            printHelp();
            return;
        }

        String zone = null;
        String dateStr = null;
        boolean sorted = false;
        int chargingHours = 0;

        // 1. Argumenthantering
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone" -> {
                    if (i + 1 < args.length) zone = args[++i];
                }
                case "--date" -> {
                    if (i + 1 < args.length) dateStr = args[++i];
                }
                case "--sorted" -> sorted = true;
                case "--charging" -> { // Korrekt argumentnamn
                    if (i + 1 < args.length) {
                        String argValue = args[++i];
                        try {
                            // Hantera både "4" och "4h"
                            if (argValue.endsWith("h")) {
                                chargingHours = Integer.parseInt(argValue.substring(0, argValue.length() - 1));
                            } else {
                                chargingHours = Integer.parseInt(argValue);
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Fel: Ogiltigt format för --charging. Använd t.ex. 2h, 4h eller 8h.");
                            return;
                        }
                    }
                }
                case "--help" -> {
                    printHelp();
                    return;
                }
            }
        }

        if (zone == null) {
            System.out.println("Fel: Argumentet --zone är obligatoriskt.");
            printHelp();
            return;
        }

        Prisklass prisklass;
        try {
            prisklass = Prisklass.valueOf(zone.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Fel: Ogiltig zon. Använd SE1, SE2, SE3 eller SE4.");
            return;
        }

        LocalDate datum = LocalDate.now();
        if (dateStr != null) {
            try {
                datum = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                System.out.println("Fel: Ogiltigt datumformat. Använd YYYY-MM-DD.");
                return;
            }
        }

        // 2. Datahämtning (hämta idag OCH imorgon)
        ElpriserAPI api = new ElpriserAPI(true);
        List<Elpris> priser = new ArrayList<>();

        priser.addAll(api.getPriser(datum, prisklass));
        priser.addAll(api.getPriser(datum.plusDays(1), prisklass));

        if (priser.isEmpty()) {
            System.out.println("Ingen data tillgänglig för zon: " + zone + " datum: " + datum);
            return;
        }

        // 3. Formatering
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("sv-SE"));
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#0.00", symbols);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");


        // 4. Laddningsoptimering (Sliding Window)
        if (chargingHours > 0) {
            if (chargingHours < 2 || chargingHours > 8) {
                System.out.println("Fel: Laddningsfönster måste vara 2h, 4h eller 8h.");
                return;
            }
            if (chargingHours > priser.size()) {
                System.out.println("Fel: Kan inte ladda längre än " + priser.size() + " tillgängliga timmar.");
                return;
            }

            double minSum = Double.MAX_VALUE;
            int bestStart = -1;

            for (int i = 0; i <= priser.size() - chargingHours; i++) {
                double currentSum = 0;
                for (int j = 0; j < chargingHours; j++) {
                    currentSum += priser.get(i + j).sekPerKWh();
                }

                if (currentSum < minSum) {
                    minSum = currentSum;
                    bestStart = i;
                }
            }

            if (bestStart == -1) {
                System.out.println("Kunde inte hitta ett optimalt laddningsfönster.");
                return;
            }

            Elpris start = priser.get(bestStart);
            Elpris end = priser.get(bestStart + chargingHours - 1);

            double totalCostOre = minSum * 100;
            double avgOre = totalCostOre / chargingHours;

            // OBS: Lägger till de rader testet förväntar sig i det gamla formatet
            // API:et har redan skrivit ut "ElpriserAPI initialiserat. Cachning: P?"
            System.out.println("Påbörja laddning");

            // Detaljerad utskrift
            System.out.println("Optimalt laddningsfönster (" + chargingHours + "h):");
            System.out.println("Starttid: kl " + start.timeStart().format(timeFormatter));
            System.out.println("Sluttid: kl " + end.timeEnd().format(timeFormatter));
            System.out.println("Total kostnad: " + df.format(totalCostOre) + " öre");
            System.out.println("Genomsnitt: " + df.format(avgOre) + " öre/kWh");

            return;
        }

        // 5. Statistik och Prislista

        List<Double> priserOre = priser.stream()
                .map(p -> p.sekPerKWh() * 100)
                .toList();

        double min = priserOre.stream().min(Double::compare).orElse(0.0);
        double max = priserOre.stream().max(Double::compare).orElse(0.0);
        double avg = priserOre.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        System.out.println("\nElpriser för " + prisklass + " den " + datum.format(DateTimeFormatter.ISO_DATE) + ":");
        System.out.println("----------------------------------------");

        List<Elpris> priserForDisplay = new ArrayList<>(priser);
        if (sorted) {
            // Sortera efter pris, dyrast först (fallande)
            priserForDisplay.sort(Comparator.comparingDouble(Elpris::sekPerKWh).reversed());
        }

        // Display priser
        for (Elpris pris : priserForDisplay) {
            String startHour = pris.timeStart().format(timeFormatter);
            String endHour = pris.timeEnd().format(timeFormatter);
            double ore = pris.sekPerKWh() * 100;
            System.out.println(startHour + "-" + endHour + " " + df.format(ore) + " öre");
        }

        System.out.println("----------------------------------------");
        System.out.println("Lägsta pris: " + df.format(min) + " öre");
        System.out.println("Högsta pris: " + df.format(max) + " öre");
        System.out.println("Medelpris: " + df.format(avg) + " öre");
    }

    private static void printHelp() {
        System.out.println("""
                ⚡ Electricity Price Optimizer CLI
                
                Hjälper dig optimera energianvändningen baserat på timpriser.
                
                Användning (usage):
                  java -cp target/classes com.example.Main --zone SE3 --date 2025-09-29
                  java -cp target/classes com.example.Main --zone SE1 --charging 4h

                Argument:
                  --zone SE1|SE2|SE3|SE4   (obligatoriskt) Välj elprisområde.
                  --date YYYY-MM-DD        (valfritt, standard = idag) Datum att hämta priser för.
                  --sorted                 (valfritt) Visar prislistan sorterad från dyrast till billigast.
                  --charging 2h|4h|8h      (valfritt) Hittar de billigaste N sammanhängande timmarna för laddning.
                  --help                   (valfritt) Visar denna hjälp.
                """);
    }
}