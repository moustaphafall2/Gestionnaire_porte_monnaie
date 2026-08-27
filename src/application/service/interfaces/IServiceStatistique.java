package application.service.interfaces;

import java.time.LocalDate;

import application.dto.StatistiqueDTO;

public interface IServiceStatistique {

    public StatistiqueDTO getStatistiques(LocalDate debut, LocalDate fin);
}
