package org.lucasbenko;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final String PLAYER_URL = "";

    public static int cellIndex = 3;
    static GoogleSheetsLiveTest liveTest = new GoogleSheetsLiveTest();
    public static void main(String[] args) throws IOException, GeneralSecurityException, InterruptedException {
        GoogleSheetsLiveTest.setup();
        while (cellIndex < 25){
            String cell = "A" + cellIndex;
            String playerInfo = readSheets(cell);

            Pattern pattern = Pattern.compile("\\b[A-Z]{3}\\b");
            Matcher matcher = pattern.matcher(playerInfo);

            String teamName = null;

            if (matcher.find()) {
                String acronym = matcher.group();
                teamName = acronym;
            }else{
                cellIndex++;
                continue;
            }

            getScheduleForTeam(teamName);
            getDatesOfCurrentWeek();
            cellIndex++;

            Thread.sleep(1); // Wait here for API limit reasons!!
        }
    }

    public static void getScheduleForTeam(String teamName) {
        String season = String.valueOf(Year.now().getValue()) + String.valueOf(Year.now().getValue() + 1);
        ArrayList<Game> games;
        try {
            String[] datesInWeek = getDatesOfCurrentWeek();
            games = new ArrayList<Game>();
            URL url = new URL("https://api-web.nhle.com/v1/club-schedule-season/" + teamName + "/" + season);
            //System.out.println(url);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            int responseCode = conn.getResponseCode();

            if (responseCode != 200) {
                System.out.println("Access is denied");
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            }

            StringBuilder informationString = new StringBuilder();
            try (Scanner scanner = new Scanner(conn.getInputStream())) {
                while (scanner.hasNext()) {
                    informationString.append(scanner.nextLine());
                }
            }

            JsonObject jsonObject = JsonParser.parseString(informationString.toString()).getAsJsonObject();
            JsonArray gamesArray = jsonObject.getAsJsonArray("games");

            for (JsonElement gameElem : gamesArray) {
                JsonObject game = gameElem.getAsJsonObject();
                String gameDate = game.get("gameDate").getAsString();

                boolean inWeek = ignoreCaseContainsForLoop(datesInWeek, gameDate);

                if (inWeek) {
                    JsonObject homeTeamObj = game.getAsJsonObject("homeTeam");
                    String homeTeamAbbrev = homeTeamObj.get("abbrev").getAsString();

                    JsonObject awayTeamObj = game.getAsJsonObject("awayTeam");
                    String awayTeamAbbrev = awayTeamObj.get("abbrev").getAsString();

                    String gameData = null;

                    if (homeTeamAbbrev.equalsIgnoreCase(teamName)){
                        gameData = "vs " + awayTeamAbbrev;
                    }else{
                        gameData = "@ " + homeTeamAbbrev;
                    }

                    Game gameObj = new Game(awayTeamAbbrev, homeTeamAbbrev, gameDate, gameData);
                    games.add(gameObj);
                }
            }

        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < games.size(); i++) {
            System.out.println(games.get(i).gameDate);
            System.out.println(games.get(i).gameData);
        }
        try {
            sendToSheets(games, cellIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean ignoreCaseContainsForLoop(String[] list, String value) {
        for (String e : list) {
            if (value.equalsIgnoreCase(e)) {
                return true;
            }
        }
        return false;
    }

    public static String[] getDatesOfCurrentWeek(){
        LocalDate today = LocalDate.now();
        return Arrays.stream(DayOfWeek.values())
                .map(today::with)
                .map(LocalDate::toString)
                .toArray(String[]::new);
    }

    private static String readSheets(String cell) throws IOException {
        return liveTest.readCell(cell);
    }

    private static void sendToSheets(ArrayList<Game> games, int cellIndex) throws IOException {
        liveTest.writeSheet(games, cellIndex);
    }
}

