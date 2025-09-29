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
        String zone = null;
        String dateStr = null;
        boolean sorted = false;
        int chargingHours = 0;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone" -> {
                    if (i + 1 < args.length) {
                        zone = args[++i];
                    }
                }
                case "--date" -> {
                    if (i + 1 < args.length) {
                        dateStr = args[++i];
                    }
                }
                case "--sorted" -> sorted = true;
                case "--charge" -> {
                    if (i + 1 < args.length) {
                        chargingHours = Integer.parseInt(args[++i]);
                    }
                }
                case "--help" -> {
                    printHelp();
                    return;
                }
            }
        }

        if (zone == null) {
            System.err.println("Du måste ange en priszon med --zone SE1|SE2|SE3|SE4");
            printHelp();
            return;
        }

        Prisklass prisklass;
        try {
            prisklass = Prisklass.valueOf(zone);
        } catch (IllegalArgumentException e) {
            System.err.println("Ogiltig zon: " + zone);
            printHelp();
            return;
        }

        LocalDate datum;
        if (dateStr == null) {
            datum = LocalDate.now();
        } else {
            try {
                datum = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                System.err.println("Ogiltigt datumformat. Använd YYYY-MM-DD");
                return;
            }
        }

        ElpriserAPI api = new ElpriserAPI();
        List<Elpris> priser = api.getPriser(datum, prisklass);

        if (priser.isEmpty()) {
            System.out.println("Inga priser hittades för " + datum + " i zon " + prisklass);
            return;
        }

        if (chargingHours > 0) {
            if (chargingHours > priser.size()) {
                System.err.println("Kan inte ladda längre än " + priser.size() + " timmar.");
                return;
            }

            double minSum = Double.MAX_VALUE;
            int bestStartIndex = 0;

            for (int i = 0; i <= priser.size() - chargingHours; i++) {
                double sum = 0;
                for (int j = 0; j < chargingHours; j++) {
                    sum += priser.get(i + j).sekPerKWh() * 100;
                }
                if (sum < minSum) {
                    minSum = sum;
                    bestStartIndex = i;
                }
            }

            Elpris start = priser.get(bestStartIndex);
            Elpris end = priser.get(bestStartIndex + chargingHours - 1);
            double avg = minSum / chargingHours;

            System.out.printf(
                    "Påbörja laddning kl %02d:00 och ladda till %02d:00%n",
                    start.timeStart().getHour(),
                    end.timeEnd().getHour()
            );
            System.out.printf("Total kostnad: %.2f öre%n", minSum);
            System.out.printf("Genomsnitt: %.2f öre/kWh%n", avg);
            return;
        }

        if (sorted) {
            priser.sort(Comparator.comparingDouble(Elpris::sekPerKWh).reversed());
        }

        for (Elpris pris : priser) {
            String start = String.format("%02d", pris.timeStart().getHour());
            String end = String.format("%02d", pris.timeEnd().getHour());
            double öre = pris.sekPerKWh() * 100;
            System.out.printf("%s-%s %.2f öre%n", start, end, öre);
        }
    }

    private static void printHelp() {
        System.out.println("""
                ⚡ Electricity Price Optimizer CLI
                Användning:
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
