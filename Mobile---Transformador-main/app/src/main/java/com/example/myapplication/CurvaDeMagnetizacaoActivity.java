package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CurvaDeMagnetizacaoActivity extends AppCompatActivity {

    private GraphView graph;
    private Button uploadButton;

    private ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri uri = data.getData();
                        readExcelFile(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curva_de_magnetizacao);

        graph = findViewById(R.id.graph);
        uploadButton = findViewById(R.id.upload_button);

        uploadButton.setOnClickListener(view -> openFilePicker());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private void readExcelFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0); // Obtém a primeira aba do arquivo

            List<Double> mmf = new ArrayList<>();
            List<Double> fluxo = new ArrayList<>();

            // Lê os dados das colunas MMF e Fluxo
            for (Row row : sheet) {
                if (row.getPhysicalNumberOfCells() < 2) continue; // Ignora linhas com menos de 2 células

                // Lê MMF
                double mmfValue = 0;
                if (row.getCell(0) != null) {
                    if (row.getCell(0).getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                        mmfValue = row.getCell(0).getNumericCellValue();
                    } else {
                        Log.e("ExcelReader", "Valor não numérico encontrado na coluna MMF: " + row.getCell(0).toString());
                        continue; // Ignora essa linha
                    }
                }
                mmf.add(mmfValue);

                // Lê Fluxo
                double fluxoValue = 0;
                if (row.getCell(1) != null) {
                    if (row.getCell(1).getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                        fluxoValue = row.getCell(1).getNumericCellValue();
                    } else {
                        Log.e("ExcelReader", "Valor não numérico encontrado na coluna Fluxo: " + row.getCell(1).toString());
                        continue; // Ignora essa linha
                    }
                }
                fluxo.add(fluxoValue);
            }

            // Evitar divisão por zero no fluxo
            for (int i = 0; i < fluxo.size(); i++) {
                if (fluxo.get(i) == 0) {
                    fluxo.set(i, 1e-6);  // Pequeno valor para evitar zero
                }
            }

            // Calcular corrente de magnetização (MMF / Fluxo)
            List<Double> correnteMagnetizacao = new ArrayList<>();
            for (int i = 0; i < mmf.size(); i++) {
                correnteMagnetizacao.add(mmf.get(i) / fluxo.get(i));
            }

            // Gerar tempo (0 <= t <= 340ms; passo de 1/3000s)
            List<Double> tempo = new ArrayList<>();
            double passo = 1.0 / 3000; // 1/3000 segundos
            for (int i = 0; i < mmf.size(); i++) {
                tempo.add(i * passo * 1000);  // Tempo em milissegundos
            }

            // Criar série de dados e adicionar ao gráfico
            DataPoint[] dataPoints = new DataPoint[mmf.size()];
            for (int i = 0; i < mmf.size(); i++) {
                dataPoints[i] = new DataPoint(tempo.get(i), correnteMagnetizacao.get(i));
            }

            LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
            graph.addSeries(series);
            graph.getViewport().setScrollable(true);
            graph.getViewport().setScalable(true);
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(getMaxValue(tempo)); // O máximo deve ser baseado no tempo
            graph.getViewport().setMinY(0);
            graph.getViewport().setMaxY(getMaxValue(correnteMagnetizacao));

            Toast.makeText(this, "Gráfico gerado com sucesso!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("ExcelReader", "Erro ao ler o arquivo: " + e.getMessage(), e);
            Toast.makeText(this, "Erro ao ler o arquivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private double getMaxValue(List<Double> values) {
        double maxValue = Double.MIN_VALUE; // Inicializa com o menor valor possível
        for (Double value : values) {
            if (value > maxValue) {
                maxValue = value;
            }
        }
        return maxValue;
    }
}
