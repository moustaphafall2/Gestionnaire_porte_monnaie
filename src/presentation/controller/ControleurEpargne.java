package presentation.controller;

import java.time.LocalDate;
import java.util.List;

import domain.entity.Epargne;
import application.service.interfaces.IServiceEpargne;
import application.service.interfaces.IServicePortefeuille;
import presentation.view.VueEpargne;

/*
    * ControleurEpargne porte les cinq actions de l'écran objectifs d'épargne : créer, contribuer,
    * retirer, afficher, supprimer. Chaque méthode publique se lit de haut en bas ; la seule
    * méthode privée, afficherListeObjectifs(), est partagée par les quatre actions qui ont
    * besoin d'afficher la liste avant de continuer (voir plus bas).
    *
    * La reprise après un échec de sauvegarde n'est plus gérée ici : ErreurSauvegardeException
    * n'est attrapée nulle part dans cette classe, elle remonte jusqu'à Main. Les exceptions
    * métier (IllegalArgumentException, IllegalStateException), elles, restent attrapées
    * localement à chaque action : avant, une seule capture partagée couvrait les cinq actions
    * dans gererObjectifsEpargne() ; maintenant que chaque action est indépendante, chacune porte
    * la sienne.
*/
public class ControleurEpargne {
    private final VueEpargne vueEpargne;
    private final IServiceEpargne serviceEpargne;
    private final IServicePortefeuille servicePortefeuille;

    public ControleurEpargne(VueEpargne vueEpargne, IServiceEpargne serviceEpargne, IServicePortefeuille servicePortefeuille) {
        this.vueEpargne = vueEpargne;
        this.serviceEpargne = serviceEpargne;
        this.servicePortefeuille = servicePortefeuille;
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
            Epargne objectif = serviceEpargne.getObjectif(id);

            vueEpargne.afficherSoldeDisponible(servicePortefeuille.getSoldeDisponible());
            double montant = vueEpargne.demanderMontantContribution();

            // Règle de gestion : une contribution qui dépasse la cible reste autorisée, avec un
            // simple signalement avant confirmation (le refus ne porte que sur le solde
            // disponible, vérifié par ServiceEpargne.contribuerObjectif au moment de
            // l'enregistrement).
            if (serviceEpargne.depasseraCible(objectif, montant)) {
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
            Epargne objectif = serviceEpargne.getObjectif(id);

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
            Epargne objectif = serviceEpargne.getObjectif(id);
            vueEpargne.afficherMouvements(objectif.getNom(), objectif.getMouvements());
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

    // Récupère la liste des objectifs et la transmet entière à VueEpargne.afficherObjectifs(),
    // avec les montants actuels et pourcentages atteints déjà calculés par ServiceEpargne pour
    // toute la liste. Ni boucle ni test de liste vide ici : les deux sont dans la vue. Partagée
    // par contribuerObjectif(), retirerObjectif(), afficherObjectifs() et supprimerObjectif(),
    // qui ont toutes besoin de montrer la liste avant de demander un identifiant.
    private void afficherListeObjectifs() {
        List<Epargne> objectifs = serviceEpargne.getObjectifs();
        vueEpargne.afficherObjectifs(objectifs, serviceEpargne.getMontantsActuels(objectifs), serviceEpargne.getPourcentagesAtteints(objectifs));
    }
}
