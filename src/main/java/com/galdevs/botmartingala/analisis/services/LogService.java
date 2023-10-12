package com.galdevs.botmartingala.analisis.services;


import org.springframework.stereotype.Service;

import com.galdevs.botmartingala.analisis.entities.LogMartingalaEven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogService {

    public List<LogMartingalaEven> readLogs(String directoryPath) {
        List<LogMartingalaEven> logs = new ArrayList<>();
        try {
            Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".log"))
                    .forEach(p -> logs.addAll(processLogFile(p)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logs;
    }

    private List<LogMartingalaEven> processLogFile(Path filePath) {
        List<LogMartingalaEven> logs = new ArrayList<>();
        File file = filePath.toFile();
        String fileName = file.getName();

        // Extract sentido and gap from file name
        Pattern pattern = Pattern.compile("_(\\d)_([\\d]{3})_");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            int sentido = Integer.parseInt(matcher.group(1));
            double gap = Double.parseDouble(matcher.group(2)) / 10;

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    LogMartingalaEven log = new LogMartingalaEven();
                    log.setSentido(sentido);
                    log.setGap(gap);

                    if (line.contains("Abrimos")) {
                        // Extraer la fecha y la hora
                        String dateTime = line.split("\\s+")[1] + " " + line.split("\\s+")[2];
                        log.setFechaApertura(dateTime);
                        log.setTimestampApertura(parseDateTimeToTimestamp(dateTime));

                        // Extraer el inst_id
                         pattern = Pattern.compile("el par (\\w+-\\w+)");
                         matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            log.setInstId(matcher.group(1));
                        }
                    } else if (line.contains("Cerramos")) {
                        // Extraer la fecha y la hora
                        String dateTime = line.split("\\s+")[1] + " " + line.split("\\s+")[2];
                        log.setFechaCierre(dateTime);
                        log.setTimestampCierre(parseDateTimeToTimestamp(dateTime));

                        // Determinar si la operación fue un éxito o una pérdida
                        if (line.contains("beneficio")) {
                            log.setExito(1);
                        } else if (line.contains("perdida")) {
                            log.setExito(0);
                        }

                        logs.add(log);
                        log = new LogMartingalaEven();  // Reiniciar el objeto log para la próxima operación
                        log.setSentido(sentido);
                        log.setGap(gap);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return logs;
    }
    
    private long parseDateTimeToTimestamp(String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy HH:mm:ss", Locale.ENGLISH);
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

}
