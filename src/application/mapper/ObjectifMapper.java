package application.mapper;

import application.dto.ObjectifDTO;
import domain.entity.Epargne;

/*
    * ObjectifMapper rassemble la conversion d'une Epargne du domaine vers son ObjectifDTO en un
    * seul endroit : ServiceEpargne l'appelle au lieu de construire le DTO champ par champ. Il
    * traduit, il ne calcule pas : montantActuel et pourcentageAtteint sont déjà calculés par
    * CalculEpargne au moment où ServiceEpargne appelle cette méthode, jamais recalculés ici.
    *
    * Pas de versListeDTO : chaque objectif porte son propre montant actuel et son propre
    * pourcentage, calculés individuellement. Une méthode de liste devrait soit recalculer
    * elle-même (interdit à un mapper), soit recevoir des listes parallèles de valeurs à faire
    * avancer en même temps que les objectifs — plus fragile que la boucle explicite que
    * ServiceEpargne.getObjectifs() garde déjà pour cette raison.
*/
public class ObjectifMapper {

    // Classe utilitaire : uniquement une méthode statique, pas d'instance à créer.
    private ObjectifMapper() {
    }

    // Construit le DTO transmis à la présentation à partir d'une Epargne du domaine : une copie
    // de ses champs, plus montantActuel et pourcentageAtteint reçus déjà calculés.
    public static ObjectifDTO versDTO(Epargne objectif, double montantActuel, double pourcentageAtteint) {
        return new ObjectifDTO(objectif.getId(), objectif.getNom(), objectif.getMontantCible(),
                montantActuel, pourcentageAtteint);
    }
}
