package application.service.implementation;

import domain.entity.Epargne;
import domain.entity.Portefeuille;
import domain.entity.Transaction;
import domain.enumeration.TypeTransaction;
import application.service.interfaces.IServicePortefeuille;
import infrastructure.persistence.GestionnaireFichier;

/*
    * ServicePortefeuille a trois responsabilités, et pas une de plus : détenir le
    * Portefeuille, calculer le solde disponible et le total épargné, et déclencher la
    * sauvegarde. Il n'expose aucune méthode d'accès aux transactions ou aux objectifs :
    * ce serait dupliquer l'API de l'entité au fil des besoins de chaque service.
    *
    * À la place, getDonnees() (visibilité de paquet) laisse les autres services de
    * application.service.implementation manipuler directement le Portefeuille — mais seulement eux, puisque
    * les contrôleurs et les vues sont dans d'autres paquets et n'y ont pas accès.
*/
public class ServicePortefeuille implements IServicePortefeuille {
    private Portefeuille portefeuille;
    private GestionnaireFichier gestionnaireFichier;

    public ServicePortefeuille(Portefeuille portefeuille, GestionnaireFichier gestionnaireFichier) {
        this.portefeuille = portefeuille;
        this.gestionnaireFichier = gestionnaireFichier;
    }

    // Solde disponible = total des revenus - total des dépenses - total actuellement épargné.
    public double getSoldeDisponible() {
        double totalRevenus = 0;
        double totalDepenses = 0;

        for (Transaction transaction : portefeuille.getTransactions()) {
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
        for (Epargne objectif : portefeuille.getObjectifs()) {
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

    // Seul point d'écriture du portefeuille sur le disque. Les autres services appellent
    // cette méthode après chaque opération validée, plutôt que de détenir eux-mêmes
    // GestionnaireFichier ou Portefeuille.
    public void sauvegarder() {
        gestionnaireFichier.sauvegarder(portefeuille);
    }

    // Visibilité de paquet, volontairement : seuls les services de application.service.implementation (le même
    // paquet) peuvent appeler cette méthode. Un contrôleur ou une vue, dans un autre paquet,
    // ne peut pas y accéder — le compilateur refuse la compilation s'il essaie. C'est ce qui
    // empêche de contourner les services pour manipuler le Portefeuille directement.
    Portefeuille getDonnees() {
        return portefeuille;
    }
}
