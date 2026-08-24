package presentation.view;

/*
    * VuePrincipale affiche le menu principal et l'écran "voir le solde". Elle hérite de
    * VueConsole pour ses briques de saisie/affichage générales (lireEntier, afficherMessage...),
    * gardées internes à cette classe : Main ne connaît le texte d'aucune invite, il appelle
    * demanderChoix() et ne reçoit que le choix saisi.
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

    // Affiche le menu principal et lit le choix en un seul appel, même patron que
    // VueCategorie.demanderChoixMenu() : Main fait directement son switch sur la valeur
    // renvoyée ici, sans jamais connaître le texte de l'invite.
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
