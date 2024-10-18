package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GraficoUtil {
    private static final String TAG = "GraficoUtil";

    public static void gerarGrafico(Context context, LinearLayout layout, String arquivoXLSX) {
        List<Double> mmf = new ArrayList<>();
        List<Double> fluxo = new ArrayList<>();
        List<Double> correnteMagnetizacao = new ArrayList<>();

        try {
            InputStream inputStream = context.getAssets().open(arquivoXLSX);
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                mmf.add(row.getCell(0).getNumericCellValue());
                double fluxValue = row.getCell(1).getNumericCellValue();
                fluxo.add(fluxValue == 0 ? 1e-6 : fluxValue); // Evitar zero no fluxo
            }

            for (int i = 0; i < mmf.size(); i++) {
                correnteMagnetizacao.add(mmf.get(i) / fluxo.get(i));
            }

            workbook.close();
            inputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao ler o arquivo XLSX: " + e.getMessage(), e);
            return;
        }

        GraphView graph = new GraphView(context);
        graph.setTitle("Corrente de Magnetização x Tempo");

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();

        double passo = 1.0 / 3000;
        for (int i = 0; i < mmf.size(); i++) {
            double tempo = i * passo * 1000;
            series.appendData(new DataPoint(tempo, correnteMagnetizacao.get(i)), true, mmf.size());
        }

        graph.addSeries(series);
        layout.addView(graph);
    }
}
