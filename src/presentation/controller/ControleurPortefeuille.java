package presentation.controller;

import application.service.interfaces.IServiceSolde;
import presentation.view.VuePrincipale;

/*
    * ControleurPortefeuille enchaîne l'écran "Voir le solde" : il lit le solde disponible et le
    * total épargné auprès de ServiceSolde, et les transmet à la vue. C'est le seul traitement
    * qui restait à ControleurPrincipal une fois son rôle d'aiguilleur reparti dans Main — il
    * devient donc son propre contrôleur, un par domaine comme les quatre autres.
*/
public class ControleurPortefeuille {
    private final VuePrincipale vuePrincipale;
    private final IServiceSolde serviceSolde;

    public ControleurPortefeuille(VuePrincipale vuePrincipale, IServiceSolde serviceSolde) {
        this.vuePrincipale = vuePrincipale;
        this.serviceSolde = serviceSolde;
    }

    public void afficherSolde() {
        double soldeDisponible = serviceSolde.getSoldeDisponible();
        double totalEpargne = serviceSolde.getTotalEpargne();
        vuePrincipale.afficherSolde(soldeDisponible, totalEpargne);
    }
}
