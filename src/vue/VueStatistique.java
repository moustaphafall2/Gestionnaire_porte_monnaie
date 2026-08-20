package vue;

import java.util.Map;

import modele.enumeration.Categorie;

/*
    * VueStatistique affiche l'écran "Voir les statistiques" : total dépensé par catégorie et
    * comparaison revenus/dépenses sur une période. Comme les autres vues d'écran, elle hérite
    * de VueConsole pour ses briques de saisie/affichage générales (lireDate...) et n'ajoute que
    * ce qui est propre à cet écran : le contrôleur ne construit aucun texte à afficher, il ne
    * fait que lui transmettre les totaux déjà calculés par ServiceStatistique.
*/
public class VueStatistique extends VueConsole {

    public void afficherTotauxParCategorie(Map<Categorie, Double> totaux) {
        afficherMessage("Total dépensé par catégorie :");
        if (totaux.isEmpty()) {
            afficherMessage("  Aucune dépense sur cette période.");
            return;
        }
        for (Map.Entry<Categorie, Double> entree : totaux.entrySet()) {
            afficherMessage(String.format("  %s : %.2f FCFA", entree.getKey().getLibelle(), entree.getValue()));
        }
    }

    public void afficherTotalRevenusEtDepenses(double totalRevenus, double totalDepenses) {
        afficherMessage(String.format("Total des revenus : %.2f FCFA", totalRevenus));
        afficherMessage(String.format("Total des dépenses : %.2f FCFA", totalDepenses));
    }
}
