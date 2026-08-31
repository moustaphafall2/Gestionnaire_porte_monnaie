package application.service.implementation;

import domain.entity.Epargne;
import domain.entity.Transaction;
import domain.enumeration.TypeTransaction;
import application.service.interfaces.IServiceSolde;

/*
    * ServiceSolde calcule le solde disponible et le total épargné : deux valeurs dérivées en
    * lecture seule des transactions et des objectifs, jamais stockées, toujours recalculées.
    * Extrait de ServicePortefeuille à l'étape 5, qui portait jusque-là trois responsabilités très
    * différentes (détenir le portefeuille, calculer, sauvegarder) pour un seul appelant qui en
    * avait vraiment besoin des trois (ServiceEpargne) — tous les autres appelants n'utilisaient
    * que le calcul (les contrôleurs) ou que la détention/la persistance (ServiceTransaction,
    * ServiceCategorie).
    *
    * Comme ServiceStatistique, il ne modifie jamais Portefeuille : aucun appel à sauvegarder().
    *
    * Il dépend de ServicePortefeuille (la classe concrète, pas l'interface) pour la même raison
    * que les autres services de ce paquet qui ont besoin de getDonnees() : cette méthode est à
    * visibilité de paquet, donc ne peut pas figurer dans IServicePortefeuille — une interface
    * Java ne peut pas déclarer de méthode à cette visibilité. C'est un choix assumé, pas un oubli
    * de la règle qui veut que les services dépendent normalement d'une interface IServiceXxx :
    * voir le journal de développement pour le détail.
*/
public class ServiceSolde implements IServiceSolde {
    private final ServicePortefeuille servicePortefeuille;

    public ServiceSolde(ServicePortefeuille servicePortefeuille) {
        this.servicePortefeuille = servicePortefeuille;
    }

    // Solde disponible = total des revenus - total des dépenses - total actuellement épargné.
    public double getSoldeDisponible() {
        double totalRevenus = 0;
        double totalDepenses = 0;

        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            if (transaction.getType() == TypeTransaction.REVENU) {
                totalRevenus += transaction.getMontant();
            } else {
                totalDepenses += transaction.getMontant();
            }
        }

        return totalRevenus - totalDepenses - getTotalEpargne();
    }

    // Somme du montant actuellement épargné sur tous les objectifs. Le calcul lui-même passe
    // par CalculEpargne (partagé avec ServiceEpargne) plutôt que d'être refait ici : le montant
    // actuel d'un objectif ne doit être calculé qu'à un seul endroit du code.
    public double getTotalEpargne() {
        double total = 0;
        for (Epargne objectif : servicePortefeuille.getDonnees().getObjectifs()) {
            total += CalculEpargne.calculerMontantActuel(objectif);
        }
        return total;
    }

    // Solde qu'on obtiendrait si cette dépense était enregistrée, sans l'enregistrer.
    // Utilisé par le contrôleur pour avertir l'utilisateur avant confirmation.
    public double soldeApresDepense(double montant) {
        return getSoldeDisponible() - montant;
    }

    // Règle de gestion "dépense > solde ⇒ autorisée avec avertissement" : le seuil (le calcul
    // qui décide s'il faut avertir) est ici, dans le service, pas dans le contrôleur. Le
    // contrôleur ne fait plus que brancher sur ce booléen, exactement comme ServiceEpargne le
    // fait déjà pour depasseraCible().
    public boolean depenseRendraSoldeNegatif(double montant) {
        return soldeApresDepense(montant) < 0;
    }
}
