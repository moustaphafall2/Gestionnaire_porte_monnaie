package application.mapper;

import application.dto.ObjectifDTO;
import domain.entity.Epargne;

/*
    * ObjectifMapper traduit une Epargne du domaine en ObjectifDTO. Ne calcule rien :
    * montantActuel et pourcentageAtteint sont reçus déjà calculés. Pas de versListeDTO, chaque
    * objectif ayant ses propres valeurs calculées individuellement.
*/
public class ObjectifMapper {

    private ObjectifMapper() {
    }

    public static ObjectifDTO versDTO(Epargne objectif, double montantActuel, double pourcentageAtteint) {
        return new ObjectifDTO(objectif.getId(), objectif.getNom(), objectif.getMontantCible(),
                montantActuel, pourcentageAtteint);
    }
}
