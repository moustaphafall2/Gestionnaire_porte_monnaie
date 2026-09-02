package application.mapper;

import java.util.ArrayList;
import java.util.List;

import application.dto.TransactionDTO;
import domain.entity.Transaction;

/*
    * TransactionMapper rassemble la conversion d'une Transaction du domaine vers son
    * TransactionDTO en un seul endroit : ServiceTransaction l'appelle au lieu de construire le
    * DTO champ par champ. Il traduit, il ne calcule rien : une copie directe des champs, aucune
    * valeur dérivée à recevoir en paramètre (contrairement à ObjectifMapper).
*/
public class TransactionMapper {

    // Classe utilitaire : uniquement des méthodes statiques, pas d'instance à créer.
    private TransactionMapper() {
    }

    // Construit le DTO à partir d'une Transaction du domaine : une simple copie des champs, sans
    // calcul ni mise en forme (le formatage reste du ressort de VueTransaction).
    public static TransactionDTO versDTO(Transaction transaction) {
        return new TransactionDTO(transaction.getId(), transaction.getMontant(), transaction.getType(),
                transaction.getCategorie(), transaction.getDate(), transaction.getDescription());
    }

    // Même conversion appliquée à une liste entière : utilisée par ServiceTransaction partout où
    // une liste de Transaction est déjà prête (historique trié, résultat d'un filtrage) et n'a
    // plus qu'à être traduite en DTO.
    public static List<TransactionDTO> versListeDTO(List<Transaction> transactions) {
        List<TransactionDTO> resultat = new ArrayList<>();
        for (Transaction transaction : transactions) {
            resultat.add(versDTO(transaction));
        }
        return resultat;
    }
}
