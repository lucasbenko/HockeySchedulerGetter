package org.lucasbenko;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.junit.BeforeClass;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class GoogleSheetsLiveTest {
    private static Sheets sheetsService;
    private static String SPREADSHEET_ID = "1kkbJsmsB3e3HXd5KADL6rVw3H0Wt2cpjPx8vOtnEjL8";

    @BeforeClass
    public static void setup() throws GeneralSecurityException, IOException {
        sheetsService = SheetsServiceUtil.getSheetsService();
    }

    public void writeSheet(ArrayList<Game> games, int cellIndex) throws IOException {

        List<Object> row = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (int i = 0; i < 7; i++) {
            LocalDate currentDay = monday.plusDays(i);

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
                .update(SPREADSHEET_ID, "C" + cellIndex, body)
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
