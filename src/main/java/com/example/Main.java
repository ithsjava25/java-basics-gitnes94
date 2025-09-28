package com.example;

import com.example.api.ElpriserAPI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Map<String, String> opts = ArgParser.parse(args);

        String zone = opts.getOrDefault("zone", "SE3");
        String dateStr = opts.getOrDefault("date", LocalDate.now().toString());

        System.out.println("⚡ Electricity CLI startar...");
        System.out.println("Vald zon: " + zone);

        try {
            ElpriserAPI.Prisklass prisklass = ElpriserAPI.Prisklass.valueOf(zone);
            LocalDate date = LocalDate.parse(dateStr);

            ElpriserAPI api = new ElpriserAPI();
            List<ElpriserAPI.Elpris> priser = api.getPriser(date, prisklass);

            for (ElpriserAPI.Elpris elpris : priser) {
                System.out.printf("%s - %.4f SEK/kWh%n",
                        elpris.timeStart().toLocalTime(),
                        elpris.sekPerKWh());
            }
        } catch (Exception e) {
            System.out.println("Fel: " + e.getMessage());
        }
    }
}
