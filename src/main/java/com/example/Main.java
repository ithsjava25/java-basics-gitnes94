package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String zone = null;
        String dateStr = null;

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
                case "--help" -> {
                    printHelp();
                    return;
                }
            }
        }

        if (zone == null) {
            System.err.println("❌ Du måste ange en priszon med --zone SE1|SE2|SE3|SE4");
            printHelp();
            return;
        }

        Prisklass prisklass;
        try {
            prisklass = Prisklass.valueOf(zone);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Ogiltig zon: " + zone);
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
                System.err.println("❌ Ogiltigt datumformat. Använd YYYY-MM-DD");
                return;
            }
        }

        ElpriserAPI api = new ElpriserAPI();
        List<Elpris> priser = api.getPriser(datum, prisklass);

        if (priser.isEmpty()) {
            System.out.println("⚠️ Inga priser hittades för " + datum + " i zon " + prisklass);
            return;
        }

        System.out.println("\n📊 Elpriser för " + prisklass + " (" + datum + "):\n");
        for (Elpris pris : priser) {
            System.out.printf("%s - %s | %.2f SEK/kWh\n",
                    pris.timeStart().toLocalTime(),
                    pris.timeEnd().toLocalTime(),
                    pris.sekPerKWh());
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
                  --help                   (visar denna hjälp)
                """);
    }
}
