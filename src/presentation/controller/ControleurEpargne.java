package presentation.controller;

import java.time.LocalDate;

import application.dto.ObjectifDTO;
import application.service.interfaces.IServiceEpargne;
import application.service.interfaces.IServiceSolde;
import presentation.view.VueEpargne;

/*
    * ControleurEpargne porte les cinq actions de l'écran objectifs d'épargne : créer, contribuer,
    * retirer, afficher, supprimer.
*/
public class ControleurEpargne {
    private final VueEpargne vueEpargne;
    private final IServiceEpargne serviceEpargne;
    private final IServiceSolde serviceSolde;

    public ControleurEpargne(VueEpargne vueEpargne, IServiceEpargne serviceEpargne, IServiceSolde serviceSolde) {
        this.vueEpargne = vueEpargne;
        this.serviceEpargne = serviceEpargne;
        this.serviceSolde = serviceSolde;
    }

    public void creerObjectif() {
        String nom = vueEpargne.demanderNomObjectif();
        double montantCible = vueEpargne.demanderMontantCible();
        LocalDate dateLimite = vueEpargne.demanderDateLimite();

        vueEpargne.afficherRecapitulatifCreation(nom, montantCible);
        if (!vueEpargne.demanderConfirmationCreation()) {
            vueEpargne.afficherOperationAnnulee();
            return;
        }

        try {
            serviceEpargne.creerObjectif(nom, montantCible, dateLimite);
            vueEpargne.afficherObjectifCree();
        } catch (IllegalArgumentException | IllegalStateException erreur) {
            vueEpargne.afficherErreur(erreur.getMessage());
        }
    }

    public void contribuerObjectif() {
        afficherListeObjectifs();
        int id = vueEpargne.demanderIdentifiantObjectif();

        try {
            ObjectifDTO objectif = serviceEpargne.getObjectif(id);

            vueEpargne.afficherSoldeDisponible(serviceSolde.getSoldeDisponible());
            double montant = vueEpargne.demanderMontantContribution();

            // Règle de gestion : un dépassement de la cible n'est pas une erreur, simple signalement.
            if (serviceEpargne.depasseraCible(id, montant)) {
                vueEpargne.afficherAvertissementDepassementCible();
            }

            LocalDate date = vueEpargne.demanderDate();
            vueEpargne.afficherRecapitulatifContribution(montant, objectif.getNom(), date);
            if (!vueEpargne.demanderConfirmationContribution()) {
                vueEpargne.afficherOperationAnnulee();
                return;
            }

            serviceEpargne.contribuerObjectif(id, montant, date);
            vueEpargne.afficherContributionEnregistree();
        } catch (IllegalArgumentException | IllegalStateException erreur) {
            vueEpargne.afficherErreur(erreur.getMessage());
        }
    }

    public void retirerObjectif() {
        afficherListeObjectifs();
        int id = vueEpargne.demanderIdentifiantObjectif();

        try {
            ObjectifDTO objectif = serviceEpargne.getObjectif(id);

            double montant = vueEpargne.demanderMontantRetrait();
            LocalDate date = vueEpargne.demanderDate();
            vueEpargne.afficherRecapitulatifRetrait(montant, objectif.getNom(), date);
            if (!vueEpargne.demanderConfirmationRetrait()) {
                vueEpargne.afficherOperationAnnulee();
                return;
            }

            serviceEpargne.retirerObjectif(id, montant, date);
            vueEpargne.afficherRetraitEnregistre();
        } catch (IllegalArgumentException | IllegalStateException erreur) {
            vueEpargne.afficherErreur(erreur.getMessage());
        }
    }

    public void afficherObjectifs() {
        afficherListeObjectifs();
        int id = vueEpargne.demanderIdentifiantDetail();
        if (id == 0) {
            return;
        }

        try {
            ObjectifDTO objectif = serviceEpargne.getObjectif(id);
            vueEpargne.afficherMouvements(objectif.getNom(), serviceEpargne.getMouvements(id));
        } catch (IllegalArgumentException | IllegalStateException erreur) {
            vueEpargne.afficherErreur(erreur.getMessage());
        }
    }

    public void supprimerObjectif() {
        afficherListeObjectifs();
        int id = vueEpargne.demanderIdentifiantSuppression();

        try {
            if (vueEpargne.demanderConfirmationSuppression()) {
                serviceEpargne.supprimerObjectif(id);
                vueEpargne.afficherObjectifSupprime();
            } else {
                vueEpargne.afficherOperationAnnulee();
            }
        } catch (IllegalArgumentException | IllegalStateException erreur) {
            vueEpargne.afficherErreur(erreur.getMessage());
        }
    }

    private void afficherListeObjectifs() {
        vueEpargne.afficherObjectifs(serviceEpargne.getObjectifs());
    }
}
