package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Map<String, String> opts = parseArgs(args);

        if (opts.containsKey("help")) {
            showHelp();
            return;
        }

        String zoneStr = opts.get("zone");
        if (zoneStr == null) {
            System.out.println("Fel: Ingen zon angiven. Ange --zone SE1|SE2|SE3|SE4");
            return;
        }

        LocalDate date = LocalDate.now();
        if (opts.containsKey("date")) {
            try {
                date = LocalDate.parse(opts.get("date"));
            } catch (Exception e) {
                System.out.println("Fel: Ogiltigt datumformat. Använd YYYY-MM-DD");
                return;
            }
        }

        boolean sorted = opts.containsKey("sorted");
        String chargingArg = opts.get("charging");

        try {
            ElpriserAPI.Prisklass zone = ElpriserAPI.Prisklass.valueOf(zoneStr);
            ElpriserAPI api = new ElpriserAPI();

            List<ElpriserAPI.Elpris> priser = new ArrayList<>(api.getPriser(date, zone));
            if (priser.isEmpty()) {
                System.out.println("Fel: Ingen data tillgänglig för vald zon/datum.");
                return;
            }

            if (sorted) priser.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh));
            else priser.sort(Comparator.comparing(p -> p.timeStart().getHour()));

            System.out.println("Zon: " + zone + " Datum: " + date);
            for (ElpriserAPI.Elpris p : priser)
                System.out.printf("%02d:00 - %.4f SEK/kWh%n", p.timeStart().getHour(), p.sekPerKWh());

            double min = priser.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).min().orElse(0);
            double max = priser.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).max().orElse(0);
            double avg = priser.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).average().orElse(0);
            System.out.printf("Minpris: %.4f%n", min);
            System.out.printf("Maxpris: %.4f%n", max);
            System.out.printf("Medelpris: %.4f%n", avg);

            if (chargingArg != null) {
                int hours;
                try { hours = Integer.parseInt(chargingArg.replace("h","")); }
                catch (Exception e) {
                    System.out.println("Fel: Ogiltig laddningstid. Ange 2h, 4h eller 8h");
                    return;
                }

                List<ElpriserAPI.Elpris> fullData = new ArrayList<>(priser);
                fullData.addAll(api.getPriser(date.plusDays(1), zone));

                ElpriserAPI.Elpris[] window = findOptimalWindow(fullData, hours);
                if (window != null) {
                    double avgPrice = Arrays.stream(window).mapToDouble(ElpriserAPI.Elpris::sekPerKWh).average().orElse(0);
                    System.out.printf("Bästa %dh-fönster: %02d:00 - %02d:00 (%.4f SEK/kWh i snitt)%n",
                            hours, window[0].timeStart().getHour(),
                            window[window.length-1].timeStart().getHour()+1, avgPrice);
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
        System.out.println("Usage: java -cp target/classes com.example.Main --zone SE1|SE2|SE3|SE4 [--date YYYY-MM-DD] [--sorted] [--charging 2h|4h|8h] [--help]");
        System.out.println("--zone <SE1|SE2|SE3|SE4>     Välj elområde (obligatoriskt)");
        System.out.println("--date <YYYY-MM-DD>           Välj datum (valfritt, default idag)");
        System.out.println("--sorted                      Sortera priserna efter lägst till högst");
        System.out.println("--charging 2h|4h|8h           Visa bästa laddningsfönster");
        System.out.println("--help                        Visa denna hjälptext");
    }

    private static Map<String,String> parseArgs(String[] args) {
        Map<String,String> map = new HashMap<>();
        for (int i=0;i<args.length;i++) {
            switch(args[i]) {
                case "--zone": if (i+1<args.length) map.put("zone", args[++i]); break;
                case "--date": if (i+1<args.length) map.put("date", args[++i]); break;
                case "--sorted": map.put("sorted",""); break;
                case "--charging": if (i+1<args.length) map.put("charging", args[++i]); break;
                case "--help": map.put("help",""); break;
            }
        }
        return map;
    }
}
