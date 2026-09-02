package presentation.view;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import application.dto.MouvementDTO;
import application.dto.ObjectifDTO;
import domain.enumeration.SensMouvement;

/*
    * VueEpargne affiche l'écran "Gérer mes objectifs d'épargne" : créer, contribuer, retirer,
    * consulter la progression et les mouvements, supprimer. Hérite de VueConsole pour ses
    * briques de saisie/affichage générales.
*/
public class VueEpargne extends VueConsole {

    private void afficherMenuEpargne() {
        afficherMessage("1. Créer un objectif");
        afficherMessage("2. Contribuer à un objectif");
        afficherMessage("3. Retirer d'un objectif");
        afficherMessage("4. Voir mes objectifs");
        afficherMessage("5. Supprimer un objectif");
        afficherMessage("6. Retour");
    }

    public int demanderChoixMenu() {
        afficherMenuEpargne();
        return lireEntier("Votre choix : ");
    }

    public void afficherAucunObjectif() {
        afficherMessage("Aucun objectif d'épargne pour le moment.");
    }

    public void afficherObjectifs(List<ObjectifDTO> objectifs) {
        if (objectifs.isEmpty()) {
            afficherAucunObjectif();
            return;
        }
        for (ObjectifDTO objectif : objectifs) {
            afficherMessage(String.format("%d - %s : %.2f / %.2f FCFA (%.2f%%)",
                    objectif.getId(), objectif.getNom(),
                    objectif.getMontantActuel(), objectif.getMontantCible(), objectif.getPourcentageAtteint()));
        }
    }

    public void afficherMouvements(String nomObjectif, List<MouvementDTO> mouvements) {
        if (mouvements.isEmpty()) {
            afficherMessage("Aucun mouvement pour le moment sur \"" + nomObjectif + "\".");
            return;
        }
        afficherMessage("Mouvements de \"" + nomObjectif + "\" :");
        for (MouvementDTO mouvement : mouvements) {
            afficherMessage("  " + formaterMouvement(mouvement));
        }
    }

    private String formaterMouvement(MouvementDTO mouvement) {
        String signe = (mouvement.getSens() == SensMouvement.CONTRIBUTION) ? "+" : "-";
        String libelle = (mouvement.getSens() == SensMouvement.CONTRIBUTION) ? "contribution" : "retrait";
        return "[" + mouvement.getDate().format(FORMAT_DATE) + "] " + signe + mouvement.getMontant() + " FCFA (" + libelle + ")";
    }

    public String demanderNomObjectif() {
        return lireLigne("Nom de l'objectif : ");
    }

    public double demanderMontantCible() {
        return lireMontant("Montant cible : ");
    }

    // Vide renvoie null (pas la date du jour) ; une date future est autorisée, contrairement aux
    // transactions.
    public LocalDate demanderDateLimite() {
        String saisie = lireLigne("Date limite (JJ/MM/AAAA, facultative, vide = aucune) : ");
        if (saisie.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(saisie, FORMAT_DATE);
        } catch (DateTimeParseException erreur) {
            afficherMessage("Date invalide, ignorée.");
            return null;
        }
    }

    public void afficherRecapitulatifCreation(String nom, double montantCible) {
        afficherMessage(String.format("Récapitulatif : \"%s\", cible %.2f FCFA", nom, montantCible));
    }

    public boolean demanderConfirmationCreation() {
        return confirmer("Confirmer la création de cet objectif ?");
    }

    public void afficherObjectifCree() {
        afficherMessage("Objectif créé.");
    }

    public int demanderIdentifiantObjectif() {
        return lireEntier("Identifiant de l'objectif : ");
    }

    public void afficherSoldeDisponible(double soldeDisponible) {
        afficherMessage(String.format("Solde disponible actuel : %.2f FCFA", soldeDisponible));
    }

    public double demanderMontantContribution() {
        return lireMontant("Montant à ajouter : ");
    }

    public LocalDate demanderDate() {
        return lireDate("Date (JJ/MM/AAAA, vide = aujourd'hui) : ");
    }

    public void afficherAvertissementDepassementCible() {
        afficherMessage("Attention : cette contribution dépassera le montant cible de l'objectif.");
    }

    public void afficherRecapitulatifContribution(double montant, String nomObjectif, LocalDate date) {
        afficherMessage(String.format("Récapitulatif : %.2f FCFA vers \"%s\" le %s", montant, nomObjectif, date.format(FORMAT_DATE)));
    }

    public boolean demanderConfirmationContribution() {
        return confirmer("Confirmer cette contribution ?");
    }

    public void afficherContributionEnregistree() {
        afficherMessage("Contribution enregistrée.");
    }

    public double demanderMontantRetrait() {
        return lireMontant("Montant à retirer : ");
    }

    public void afficherRecapitulatifRetrait(double montant, String nomObjectif, LocalDate date) {
        afficherMessage(String.format("Récapitulatif : %.2f FCFA retirés de \"%s\" le %s", montant, nomObjectif, date.format(FORMAT_DATE)));
    }

    public boolean demanderConfirmationRetrait() {
        return confirmer("Confirmer ce retrait ?");
    }

    public void afficherRetraitEnregistre() {
        afficherMessage("Retrait enregistré.");
    }

    public int demanderIdentifiantDetail() {
        return lireEntier("Identifiant de l'objectif à détailler (0 pour revenir) : ");
    }

    public int demanderIdentifiantSuppression() {
        return lireEntier("Identifiant de l'objectif à supprimer : ");
    }

    public boolean demanderConfirmationSuppression() {
        return confirmer("Confirmer la suppression de cet objectif ?");
    }

    public void afficherObjectifSupprime() {
        afficherMessage("Objectif supprimé.");
    }
}
