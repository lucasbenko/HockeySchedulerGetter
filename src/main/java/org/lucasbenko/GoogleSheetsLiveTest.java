package org.lucasbenko;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.junit.BeforeClass;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class GoogleSheetsLiveTest {
    private static Sheets sheetsService;
    private static String SPREADSHEET_ID = "1KlHe6KaROhFh5nSZjmwB2dwtrTNvtNvKwNj9eDeVO6s";

    @BeforeClass
    public static void setup() throws GeneralSecurityException, IOException {
        sheetsService = SheetsServiceUtil.getSheetsService();
    }

    public void writeSheet(ArrayList<Game> games, int cellIndex) throws IOException {

        int sheetIndex = 7;

        if (Main.getChoice() == 2){
            sheetIndex++;
        }

        List<Object> row = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        LocalDate referenceMonday = LocalDate.of(2024, 11, 18);
        LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        long weeksBetween = ChronoUnit.WEEKS.between(referenceMonday, today);

        sheetIndex += weeksBetween;

        for (int i = 0; i < 7; i++) {
            LocalDate currentDay = monday.plusDays(i);
            if (Main.getChoice() == 2){
                currentDay = nextMonday.plusDays(i);
            }

            boolean matchFound = false;
            for (int j = 0; j < games.size(); j++) {
                if (games.get(j).gameDate.equals(currentDay.toString())) {
                    row.add(games.get(j).gameData);
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                row.add("");
            }
        }

        ValueRange body = new ValueRange()
                .setValues(Arrays.asList(row));

        UpdateValuesResponse result = sheetsService.spreadsheets().values()
                .update(SPREADSHEET_ID, "'Week " + sheetIndex + "'!C" + cellIndex, body) // Add single quotes
                .setValueInputOption("RAW")
                .execute();
    }

    public String readCell(String cell) throws IOException {
        ValueRange cellData = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, cell).execute();
        List<List<Object>> values = cellData.getValues();

        if (values != null && !values.isEmpty()) {
            List<Object> row = values.get(0);

            if (!row.isEmpty()) {
                String cellValue = (String) row.get(0);
                return cellValue;
            }
        }
        return null;
    }
}
