package com.galdevs.botmartingala.analisis.repositories;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.galdevs.botmartingala.analisis.entities.LogMartingalaEven;

public interface LogMartingalaEvenRepository extends JpaRepository<LogMartingalaEven, Long> {
	 boolean existsByTimestampAperturaAndInstIdAndSentido(long timestampApertura, String instId, int sentido);
	 boolean existsByTimestampAperturaAndInstIdAndSentidoAndGap(long timestampApertura, String instId, int sentido, double gap);
	    List<LogMartingalaEven> findByGapAndSentidoOrderByTimestampCierreAsc(double gap, int sentido);  // Existing method for filtering and sorting
	
	    @Query("SELECT DISTINCT gap FROM LogMartingalaEven")
	    List<Double> findDistinctGaps();

	    @Query("SELECT DISTINCT sentido FROM LogMartingalaEven")
	    List<Integer> findDistinctSentidos();

}