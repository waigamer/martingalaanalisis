package com.galdevs.botmartingala.analisis.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.galdevs.botmartingala.analisis.entities.LogMartingalaEven;
import com.galdevs.botmartingala.analisis.repositories.LogMartingalaEvenRepository;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogService {
	
	  @Autowired
	    private LogMartingalaEvenRepository logRepository;  // Inject the repository

	  @Autowired
	    private ResourceLoader resourceLoader;  // Inyectar ResourceLoader

	   public List<LogMartingalaEven> readLogs(String directoryPath) {
	        if (directoryPath == null || directoryPath.isEmpty()) {
	           
	            	directoryPath = System.getProperty("user.dir");
				
	        }
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
		    Map<String, LogMartingalaEven> openOperations = new HashMap<>();  // Mapa para rastrear operaciones abiertas por par
		    File file = filePath.toFile();
		    String fileName = file.getName();

		    // Extract sentido and gap from file name
		    Pattern pattern = Pattern.compile("_(\\d)_([\\d]{3})__.*_(\\d)\\.log$");
		    Matcher matcher = pattern.matcher(fileName);
		    if (matcher.find()) {
		    	int sentido = Integer.parseInt(matcher.group(3));
		        double gap = Double.parseDouble(matcher.group(2)) / 10;

		        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
		            String line;
		            while ((line = br.readLine()) != null) {
		                if (line.contains("Abrimos")) {
		                    LogMartingalaEven tempLog = new LogMartingalaEven();  // Iniciar un nuevo objeto temporal
		                    tempLog.setSentido(sentido);
		                    tempLog.setGap(gap);

		                    // Extraer la fecha y la hora
		                    String dateTime = line.split("\\s+")[1] + " " + line.split("\\s+")[2];
		                    tempLog.setFechaApertura(dateTime);
		                    tempLog.setTimestampApertura(parseDateTimeToTimestamp(dateTime));

		                    // Extraer el inst_id
		                    pattern = Pattern.compile("el par (\\w+-\\w+)");
		                    matcher = pattern.matcher(line);
		                    if (matcher.find()) {
		                        String instId = matcher.group(1);
		                        tempLog.setInstId(instId);
		                        openOperations.put(instId, tempLog);  // Guardar la operación abierta en el mapa
		                    }
		                } else if (line.contains("Cerramos")) {
		                    // Extraer el inst_id
		                	pattern = Pattern.compile("el par:?\\s*(\\w+-\\w+)");
		                    matcher = pattern.matcher(line);
		                    if (matcher.find()) {
		                        String instId = matcher.group(1);
		                        LogMartingalaEven tempLog = openOperations.get(instId);  // Obtener la operación abierta del mapa
		                        if (tempLog != null) {
		                            // Extraer la fecha y la hora
		                            String dateTime = line.split("\\s+")[1] + " " + line.split("\\s+")[2];
		                            tempLog.setFechaCierre(dateTime);
		                            tempLog.setTimestampCierre(parseDateTimeToTimestamp(dateTime));

		                            // Determinar si la operación fue un éxito o una pérdida
		                            if (line.contains("beneficio")) {
		                                tempLog.setExito(1);
		                            } else if (line.contains("perdida")) {
		                                tempLog.setExito(0);
		                            }
		                         // Calculate the time-to-live (TTL) for the operation
		                            long ttl = tempLog.getTimestampCierre() - tempLog.getTimestampApertura();
		                            tempLog.setTtl(ttl);
		                            logs.add(tempLog);  // Añadir el objeto temporal completo a la lista
		                            openOperations.remove(instId);  // Eliminar la operación abierta del mapa
		                        }
		                    }
		                }
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		    }
		 // Antes de guardar, verificar si el registro ya existe
		    List<LogMartingalaEven> logsToSave = new ArrayList<>();
		    for (LogMartingalaEven log : logs) {
		    	boolean exists = logRepository.existsByTimestampAperturaAndInstIdAndSentidoAndGap(
		    		    log.getTimestampApertura(),
		    		    log.getInstId(),
		    		    log.getSentido(),
		    		    log.getGap()
		    		);
		        if (!exists) {
		            logsToSave.add(log);
		        }
		    }
		    // Guardar solo los registros que no existen
		    logRepository.saveAll(logsToSave);
		    return logs;
		}
    
    private long parseDateTimeToTimestamp(String dateTime) {
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy HH:mm:ss:", Locale.ENGLISH);
    	LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    
    public List<Integer> generateStateList(List<LogMartingalaEven> logs) {
        List<Integer> stateList = new ArrayList<>();
        int sum = 0;

        for (LogMartingalaEven log : logs) {
            if (log.getExito() == 1) {
                sum += 1;  // Incrementar para una operación ganada
            } else if (log.getExito() == 0) {
                sum -= 1;  // Decrementar para una operación perdida
            }
            stateList.add(sum);
        }

        return stateList;
    }

    public List<Integer> generateStateListFromDB(double gap, int sentido) {
        List<LogMartingalaEven> logs = logRepository.findByGapAndSentidoOrderByTimestampCierreAsc(gap, sentido);  // Obtener registros filtrados de la base de datos
        return generateStateList(logs);  // Llamar al método existente para generar la lista de estados
    }
    

    public List<Double> findDistinctGaps() {
        return logRepository.findDistinctGaps();
    }

    public List<Integer> findDistinctSentidos() {
        return logRepository.findDistinctSentidos();
    }
}
