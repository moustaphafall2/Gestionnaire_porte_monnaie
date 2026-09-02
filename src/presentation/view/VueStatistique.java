package presentation.view;

import java.time.LocalDate;
import java.util.Map;

import domain.enumeration.Categorie;

/*
    * VueStatistique affiche l'écran "Voir les statistiques" : total dépensé par catégorie et
    * comparaison revenus/dépenses sur une période.
*/
public class VueStatistique extends VueConsole {

    public LocalDate demanderDateDebut() {
        return lireDate("Date de début (JJ/MM/AAAA) : ");
    }

    public LocalDate demanderDateFin() {
        return lireDate("Date de fin (JJ/MM/AAAA) : ");
    }

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
