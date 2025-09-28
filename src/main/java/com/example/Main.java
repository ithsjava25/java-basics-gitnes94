package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        Map<String, String> opts = ArgParser.parse(args);

        if (opts.containsKey("help") || args.length == 0) {
            printHelp();
            return;
        }

        String zone = opts.getOrDefault("zone", "SE3");
        String dateStr = opts.getOrDefault("date", LocalDate.now().toString());

        System.out.println("Zon: " + zone);
        System.out.println("Datum: " + dateStr);

        try {
            ElpriserAPI.Prisklass prisklass = ElpriserAPI.Prisklass.valueOf(zone);
            LocalDate date = LocalDate.parse(dateStr);

            ElpriserAPI api = new ElpriserAPI();

            List<ElpriserAPI.Elpris> priser = new ArrayList<>();
            priser.addAll(api.getPriser(date, prisklass));
            priser.addAll(api.getPriser(date.plusDays(1), prisklass));

            if (opts.containsKey("sort")) {
                priser.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh));
            } else {
                priser.sort(Comparator.comparing(ElpriserAPI.Elpris::timeStart));
            }

            double minPris = priser.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).min().orElse(0);
            double maxPris = priser.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).max().orElse(0);
            double medelPris = priser.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).average().orElse(0);

            for (ElpriserAPI.Elpris p : priser) {
                System.out.printf("%02d:00 - %.4f SEK/kWh%n", p.timeStart().getHour(), p.sekPerKWh());
            }

            System.out.printf("Minpris: %.4f%n", minPris);
            System.out.printf("Maxpris: %.4f%n", maxPris);
            System.out.printf("Medelpris: %.4f%n", medelPris);

            if (opts.containsKey("charging")) {
                printOptimalCharging(priser, 2);
                printOptimalCharging(priser, 4);
                printOptimalCharging(priser, 8);
            }

        } catch (Exception e) {
            System.out.println("Fel: " + e.getMessage());
        }
    }

    private static void printOptimalCharging(List<ElpriserAPI.Elpris> priser, int hours) {
        if (priser.size() < hours) return;

        double bestAvg = Double.MAX_VALUE;
        int bestIndex = 0;

        for (int i = 0; i <= priser.size() - hours; i++) {
            double sum = 0;
            for (int j = 0; j < hours; j++) {
                sum += priser.get(i + j).sekPerKWh();
            }
            double avg = sum / hours;
            if (avg < bestAvg) {
                bestAvg = avg;
                bestIndex = i;
            }
        }

        LocalTime start = priser.get(bestIndex).timeStart().toLocalTime();
        LocalTime end = priser.get(bestIndex + hours - 1).timeStart().plusHours(1).toLocalTime();

        System.out.printf("Bästa %dh-fönster: %02d:00 - %02d:00 (%.4f SEK/kWh i snitt)%n",
                hours, start.getHour(), end.getHour(), bestAvg);
    }

    private static void printHelp() {
        System.out.println("Användning: java -jar app.jar --zone SE3 --date YYYY-MM-DD [--sort] [--charging]");
        System.out.println("--zone <SE1|SE2|SE3|SE4> Välj elområde (default SE3)");
        System.out.println("--date <YYYY-MM-DD>      Välj datum (default idag)");
        System.out.println("--sort                   Sortera priserna efter lägst till högst");
        System.out.println("--charging               Visa bästa laddningsfönster");
        System.out.println("--help                   Visa denna hjälptext");
    }
}
