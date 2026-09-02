package application.mapper;

import java.util.ArrayList;
import java.util.List;

import application.dto.TransactionDTO;
import domain.entity.Transaction;

/*
    * TransactionMapper traduit une Transaction du domaine en TransactionDTO. Ne calcule rien :
    * copie directe des champs.
*/
public class TransactionMapper {

    private TransactionMapper() {
    }

    public static TransactionDTO versDTO(Transaction transaction) {
        return new TransactionDTO(transaction.getId(), transaction.getMontant(), transaction.getType(),
                transaction.getCategorie(), transaction.getDate(), transaction.getDescription());
    }

    public static List<TransactionDTO> versListeDTO(List<Transaction> transactions) {
        List<TransactionDTO> resultat = new ArrayList<>();
        for (Transaction transaction : transactions) {
            resultat.add(versDTO(transaction));
        }
        return resultat;
    }
}
