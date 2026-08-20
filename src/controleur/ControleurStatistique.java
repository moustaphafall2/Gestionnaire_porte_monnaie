package controleur;

import java.time.LocalDate;
import java.util.Map;

import modele.enumeration.Categorie;
import modele.service.ServiceStatistique;
import vue.VueStatistique;

/*
    * ControleurStatistique enchaîne l'écran "Voir les statistiques" : il lit la période via
    * VueStatistique, appelle ServiceStatistique pour obtenir les totaux (déjà calculés par le
    * service, aucun calcul ici), et transmet le résultat à la vue.
    *
    * Écran en lecture seule : aucune donnée n'est modifiée, donc aucun appel à sauvegarder() et
    * aucune exception de sauvegarde à gérer, contrairement aux six écrans précédents.
*/
public class ControleurStatistique {
    private VueStatistique vueStatistique;
    private ServiceStatistique serviceStatistique;

    public ControleurStatistique(VueStatistique vueStatistique, ServiceStatistique serviceStatistique) {
        this.vueStatistique = vueStatistique;
        this.serviceStatistique = serviceStatistique;
    }

    // ----- 7. Statistiques -----

    public void gererStatistiques() {
        LocalDate debut = vueStatistique.lireDate("Date de début (JJ/MM/AAAA) : ");
        LocalDate fin = vueStatistique.lireDate("Date de fin (JJ/MM/AAAA) : ");

        Map<Categorie, Double> totauxParCategorie = serviceStatistique.getTotalParCategorie(debut, fin);
        vueStatistique.afficherTotauxParCategorie(totauxParCategorie);

        double[] totaux = serviceStatistique.getTotalRevenusEtDepenses(debut, fin);
        vueStatistique.afficherTotalRevenusEtDepenses(totaux[0], totaux[1]);
    }
}
