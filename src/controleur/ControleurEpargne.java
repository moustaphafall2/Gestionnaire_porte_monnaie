package controleur;

import java.time.LocalDate;
import java.util.List;

import modele.entite.Epargne;
import modele.exception.ErreurSauvegardeException;
import modele.iService.IServiceEpargne;
import modele.iService.IServicePortefeuille;
import vue.VueEpargne;

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
public class ControleurEpargne {
    private VueEpargne vueEpargne;
    private IServiceEpargne serviceEpargne;
    private IServicePortefeuille servicePortefeuille;

    public ControleurEpargne(VueEpargne vueEpargne, IServiceEpargne serviceEpargne, IServicePortefeuille servicePortefeuille) {
        this.vueEpargne = vueEpargne;
        this.serviceEpargne = serviceEpargne;
        this.servicePortefeuille = servicePortefeuille;
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
        if (!serviceEpargne.getObjectifs().isEmpty()) {
            int id = vueEpargne.lireEntier("Identifiant de l'objectif à détailler (0 pour revenir) : ");
            if (id != 0) {
                Epargne objectif = serviceEpargne.getObjectif(id);
                vueEpargne.afficherMouvements(objectif.getNom(), objectif.getMouvements());
            }
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

    // Affiche chaque objectif avec sa progression. Le montant actuel et le pourcentage atteint
    // viennent tous les deux de ServiceEpargne : ce contrôleur ne fait que les transmettre à la
    // vue, jamais de calcul sur les mouvements ou les montants.
    private void afficherListeObjectifs() {
        List<Epargne> objectifs = serviceEpargne.getObjectifs();
        if (objectifs.isEmpty()) {
            vueEpargne.afficherAucunObjectif();
            return;
        }
        for (Epargne objectif : objectifs) {
            vueEpargne.afficherObjectif(objectif, serviceEpargne.getMontantActuel(objectif), serviceEpargne.getPourcentageAtteint(objectif));
        }
    }

    // Réessaie uniquement l'écriture sur le disque, jamais l'opération elle-même : elle a déjà
    // eu lieu en mémoire au moment où ServiceEpargne lève cette exception (voir
    // ServicePortefeuille.sauvegarder()). Tant que l'utilisateur accepte de réessayer, on
    // rappelle directement servicePortefeuille.sauvegarder() ; s'il refuse, l'application
    // continue sans bloquer, avec un message clair sur les données non encore enregistrées.
    private void confirmerNouvelleSauvegarde(ErreurSauvegardeException erreur) {
        String messageErreur = erreur.getMessage();
        while (vueEpargne.demanderNouvelleTentativeSauvegarde(messageErreur)) {
            try {
                servicePortefeuille.sauvegarder();
                vueEpargne.afficherSauvegardeReussie();
                return;
            } catch (ErreurSauvegardeException nouvelleErreur) {
                messageErreur = nouvelleErreur.getMessage();
            }
        }
        vueEpargne.afficherSauvegardeAbandonnee();
    }
}
