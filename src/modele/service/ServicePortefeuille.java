package modele.service;

import modele.entite.Epargne;
import modele.entite.MouvementEpargne;
import modele.entite.Portefeuille;
import modele.entite.Transaction;
import modele.enumeration.SensMouvement;
import modele.enumeration.TypeTransaction;
import persistance.GestionnaireFichier;

/*
    * ServicePortefeuille a trois responsabilités, et pas une de plus : détenir le
    * Portefeuille, calculer le solde disponible et le total épargné, et déclencher la
    * sauvegarde. Il n'expose aucune méthode d'accès aux transactions ou aux objectifs :
    * ce serait dupliquer l'API de l'entité au fil des besoins de chaque service.
    *
    * À la place, getDonnees() (visibilité de paquet) laisse les autres services de
    * modele.service manipuler directement le Portefeuille — mais seulement eux, puisque
    * les contrôleurs et les vues sont dans d'autres paquets et n'y ont pas accès.
*/
public class ServicePortefeuille {
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

    // Somme du montant actuellement épargné sur tous les objectifs. Calculé ici plutôt que
    // délégué à ServiceEpargne : ServiceEpargne dépend déjà de ServicePortefeuille (pour le
    // solde disponible et la sauvegarde), une dépendance dans l'autre sens créerait un cycle.
    // Même logique que getSoldeDisponible() ci-dessus, qui parcourt directement les
    // transactions plutôt que de passer par ServiceTransaction.
    public double getTotalEpargne() {
        double total = 0;
        for (Epargne objectif : portefeuille.getObjectifs()) {
            for (MouvementEpargne mouvement : objectif.getMouvements()) {
                if (mouvement.getSens() == SensMouvement.CONTRIBUTION) {
                    total += mouvement.getMontant();
                } else {
                    total -= mouvement.getMontant();
                }
            }
        }
        return total;
    }

    // Solde qu'on obtiendrait si cette dépense était enregistrée, sans l'enregistrer.
    // Utilisé par le contrôleur pour avertir l'utilisateur avant confirmation.
    public double soldeApresDepense(double montant) {
        return getSoldeDisponible() - montant;
    }

    // Seul point d'écriture du portefeuille sur le disque. Les autres services appellent
    // cette méthode après chaque opération validée, plutôt que de détenir eux-mêmes
    // GestionnaireFichier ou Portefeuille.
    public void sauvegarder() {
        gestionnaireFichier.sauvegarder(portefeuille);
    }

    // Visibilité de paquet, volontairement : seuls les services de modele.service (le même
    // paquet) peuvent appeler cette méthode. Un contrôleur ou une vue, dans un autre paquet,
    // ne peut pas y accéder — le compilateur refuse la compilation s'il essaie. C'est ce qui
    // empêche de contourner les services pour manipuler le Portefeuille directement.
    Portefeuille getDonnees() {
        return portefeuille;
    }
}
