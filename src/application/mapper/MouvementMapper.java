package application.mapper;

import application.dto.MouvementDTO;
import domain.entity.MouvementEpargne;

/*
    * MouvementMapper traduit un MouvementEpargne du domaine en MouvementDTO. Aucun calcul, pas
    * de versListeDTO : un seul appelant, la boucle de ServiceEpargne.getMouvements().
*/
public class MouvementMapper {

    private MouvementMapper() {
    }

    public static MouvementDTO versDTO(MouvementEpargne mouvement) {
        return new MouvementDTO(mouvement.getMontant(), mouvement.getSens(), mouvement.getDate());
    }
}
