package com.example;

import com.example.api.ElpriserAPI;
import java.time.LocalDate;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Map<String, String> opts = ArgParser.parse(args);

        if (opts.containsKey("help") || opts.isEmpty()) {
            printHelp();
            return;
        }

        String zone = opts.getOrDefault("zone", "SE3");
        LocalDate today = LocalDate.now();
        String dateStr = opts.getOrDefault("date", today.toString());

        try {
            ElpriserAPI.Prisklass prisklass = ElpriserAPI.Prisklass.valueOf(zone);
            LocalDate date = LocalDate.parse(dateStr);

            ElpriserAPI api = new ElpriserAPI();
            List<ElpriserAPI.Elpris> priser = new ArrayList<>(api.getPriser(date, prisklass));

            if (!opts.containsKey("date")) {
                priser.addAll(api.getPriser(date.plusDays(1), prisklass));
            }

            if (priser.isEmpty()) {
                System.out.println("Ingen data tillgänglig.");
                return;
            }

            if (opts.containsKey("sort")) {
                priser.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh));
            }

            System.out.println("Zon: " + zone);
            System.out.println("Datum: " + dateStr);
            for (ElpriserAPI.Elpris p : priser) {
                System.out.printf("%s - %.4f SEK/kWh%n",
                        p.timeStart().toLocalTime(), p.sekPerKWh());
            }

            double min = priser.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).min().orElse(0);
            double max = priser.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).max().orElse(0);
            double mean = priser.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).average().orElse(0);

            System.out.printf("Minpris: %.4f%n", min);
            System.out.printf("Maxpris: %.4f%n", max);
            System.out.printf("Medelpris: %.4f%n", mean);

            findOptimal(priser, 2);
            findOptimal(priser, 4);
            findOptimal(priser, 8);

        } catch (IllegalArgumentException e) {
            System.out.println("Fel: Ogiltig zon " + zone);
        } catch (Exception e) {
            System.out.println("Fel: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Användning: java -jar app.jar --zone SE3 --date 2025-09-26 [--sort]");
        System.out.println("  --zone <SE1|SE2|SE3|SE4>  Välj elområde (default SE3)");
        System.out.println("  --date <YYYY-MM-DD>       Välj datum (default idag)");
        System.out.println("  --sort                    Sortera priserna efter lägst till högst");
        System.out.println("  --help                    Visa denna hjälptext");
    }

    private static void findOptimal(List<ElpriserAPI.Elpris> priser, int hours) {
        if (priser.size() < hours) return;

        double bestAvg = Double.MAX_VALUE;
        int bestIndex = 0;

        for (int i = 0; i <= priser.size() - hours; i++) {
            double avg = priser.subList(i, i + hours).stream()
                    .mapToDouble(ElpriserAPI.Elpris::sekPerKWh)
                    .average().orElse(Double.MAX_VALUE);

            if (avg < bestAvg) {
                bestAvg = avg;
                bestIndex = i;
            }
        }

        ElpriserAPI.Elpris start = priser.get(bestIndex);
        ElpriserAPI.Elpris end = priser.get(bestIndex + hours - 1);

        System.out.printf("Bästa %dh-fönster: %s - %s (%.4f SEK/kWh i snitt)%n",
                hours, start.timeStart().toLocalTime(),
                end.timeStart().toLocalTime(), bestAvg);
    }
}
