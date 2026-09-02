package presentation.controller;

import java.time.LocalDate;

import application.dto.StatistiqueDTO;
import application.service.interfaces.IServiceStatistique;
import presentation.view.VueStatistique;

/*
    * ControleurStatistique enchaîne l'écran "Voir les statistiques" : lit la période, appelle
    * ServiceStatistique, transmet le résultat à la vue.
*/
public class ControleurStatistique {
    private final VueStatistique vueStatistique;
    private final IServiceStatistique serviceStatistique;

    public ControleurStatistique(VueStatistique vueStatistique, IServiceStatistique serviceStatistique) {
        this.vueStatistique = vueStatistique;
        this.serviceStatistique = serviceStatistique;
    }

    public void afficherStatistiques() {
        LocalDate debut = vueStatistique.demanderDateDebut();
        LocalDate fin = vueStatistique.demanderDateFin();

        try {
            StatistiqueDTO statistiques = serviceStatistique.getStatistiques(debut, fin);
            vueStatistique.afficherTotauxParCategorie(statistiques.getTotalParCategorie());
            vueStatistique.afficherTotalRevenusEtDepenses(statistiques.getTotalRevenus(), statistiques.getTotalDepenses());
        } catch (IllegalArgumentException erreur) {
            vueStatistique.afficherErreur(erreur.getMessage());
        }
    }
}
