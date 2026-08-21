package presentation.controller;

import java.time.LocalDate;
import java.util.List;

import domain.entity.Epargne;
import exception.ErreurSauvegardeException;
import application.service.interfaces.IServiceEpargne;
import application.service.interfaces.IServicePortefeuille;
import presentation.view.VueEpargne;

/*
    * ControleurEpargne enchaîne l'écran "Gérer mes objectifs d'épargne" : il lit les saisies via
    * VueEpargne, applique les règles en appelant ServiceEpargne (création, contribution,
    * retrait, suppression, calculs de progression) et ServicePortefeuille (solde disponible), et
    * transmet le résultat à la vue. Il n'affiche jamais rien lui-même et ne contient aucun
    * calcul métier : le montant actuel, le pourcentage atteint et le dépassement de cible
    * viennent tous de ServiceEpargne.
    *
    * Toutes les opérations sauf la simple consultation modifient réellement les données du
    * portefeuille. Si la sauvegarde échoue après une opération déjà appliquée en mémoire (voir
    * ErreurSauvegardeException), le contrôleur propose à l'utilisateur de réessayer l'écriture
    * sur le disque, tant qu'il l'accepte, sans jamais rejouer l'opération elle-même. S'il
    * refuse, l'application continue normalement : ce n'est pas bloquant.
*/
public class ControleurEpargne extends ControleurConsole {
    private VueEpargne vueEpargne;
    private IServiceEpargne serviceEpargne;

    public ControleurEpargne(VueEpargne vueEpargne, IServiceEpargne serviceEpargne, IServicePortefeuille servicePortefeuille) {
        super(vueEpargne, servicePortefeuille);
        this.vueEpargne = vueEpargne;
        this.serviceEpargne = serviceEpargne;
    }

    // ----- 5. Objectifs d'épargne -----

    public void gererObjectifsEpargne() {
        vueEpargne.afficherMenuEpargne();
        int choix = vueEpargne.lireEntier("Votre choix : ");

        try {
            switch (choix) {
                case 1 -> gererCreationObjectif();
                case 2 -> gererContribution();
                case 3 -> gererRetrait();
                case 4 -> gererConsultationObjectifs();
                case 5 -> gererSuppressionObjectif();
                default -> { }
            }
        } catch (ErreurSauvegardeException erreur) {
            confirmerNouvelleSauvegarde(erreur);
        } catch (IllegalArgumentException | IllegalStateException erreur) {
            vueEpargne.afficherErreur(erreur.getMessage());
        }
    }

    private void gererCreationObjectif() {
        String nom = vueEpargne.lireLigne("Nom de l'objectif : ");
        double montantCible = vueEpargne.lireMontant("Montant cible : ");
        LocalDate dateLimite = vueEpargne.lireDateLimite("Date limite (JJ/MM/AAAA, facultative, vide = aucune) : ");

        vueEpargne.afficherRecapitulatifCreation(nom, montantCible);
        if (!vueEpargne.confirmer("Confirmer la création de cet objectif ?")) {
            vueEpargne.afficherOperationAnnulee();
            return;
        }

        serviceEpargne.creerObjectif(nom, montantCible, dateLimite);
        vueEpargne.afficherObjectifCree();
    }

    private void gererContribution() {
        afficherListeObjectifs();
        int id = vueEpargne.lireEntier("Identifiant de l'objectif : ");
        Epargne objectif = serviceEpargne.getObjectif(id);

        double montant = vueEpargne.lireMontant("Montant à ajouter : ");
        vueEpargne.afficherSoldeDisponible(servicePortefeuille.getSoldeDisponible());

        // Règle de gestion : une contribution qui dépasse la cible reste autorisée, avec un
        // simple signalement avant confirmation (le refus ne porte que sur le solde disponible,
        // vérifié par ServiceEpargne.contribuerObjectif au moment de l'enregistrement).
        if (serviceEpargne.depasseraCible(objectif, montant)) {
            vueEpargne.afficherAvertissementDepassementCible();
        }

        LocalDate date = vueEpargne.lireDate("Date (JJ/MM/AAAA, vide = aujourd'hui) : ");
        vueEpargne.afficherRecapitulatifContribution(montant, objectif.getNom(), date);
        if (!vueEpargne.confirmer("Confirmer cette contribution ?")) {
            vueEpargne.afficherOperationAnnulee();
            return;
        }

        serviceEpargne.contribuerObjectif(id, montant, date);
        vueEpargne.afficherContributionEnregistree();
    }

    private void gererRetrait() {
        afficherListeObjectifs();
        int id = vueEpargne.lireEntier("Identifiant de l'objectif : ");
        Epargne objectif = serviceEpargne.getObjectif(id);

        double montant = vueEpargne.lireMontant("Montant à retirer : ");
        LocalDate date = vueEpargne.lireDate("Date (JJ/MM/AAAA, vide = aujourd'hui) : ");
        vueEpargne.afficherRecapitulatifRetrait(montant, objectif.getNom(), date);
        if (!vueEpargne.confirmer("Confirmer ce retrait ?")) {
            vueEpargne.afficherOperationAnnulee();
            return;
        }

        serviceEpargne.retirerObjectif(id, montant, date);
        vueEpargne.afficherRetraitEnregistre();
    }

    private void gererConsultationObjectifs() {
        afficherListeObjectifs();
        if (!serviceEpargne.aAuMoinsUnObjectif()) {
            return;
        }
        int id = vueEpargne.lireEntier("Identifiant de l'objectif à détailler (0 pour revenir) : ");
        if (id != 0) {
            Epargne objectif = serviceEpargne.getObjectif(id);
            vueEpargne.afficherMouvements(objectif.getNom(), objectif.getMouvements());
        }
    }

    private void gererSuppressionObjectif() {
        afficherListeObjectifs();
        int id = vueEpargne.lireEntier("Identifiant de l'objectif à supprimer : ");
        if (vueEpargne.confirmer("Confirmer la suppression de cet objectif ?")) {
            serviceEpargne.supprimerObjectif(id);
            vueEpargne.afficherObjectifSupprime();
        } else {
            vueEpargne.afficherOperationAnnulee();
        }
    }

    // Récupère la liste des objectifs et la transmet entière à VueEpargne.afficherObjectifs(),
    // avec les montants actuels et pourcentages atteints déjà calculés par ServiceEpargne pour
    // toute la liste. Ni boucle ni test de liste vide ici : les deux sont dans la vue. Un appelant
    // qui a besoin de savoir s'il existe au moins un objectif (gererConsultationObjectifs)
    // interroge serviceEpargne.aAuMoinsUnObjectif() plutôt que d'inspecter une liste renvoyée ici.
    private void afficherListeObjectifs() {
        List<Epargne> objectifs = serviceEpargne.getObjectifs();
        vueEpargne.afficherObjectifs(objectifs, serviceEpargne.getMontantsActuels(objectifs), serviceEpargne.getPourcentagesAtteints(objectifs));
    }
}
