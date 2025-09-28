package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Map<String, String> opts = ArgParser.parse(args);

        // Standardvärden om man inte anger något
        String zone = opts.getOrDefault("zone", "SE3");
        String dateStr = opts.getOrDefault("date", LocalDate.now().toString());

        System.out.println("Zon: " + zone);
        System.out.println("Datum: " + dateStr);

        try {

            ElpriserAPI.Prisklass prisklass = ElpriserAPI.Prisklass.valueOf(zone);

            LocalDate date = LocalDate.parse(dateStr);

            ElpriserAPI api = new ElpriserAPI();
            List<ElpriserAPI.Elpris> priser = api.getPriser(date, prisklass);

            for (ElpriserAPI.Elpris elpriser : priser) {
                System.out.printf("%s - %.4f SEK/KWh%n", elpriser.timeStart().toLocalTime(), elpriser.sekPerKWh());
            }
        } catch (Exception e) {
            System.out.println("Fel: " + e.getMessage());
        }

    }
}
