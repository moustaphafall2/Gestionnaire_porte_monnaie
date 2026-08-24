package presentation.controller;

import application.service.interfaces.IServicePortefeuille;
import presentation.view.VuePrincipale;

/*
    * ControleurPortefeuille enchaîne l'écran "Voir le solde" : il lit le solde disponible et le
    * total épargné auprès de ServicePortefeuille, et les transmet à la vue. C'est le seul
    * traitement qui restait à ControleurPrincipal une fois son rôle d'aiguilleur reparti dans
    * Main — il devient donc son propre contrôleur, un par domaine comme les quatre autres.
*/
public class ControleurPortefeuille {
    private final VuePrincipale vuePrincipale;
    private final IServicePortefeuille servicePortefeuille;

    public ControleurPortefeuille(VuePrincipale vuePrincipale, IServicePortefeuille servicePortefeuille) {
        this.vuePrincipale = vuePrincipale;
        this.servicePortefeuille = servicePortefeuille;
    }

    public void afficherSolde() {
        double soldeDisponible = servicePortefeuille.getSoldeDisponible();
        double totalEpargne = servicePortefeuille.getTotalEpargne();
        vuePrincipale.afficherSolde(soldeDisponible, totalEpargne);
    }
}
