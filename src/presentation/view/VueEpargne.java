package presentation.view;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import application.dto.MouvementDTO;
import application.dto.ObjectifDTO;
import domain.enumeration.SensMouvement;

/*
    * VueEpargne affiche l'écran "Gérer mes objectifs d'épargne" : créer un objectif, contribuer,
    * retirer, consulter la progression et les mouvements, supprimer. Comme VueTransaction, elle
    * hérite de VueConsole pour ses briques de saisie/affichage générales et n'ajoute que ce qui
    * est propre à cet écran : le contrôleur ne construit aucun texte à afficher, il ne fait que
    * lui transmettre les valeurs obtenues des services.
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

    // Affiche le sous-menu et lit le choix en un seul appel : Main n'a plus besoin d'une méthode
    // intermédiaire pour ça, il fait directement son switch sur la valeur renvoyée ici.
    public int demanderChoixMenu() {
        afficherMenuEpargne();
        return lireEntier("Votre choix : ");
    }

    public void afficherAucunObjectif() {
        afficherMessage("Aucun objectif d'épargne pour le moment.");
    }

    // Traitement rapatrié depuis ControleurEpargne.afficherListeObjectifs() : tester si la liste
    // est vide et boucler pour afficher chaque ligne sont des décisions de présentation (quel
    // message montrer, comment parcourir pour mettre en forme), pas des règles métier. Depuis
    // l'étape DTO, ServiceEpargne fournit une seule liste d'ObjectifDTO, chacun portant déjà son
    // montant actuel et son pourcentage atteint : plus de listes parallèles à faire avancer
    // ensemble.
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

    // Détail des contributions/retraits d'un objectif. Ces mouvements ne sont jamais mélangés à
    // l'historique des transactions : ce n'est ni une dépense ni un revenu.
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

    // Reprend la mise en forme qui vivait auparavant dans MouvementEpargne.toString() : depuis
    // l'introduction des DTO, ni l'entité ni MouvementDTO ne portent de mise en forme, c'est
    // entièrement le rôle de la vue.
    private String formaterMouvement(MouvementDTO mouvement) {
        String signe = (mouvement.getSens() == SensMouvement.CONTRIBUTION) ? "+" : "-";
        String libelle = (mouvement.getSens() == SensMouvement.CONTRIBUTION) ? "contribution" : "retrait";
        return "[" + mouvement.getDate().format(FORMAT_DATE) + "] " + signe + mouvement.getMontant() + " FCFA (" + libelle + ")";
    }

    // Le nom se lit en texte libre : aucune contrainte de format, seule Epargne validera qu'il
    // n'est pas vide.
    public String demanderNomObjectif() {
        return lireLigne("Nom de l'objectif : ");
    }

    public double demanderMontantCible() {
        return lireMontant("Montant cible : ");
    }

    // Variante de lireDate() pour un champ facultatif (la date limite d'un objectif) : une
    // saisie vide renvoie null plutôt que la date du jour, et une date future est autorisée
    // (un objectif peut viser une échéance à venir, contrairement à une transaction).
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

    // Même texte pour contribuerObjectif() et retirerObjectif() : les deux désignent l'objectif
    // concerné de la même façon, avant de demander autre chose de spécifique à l'opération.
    public int demanderIdentifiantObjectif() {
        return lireEntier("Identifiant de l'objectif : ");
    }

    public void afficherSoldeDisponible(double soldeDisponible) {
        afficherMessage(String.format("Solde disponible actuel : %.2f FCFA", soldeDisponible));
    }

    public double demanderMontantContribution() {
        return lireMontant("Montant à ajouter : ");
    }

    // Même texte pour contribuerObjectif() et retirerObjectif() : la date de l'opération se
    // demande de la même façon dans les deux cas.
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
