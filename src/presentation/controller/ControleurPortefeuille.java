package presentation.controller;

import java.math.BigDecimal;

import application.service.interfaces.IServiceSolde;
import presentation.view.VuePrincipale;

/*
    * ControleurPortefeuille enchaîne l'écran "Voir le solde" : lit le solde disponible et le
    * total épargné auprès de ServiceSolde, les transmet à la vue.
*/
public class ControleurPortefeuille {
    private final VuePrincipale vuePrincipale;
    private final IServiceSolde serviceSolde;

    public ControleurPortefeuille(VuePrincipale vuePrincipale, IServiceSolde serviceSolde) {
        this.vuePrincipale = vuePrincipale;
        this.serviceSolde = serviceSolde;
    }

    public void afficherSolde() {
        BigDecimal soldeDisponible = serviceSolde.getSoldeDisponible();
        BigDecimal totalEpargne = serviceSolde.getTotalEpargne();
        vuePrincipale.afficherSolde(soldeDisponible, totalEpargne);
    }
}
