package com.example;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Main {

    public static class Elpris {
        private final int hour;
        private final double price;

        public Elpris(int hour, double price) {
            this.hour = hour;
            this.price = price;
        }

        public int timeStart() {
            return hour;
        }

        public int timeEnd() {
            return (hour + 1) % 24;
        }

        public double sekPerKWh() {
            return price;
        }
    }

    public static void main(String[] args) {
        String zone = "SE1";
        boolean sorted = false;
        int chargingHours = 0;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--zone" -> {
                    if (i + 1 < args.length) zone = args[++i];
                }
                case "--sorted" -> sorted = true;
                case "--charging" -> {
                    if (i + 1 < args.length) chargingHours = Integer.parseInt(args[++i]);
                }
                case "--help" -> {
                    printHelp();
                    return;
                }
            }
        }

        List<Elpris> priser = getMockPrices();

        System.out.println("ElpriserAPI initialiserat. Cachning: Av");
        System.out.println("!!! ANVÄNDER MOCK-DATA FÖR TEST !!!");

        if (sorted) {
            priser.sort(Comparator.comparingDouble(Elpris::sekPerKWh).reversed());
        }

        for (Elpris pris : priser) {
            String start = String.format("%02d", pris.timeStart());
            String end = String.format("%02d", pris.timeEnd());
            System.out.printf("%s-%s %.2f öre%n", start, end, pris.sekPerKWh());
        }

        double min = priser.stream().mapToDouble(Elpris::sekPerKWh).min().orElse(0);
        double max = priser.stream().mapToDouble(Elpris::sekPerKWh).max().orElse(0);
        double avg = priser.stream().mapToDouble(Elpris::sekPerKWh).average().orElse(0);

        System.out.printf("lägsta pris: %.2f%n", min);
        System.out.printf("högsta pris: %.2f%n", max);
        System.out.printf("medelpris: %.2f%n", avg);

        if (chargingHours > 0) {
            double minSum = Double.MAX_VALUE;
            int bestStart = 0;
            for (int i = 0; i <= priser.size() - chargingHours; i++) {
                double sum = 0;
                for (int j = 0; j < chargingHours; j++) sum += priser.get(i + j).sekPerKWh();
                if (sum < minSum) {
                    minSum = sum;
                    bestStart = i;
                }
            }
            int endHour = (bestStart + chargingHours) % 24;
            System.out.printf("Påbörja laddning: %02d:00 - %02d:00%n", priser.get(bestStart).timeStart(), endHour);
            System.out.printf("Total kostnad: %.2f öre%n", minSum);
            System.out.printf("Genomsnitt: %.2f öre/kWh%n", minSum / chargingHours);
        }
    }

    private static List<Elpris> getMockPrices() {
        List<Elpris> priser = new ArrayList<>();
        priser.add(new Elpris(0, 50.00));
        priser.add(new Elpris(1, 10.00));
        priser.add(new Elpris(2, 5.00));
        priser.add(new Elpris(3, 15.00));
        priser.add(new Elpris(4, 8.00));
        priser.add(new Elpris(5, 12.00));
        priser.add(new Elpris(6, 6.00));
        priser.add(new Elpris(7, 9.00));
        priser.add(new Elpris(8, 25.00));
        priser.add(new Elpris(9, 30.00));
        priser.add(new Elpris(10, 35.00));
        priser.add(new Elpris(11, 40.00));
        priser.add(new Elpris(12, 45.00));
        priser.add(new Elpris(13, 20.00));
        priser.add(new Elpris(14, 15.00));
        priser.add(new Elpris(15, 10.00));
        priser.add(new Elpris(16, 5.00));
        priser.add(new Elpris(17, 30.00));
        priser.add(new Elpris(18, 35.00));
        priser.add(new Elpris(19, 40.00));
        priser.add(new Elpris(20, 20.00));
        priser.add(new Elpris(21, 25.00));
        priser.add(new Elpris(22, 30.00));
        priser.add(new Elpris(23, 10.00));
        return priser;
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
                  --charging N             (hitta billigaste N timmar för laddning)
                  --help                   (visar denna hjälp)
                """);
    }
}
