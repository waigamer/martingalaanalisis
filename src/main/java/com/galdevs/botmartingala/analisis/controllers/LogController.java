package com.galdevs.botmartingala.analisis.controllers;

import com.galdevs.botmartingala.analisis.entities.LogMartingalaEven;
import com.galdevs.botmartingala.analisis.services.LogService;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private LogService logService;

    @GetMapping("/read")
    public List<LogMartingalaEven> readLogs(@RequestParam(required = false) String directoryPath) {
        return logService.readLogs("C:\\Users\\pcage\\Documents\\GitHub\\cryptobobemulatornode\\logs");
    }
    
    @GetMapping("/state-list")
    public List<Integer> getStateList(@RequestParam double gap, @RequestParam int sentido) {
        return logService.generateStateListFromDB(gap, sentido);
    }
    @GetMapping("/state-list/csv")
    public ResponseEntity<String> getStateListCsv(@RequestParam double gap, @RequestParam int sentido) {
        List<Integer> stateList = logService.generateStateListFromDB(gap, sentido);

        // Convertir la lista de estados a formato CSV
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Index,State\n");
        for (int i = 0; i < stateList.size(); i++) {
            csvBuilder.append(i).append(",").append(stateList.get(i)).append("\n");
        }
        String csvData = csvBuilder.toString();

        // Configurar las cabeceras HTTP para indicar que se trata de un archivo CSV
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/csv");
        headers.add("Content-Disposition", "attachment; filename=state_list.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }
    
    @GetMapping(value = "/state-list/chart", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getStateChart(@RequestParam double gap, @RequestParam int sentido) throws IOException {
        List<Integer> stateList = logService.generateStateListFromDB(gap, sentido);

        // Crear el conjunto de datos para la gráfica
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < stateList.size(); i++) {
            dataset.addValue(stateList.get(i), "State", Integer.toString(i));
        }

     // Crear la gráfica con un título que incluye el gap y el sentido
        String chartTitle = "(Gap: " + gap + ", Sentido: " + sentido + ")";
        JFreeChart chart = ChartFactory.createLineChart(
                chartTitle,
                "Time",
                "State",
                dataset
        );

        // Personalizar el eje X para mostrar 4 referencias del índice
        CategoryPlot plot = chart.getCategoryPlot();
        CategoryAxis xAxis = plot.getDomainAxis();
        int tickInterval = stateList.size() > 4 ? stateList.size() / 4 : 1;

        for (int i = 0; i < stateList.size(); i++) {
            if (i % tickInterval != 0) {
                xAxis.setTickLabelPaint(Integer.toString(i), Color.white);  // Ocultar la etiqueta haciendo que sea del mismo color que el fondo
            }
        }

        xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45); 
       

        // Añadir una línea en el valor cero del eje Y
        ValueMarker zeroMarker = new ValueMarker(0);  // posición para la línea
        zeroMarker.setPaint(Color.red);  // color de la línea
        //zeroMarker.setLabel("Zero Line");  // etiqueta opcional para la línea
        plot.addRangeMarker(zeroMarker);

        // Convertir la gráfica a una imagen
        BufferedImage image = chart.createBufferedImage(1200, 800);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeBufferedImageAsPNG(baos, image);

        return baos.toByteArray();
    }

    
    @GetMapping(value = "/state-list/charts-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public byte[] getAllStateChartsPdf() throws IOException, DocumentException {
        List<Double> gaps = logService.findDistinctGaps();
        List<Integer> sentidos = logService.findDistinctSentidos();

        Document document = new Document();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        for (double gap : gaps) {
            for (int sentido : sentidos) {
                byte[] chartImage = getStateChart(gap, sentido);
                Image image = Image.getInstance(chartImage);
                document.setPageSize(image);
                document.newPage();
                image.setAbsolutePosition(0, 0);
                document.add(image);
            }
        }

        document.close();

        return baos.toByteArray();
    }


}