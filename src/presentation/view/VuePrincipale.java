package presentation.view;

/*
    * VuePrincipale affiche le menu principal et l'écran "voir le solde". Elle hérite de
    * VueConsole pour ses briques de saisie/affichage générales (lireEntier, afficherMessage...)
    * et ajoute les affichages propres à cet écran : le contrôleur ne construit aucun texte, il
    * ne fait que lui transmettre les valeurs à afficher.
*/
public class VuePrincipale extends VueConsole {

    public void afficherMenuPrincipal() {
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

    // Affichée quand l'opération a réussi en mémoire mais que l'écriture sur le disque a
    // échoué : l'utilisateur doit savoir que ses données ne sont pas encore en sécurité, sans
    // jamais voir la trace de l'exception d'origine.
    public void afficherEchecSauvegarde(String messageErreur) {
        afficherMessage("Attention : " + messageErreur);
        afficherMessage("L'opération a bien été effectuée en mémoire, mais pas encore enregistrée sur le disque.");
    }
}
