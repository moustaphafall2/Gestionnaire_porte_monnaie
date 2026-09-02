package presentation.view;

/*
    * VuePrincipale affiche le menu principal et l'écran "voir le solde".
*/
public class VuePrincipale extends VueConsole {

    private void afficherMenuPrincipal() {
        afficherMessage("");
        afficherMessage("==== GESTION DE PORTE-MONNAIE ====");
        afficherMessage("1. Voir le solde");
        afficherMessage("2. Ajouter une dépense");
        afficherMessage("3. Ajouter un revenu");
        afficherMessage("4. Voir l'historique des transactions");
        afficherMessage("5. Gérer mes objectifs d'épargne");
        afficherMessage("6. Gérer mes catégories");
        afficherMessage("7. Voir les statistiques");
        afficherMessage("8. Quitter");
    }

    public int demanderChoix() {
        afficherMenuPrincipal();
        return lireEntier("Votre choix : ");
    }

    public void afficherSolde(double soldeDisponible, double totalEpargne) {
        afficherMessage(String.format("Solde disponible : %.2f FCFA", soldeDisponible));
        afficherMessage(String.format("Total épargné : %.2f FCFA", totalEpargne));
    }

    public void afficherChoixInvalide() {
        afficherMessage("Choix invalide, veuillez recommencer.");
    }

    public void afficherAuRevoir() {
        afficherMessage("Au revoir !");
    }
}
