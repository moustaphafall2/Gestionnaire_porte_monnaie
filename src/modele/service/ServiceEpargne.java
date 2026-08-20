package modele.service;

import java.time.LocalDate;
import java.util.List;

import modele.entite.Epargne;
import modele.entite.MouvementEpargne;
import modele.entite.Portefeuille;
import modele.enumeration.SensMouvement;

/*
    * ServiceEpargne porte désormais toutes les règles de gestion des objectifs d'épargne :
    * création, contribution, retrait, suppression, et les calculs de progression (montant
    * actuel, pourcentage atteint, cible dépassée...). Cette logique vivait auparavant dans
    * Epargne et Portefeuille ; Epargne ne garde plus que sa structure (attributs, constructeur,
    * getters, ajouterMouvement), et Portefeuille se limite à l'accès par identifiant
    * (getObjectif), un accès par clé et non un calcul.
    *
    * Comme les autres services, il ne détient jamais Portefeuille directement : il passe par
    * servicePortefeuille.getDonnees() pour lire ou modifier les objectifs, et par
    * sauvegarder() pour écrire le résultat.
*/
public class ServiceEpargne {
    // Les montants sont des FCFA sans centimes, mais restent des double : deux montants
    // "égaux" en théorie peuvent différer d'une poussière après une suite d'additions/
    // soustractions. On compare donc à un epsilon près plutôt qu'avec ==.
    private static final double EPSILON = 0.01;

    private ServicePortefeuille servicePortefeuille;

    public ServiceEpargne(ServicePortefeuille servicePortefeuille) {
        this.servicePortefeuille = servicePortefeuille;
    }

    // Montant actuellement épargné sur cet objectif. Le calcul passe par CalculEpargne, partagé
    // avec ServicePortefeuille (getTotalEpargne), pour qu'il n'existe qu'à un seul endroit.
    public double getMontantActuel(Epargne objectif) {
        return CalculEpargne.calculerMontantActuel(objectif);
    }

    public double getPourcentageAtteint(Epargne objectif) {
        return (getMontantActuel(objectif) / objectif.getMontantCible()) * 100;
    }

    // Indique si une contribution de ce montant ferait dépasser le montant cible. Dépasser la
    // cible reste autorisé (règle de gestion) : c'est à l'appelant (Menu) de décider s'il
    // signale ce dépassement avant de confirmer l'opération.
    public boolean depasseraCible(Epargne objectif, double montant) {
        return getMontantActuel(objectif) + montant > objectif.getMontantCible();
    }

    // Condition nécessaire à la suppression de l'objectif
    public boolean estVide(Epargne objectif) {
        return Math.abs(getMontantActuel(objectif)) < EPSILON;
    }

    public boolean estAtteint(Epargne objectif) {
        return getMontantActuel(objectif) >= objectif.getMontantCible();
    }

    // Liste complète des objectifs, utilisée par ControleurEpargne pour les afficher (avec leur
    // progression, calculée à part par getMontantActuel/getPourcentageAtteint) avant de demander
    // un identifiant à l'utilisateur.
    public List<Epargne> getObjectifs() {
        return servicePortefeuille.getDonnees().getObjectifs();
    }

    // Recherche publique d'un objectif par id, utilisée par ControleurEpargne pour récupérer
    // l'objectif choisi (nom, mouvements...) avant de contribuer, retirer ou en afficher le
    // détail.
    public Epargne getObjectif(int idObjectif) {
        return servicePortefeuille.getDonnees().getObjectif(idObjectif);
    }

    // Crée un nouvel objectif et l'ajoute au portefeuille. Le compteur d'identifiants reste
    // dans Portefeuille (donc sauvegardé), pour la même raison que genererIdTransaction() :
    // repartir de zéro au redémarrage créerait des doublons.
    public Epargne creerObjectif(String nom, double montantCible, LocalDate dateLimite) {
        Portefeuille portefeuille = servicePortefeuille.getDonnees();
        Epargne objectif = new Epargne(portefeuille.genererIdObjectif(), nom, montantCible, dateLimite);
        portefeuille.ajouterObjectif(objectif);
        servicePortefeuille.sauvegarder();
        return objectif;
    }

    // Un objectif d'épargne fonctionne comme un coffre : impossible d'y placer une somme
    // dont on ne dispose pas. La vérification se fait ici, et pas dans Epargne, car Epargne
    // ne connaît pas le solde disponible du portefeuille. Refusé à cause de l'état actuel
    // du portefeuille (pas d'une donnée invalide en soi) : IllegalStateException.
    public void contribuerObjectif(int idObjectif, double montant, LocalDate date) {
        double soldeDisponible = servicePortefeuille.getSoldeDisponible();
        if (montant > soldeDisponible) {
            throw new IllegalStateException("Le montant de la contribution dépasse le solde disponible ("
                    + soldeDisponible + " FCFA).");
        }

        Epargne objectif = servicePortefeuille.getDonnees().getObjectif(idObjectif);
        objectif.ajouterMouvement(new MouvementEpargne(montant, SensMouvement.CONTRIBUTION, date));
        servicePortefeuille.sauvegarder();
    }

    // Le retrait est refusé si le montant demandé dépasse ce qui est réellement épargné : ce
    // n'est pas le montant en lui-même qui est invalide, c'est l'état actuel de l'objectif qui
    // ne le permet pas (IllegalStateException, pas IllegalArgumentException).
    public void retirerObjectif(int idObjectif, double montant, LocalDate date) {
        Epargne objectif = servicePortefeuille.getDonnees().getObjectif(idObjectif);
        if (montant > getMontantActuel(objectif)) {
            throw new IllegalStateException("Le montant du retrait ne peut pas dépasser le montant actuellement épargné.");
        }

        objectif.ajouterMouvement(new MouvementEpargne(montant, SensMouvement.RETRAIT, date));
        servicePortefeuille.sauvegarder();
    }

    // Un objectif ne peut être supprimé que s'il est vide : l'utilisateur doit d'abord décider
    // de la destination des sommes qui y étaient placées. Refusé à cause de l'état de l'objectif
    // (pas d'une donnée invalide) : IllegalStateException.
    public void supprimerObjectif(int idObjectif) {
        Portefeuille portefeuille = servicePortefeuille.getDonnees();
        Epargne objectif = portefeuille.getObjectif(idObjectif);
        if (!estVide(objectif)) {
            throw new IllegalStateException("L'objectif n'est pas vide (" + getMontantActuel(objectif)
                    + " FCFA restants), retirez d'abord les sommes épargnées.");
        }
        portefeuille.retirerObjectif(objectif);
        servicePortefeuille.sauvegarder();
    }
}
