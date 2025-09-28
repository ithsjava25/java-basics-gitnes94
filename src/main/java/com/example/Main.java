package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        Map<String, String> opts = ArgParser.parse(args);

        if (opts.containsKey("help")) {
            showHelp();
            return;
        }

        String zone = opts.get("zone");
        if (zone == null) {
            System.out.println("Fel: --zone är obligatoriskt (SE1|SE2|SE3|SE4)");
            return;
        }

        String dateStr = opts.getOrDefault("date", LocalDate.now().toString());
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            System.out.println("Fel: Ogiltigt datumformat. Använd YYYY-MM-DD");
            return;
        }

        boolean sorted = opts.containsKey("sorted");
        String charging = opts.get("charging");

        try {
            ElpriserAPI.Prisklass prisklass = ElpriserAPI.Prisklass.valueOf(zone);
            ElpriserAPI api = new ElpriserAPI();
            List<ElpriserAPI.Elpris> priser = api.getPriser(date, prisklass);

            if (priser.isEmpty()) {
                System.out.println("Fel: Ingen data tillgänglig för vald zon/datum.");
                return;
            }

            priser.sort((p1, p2) -> sorted
                    ? Double.compare(p1.sekPerKWh(), p2.sekPerKWh())
                    : Integer.compare(p1.timeStart().getHour(), p2.timeStart().getHour())
            );

            System.out.println("ElpriserAPI initialiserat. Cachning: PÅ");
            System.out.println("!!! ANVÄNDER MOCK-DATA FÖR TEST !!!");
            System.out.println("Zon: " + zone);
            System.out.println("Datum: " + date);
            for (ElpriserAPI.Elpris p : priser) {
                System.out.printf("%02d:00 - %.4f SEK/kWh%n", p.timeStart().getHour(), p.sekPerKWh());
            }

            double minPris = priser.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).min().orElse(0);
            double maxPris = priser.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).max().orElse(0);
            double medelPris = priser.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).average().orElse(0);

            System.out.printf("Lägsta pris: %.4f%n", minPris);
            System.out.printf("Högsta pris: %.4f%n", maxPris);
            System.out.printf("Medelpris: %.4f%n", medelPris);

            if (charging != null) {
                int hours;
                try {
                    hours = Integer.parseInt(charging.replace("h", ""));
                } catch (Exception e) {
                    System.out.println("Fel: Ogiltig laddningstid. Ange 2h, 4h eller 8h");
                    return;
                }
                List<ElpriserAPI.Elpris> fullData = new ArrayList<>(priser);
                List<ElpriserAPI.Elpris> nextDayPrices = api.getPriser(date.plusDays(1), prisklass);
                fullData.addAll(nextDayPrices);

                ElpriserAPI.Elpris[] optimalWindow = findOptimalWindow(fullData, hours);
                if (optimalWindow != null) {
                    double avgPrice = Arrays.stream(optimalWindow)
                            .mapToDouble(ElpriserAPI.Elpris::sekPerKWh)
                            .average()
                            .orElse(0);
                    System.out.printf("Börja laddning: %02d:00 - %02d:00 (%.4f SEK/kWh i snitt)%n",
                            optimalWindow[0].timeStart().getHour(),
                            optimalWindow[optimalWindow.length - 1].timeStart().getHour() + 1,
                            avgPrice);
                }
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Fel: Ogiltig zon. Välj SE1, SE2, SE3 eller SE4");
        } catch (Exception e) {
            System.out.println("Fel: " + e.getMessage());
        }
    }

    private static ElpriserAPI.Elpris[] findOptimalWindow(List<ElpriserAPI.Elpris> priser, int hours) {
        if (priser.size() < hours) return null;
        double minSum = Double.MAX_VALUE;
        int startIdx = 0;
        for (int i = 0; i <= priser.size() - hours; i++) {
            double sum = 0;
            for (int j = 0; j < hours; j++) sum += priser.get(i + j).sekPerKWh();
            if (sum < minSum) {
                minSum = sum;
                startIdx = i;
            }
        }
        ElpriserAPI.Elpris[] window = new ElpriserAPI.Elpris[hours];
        for (int i = 0; i < hours; i++) window[i] = priser.get(startIdx + i);
        return window;
    }

    private static void showHelp() {
        System.out.println("Usage: java -jar app.jar --zone SE1|SE2|SE3|SE4 [--date YYYY-MM-DD] [--sorted] [--charging 2h|4h|8h] [--help]");
        System.out.println("--zone <SE1|SE2|SE3|SE4>     Välj elområde (obligatoriskt)");
        System.out.println("--date <YYYY-MM-DD>           Välj datum (valfritt, default idag)");
        System.out.println("--sorted                      Sortera priserna efter lägst till högst");
        System.out.println("--charging 2h|4h|8h           Visa bästa laddningsfönster");
        System.out.println("--help                        Visa denna hjälptext");
    }
}
