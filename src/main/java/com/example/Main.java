package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.time.LocalDate;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java -jar app.jar --zone <SE1|SE2|SE3|SE4> [--date YYYY-MM-DD]");
            return;
        }

        String zoneArg = null;
        String dateArg = null;
        for (int i = 0; i < args.length; i++) {
            if ("--zone".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                zoneArg = args[i + 1];
            }
            if ("--date".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                dateArg = args[i + 1];
            }
        }

        if (zoneArg == null) {
            System.out.println("Fel: --zone är obligatoriskt (SE1|SE2|SE3|SE4)");
            return;
        }

        Prisklass prisklass;
        try {
            prisklass = Prisklass.valueOf(zoneArg.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Fel: okänd zon. Välj SE1, SE2, SE3 eller SE4");
            return;
        }

        LocalDate datum = dateArg != null ? LocalDate.parse(dateArg) : LocalDate.now();

        ElpriserAPI api = new ElpriserAPI(false);
        List<Elpris> priser = api.getPriser(datum, prisklass);

        if (priser.isEmpty()) {
            System.out.println("Inga priser hittades för " + prisklass + " den " + datum);
            return;
        }

        double minPris = priser.stream().mapToDouble(Elpris::sekPerKWh).min().orElse(0);
        double maxPris = priser.stream().mapToDouble(Elpris::sekPerKWh).max().orElse(0);
        double medelPris = priser.stream().mapToDouble(Elpris::sekPerKWh).average().orElse(0);

        System.out.println("Zon: " + prisklass);
        System.out.println("Datum: " + datum);
        priser.forEach(pris ->
                System.out.printf("%02d:00 - %.4f SEK/kWh%n", pris.timeStart().getHour(), pris.sekPerKWh())
        );
        System.out.printf("Lägsta pris: %.4f%n", minPris);
        System.out.printf("Högsta pris: %.4f%n", maxPris);
        System.out.printf("Medelpris: %.4f%n", medelPris);

        double lägstaGenomsnitt = Double.MAX_VALUE;
        int startHour = 0;
        for (int i = 0; i <= priser.size() - 8; i++) {
            double sum = 0;
            for (int j = 0; j < 8; j++) {
                sum += priser.get(i + j).sekPerKWh();
            }
            double avg = sum / 8;
            if (avg < lägstaGenomsnitt) {
                lägstaGenomsnitt = avg;
                startHour = i;
            }
        }

        System.out.printf("Påbörja laddning: %02d:00 - %02d:00 (%.4f SEK/kWh i snitt)%n",
                startHour, startHour + 8, lägstaGenomsnitt);
    }
}
