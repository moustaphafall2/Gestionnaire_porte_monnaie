package presentation.view;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import domain.entity.Epargne;
import domain.entity.MouvementEpargne;

/*
    * VueEpargne affiche l'écran "Gérer mes objectifs d'épargne" : créer un objectif, contribuer,
    * retirer, consulter la progression et les mouvements, supprimer. Comme VueTransaction, elle
    * hérite de VueConsole pour ses briques de saisie/affichage générales et n'ajoute que ce qui
    * est propre à cet écran : le contrôleur ne construit aucun texte à afficher, il ne fait que
    * lui transmettre les valeurs obtenues des services.
*/
public class VueEpargne extends VueConsole {

    public void afficherMenuEpargne() {
        afficherMessage("1. Créer un objectif");
        afficherMessage("2. Contribuer à un objectif");
        afficherMessage("3. Retirer d'un objectif");
        afficherMessage("4. Voir mes objectifs");
        afficherMessage("5. Supprimer un objectif");
        afficherMessage("6. Retour");
    }

    public void afficherAucunObjectif() {
        afficherMessage("Aucun objectif d'épargne pour le moment.");
    }

    // Une ligne de progression pour un objectif : le contrôleur fournit le montant actuel et le
    // pourcentage atteint (calculés par ServiceEpargne), la vue ne fait que les mettre en forme.
    public void afficherObjectif(Epargne objectif, double montantActuel, double pourcentageAtteint) {
        afficherMessage(String.format("%d - %s : %.2f / %.2f FCFA (%.2f%%)", objectif.getId(), objectif.getNom(),
                montantActuel, objectif.getMontantCible(), pourcentageAtteint));
    }

    // Traitement rapatrié depuis ControleurEpargne.afficherListeObjectifs() : tester si la liste
    // est vide et boucler pour afficher chaque ligne sont des décisions de présentation (quel
    // message montrer, comment parcourir pour mettre en forme), pas des règles métier. Les trois
    // listes reçues sont déjà calculées par ServiceEpargne et vont ensemble, index par index :
    // c'est la vue qui les assemble ligne par ligne via afficherObjectif().
    public void afficherObjectifs(List<Epargne> objectifs, List<Double> montantsActuels, List<Double> pourcentagesAtteints) {
        if (objectifs.isEmpty()) {
            afficherAucunObjectif();
            return;
        }
        for (int i = 0; i < objectifs.size(); i++) {
            afficherObjectif(objectifs.get(i), montantsActuels.get(i), pourcentagesAtteints.get(i));
        }
    }

    // Détail des contributions/retraits d'un objectif. Ces mouvements ne sont jamais mélangés à
    // l'historique des transactions : ce n'est ni une dépense ni un revenu.
    public void afficherMouvements(String nomObjectif, List<MouvementEpargne> mouvements) {
        if (mouvements.isEmpty()) {
            afficherMessage("Aucun mouvement pour le moment sur \"" + nomObjectif + "\".");
            return;
        }
        afficherMessage("Mouvements de \"" + nomObjectif + "\" :");
        for (MouvementEpargne mouvement : mouvements) {
            afficherMessage("  " + mouvement);
        }
    }

    // Variante de lireDate() pour un champ facultatif (la date limite d'un objectif) : une
    // saisie vide renvoie null plutôt que la date du jour, et une date future est autorisée
    // (un objectif peut viser une échéance à venir, contrairement à une transaction).
    public LocalDate lireDateLimite(String message) {
        String saisie = lireLigne(message);
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

    public void afficherObjectifCree() {
        afficherMessage("Objectif créé.");
    }

    public void afficherSoldeDisponible(double soldeDisponible) {
        afficherMessage(String.format("Solde disponible actuel : %.2f FCFA", soldeDisponible));
    }

    public void afficherAvertissementDepassementCible() {
        afficherMessage("Attention : cette contribution dépassera le montant cible de l'objectif.");
    }

    public void afficherRecapitulatifContribution(double montant, String nomObjectif, LocalDate date) {
        afficherMessage(String.format("Récapitulatif : %.2f FCFA vers \"%s\" le %s", montant, nomObjectif, date.format(FORMAT_DATE)));
    }

    public void afficherContributionEnregistree() {
        afficherMessage("Contribution enregistrée.");
    }

    public void afficherRecapitulatifRetrait(double montant, String nomObjectif, LocalDate date) {
        afficherMessage(String.format("Récapitulatif : %.2f FCFA retirés de \"%s\" le %s", montant, nomObjectif, date.format(FORMAT_DATE)));
    }

    public void afficherRetraitEnregistre() {
        afficherMessage("Retrait enregistré.");
    }

    public void afficherObjectifSupprime() {
        afficherMessage("Objectif supprimé.");
    }
}
