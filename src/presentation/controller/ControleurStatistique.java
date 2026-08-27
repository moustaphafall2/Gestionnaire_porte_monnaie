package presentation.controller;

import java.time.LocalDate;

import application.dto.StatistiqueDTO;
import application.service.interfaces.IServiceStatistique;
import presentation.view.VueStatistique;

/*
    * ControleurStatistique enchaîne l'écran "Voir les statistiques" : il lit la période via
    * VueStatistique, appelle ServiceStatistique pour obtenir les totaux (déjà calculés par le
    * service, aucun calcul ici), et transmet le résultat à la vue.
    *
    * Écran en lecture seule : aucune donnée n'est modifiée, donc aucun appel à sauvegarder() et
    * aucune exception de sauvegarde à gérer, contrairement aux six écrans précédents.
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

        // La période doit être cohérente (début <= fin) : ServiceStatistique la refuse avec
        // IllegalArgumentException, comme toute donnée invalide en soi (peu importe l'état du
        // portefeuille). Attrapée ici, comme les autres erreurs métier.
        try {
            StatistiqueDTO statistiques = serviceStatistique.getStatistiques(debut, fin);
            vueStatistique.afficherTotauxParCategorie(statistiques.getTotalParCategorie());
            vueStatistique.afficherTotalRevenusEtDepenses(statistiques.getTotalRevenus(), statistiques.getTotalDepenses());
        } catch (IllegalArgumentException erreur) {
            vueStatistique.afficherErreur(erreur.getMessage());
        }
    }
}
