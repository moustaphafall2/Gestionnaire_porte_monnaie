package application.mapper;

import application.dto.MouvementDTO;
import domain.entity.MouvementEpargne;

/*
    * MouvementMapper rassemble la conversion d'un MouvementEpargne du domaine vers son
    * MouvementDTO en un seul endroit : ServiceEpargne l'appelle au lieu de construire le DTO
    * champ par champ. Aucun calcul : une copie directe des trois champs.
    *
    * Pas de versListeDTO, contrairement à TransactionMapper : cette conversion n'a qu'un seul
    * appelant, la boucle de ServiceEpargne.getMouvements(). Une méthode de liste ici n'aurait
    * aucun second usage réel ; la cohérence de forme avec TransactionMapper ne justifie pas à
    * elle seule une méthode sans appelant.
*/
public class MouvementMapper {

    // Classe utilitaire : uniquement une méthode statique, pas d'instance à créer.
    private MouvementMapper() {
    }

    // Construit le DTO à partir d'un MouvementEpargne du domaine : une simple copie des champs.
    public static MouvementDTO versDTO(MouvementEpargne mouvement) {
        return new MouvementDTO(mouvement.getMontant(), mouvement.getSens(), mouvement.getDate());
    }
}
