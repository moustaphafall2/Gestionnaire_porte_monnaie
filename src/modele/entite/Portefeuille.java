package modele.entite;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import modele.enumeration.Categorie;
import modele.enumeration.TypeTransaction;

/*
    * La classe Portefeuille regroupe toutes les données de l'utilisateur : les transactions,
    * les catégories actives et les objectifs d'épargne. Migration en cours vers l'architecture
    * MVC : elle contient encore une partie de la logique métier (à extraire progressivement
    * vers les classes de modele.service), mais plus rien de la persistance — c'est
    * ServicePortefeuille qui détient le GestionnaireFichier et déclenche la sauvegarde.
*/
public class Portefeuille {

    // Attributs
    private List<Transaction> transactions;
    private Set<Categorie> categoriesActives;
    private List<Epargne> objectifs;
    private int prochainIdTransaction;
    private int prochainIdObjectif;

    // Constructeur : crée un portefeuille vide, sans transaction ni objectif
    public Portefeuille() {
        this.transactions = new ArrayList<>();
        this.categoriesActives = new HashSet<>();
        this.objectifs = new ArrayList<>();
        this.prochainIdTransaction = 1;
        this.prochainIdObjectif = 1;
    }

    // Gestion des transactions

    // Vue brute des transactions, utilisée par les services pour faire leurs calculs.
    // L'entité ne fait ici que donner accès à ses données.
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    // Distribue l'identifiant suivant et avance le compteur. Le compteur reste ici (champ de
    // l'entité, donc sauvegardé) : s'il repartait de zéro au redémarrage, on créerait des
    // doublons. Mais la construction de la Transaction elle-même se fait dans ServiceTransaction.
    public int genererIdTransaction() {
        return prochainIdTransaction++;
    }

    // Remplace l'ancien setter : ajouter une transaction est un ajout à la liste, pas une
    // substitution d'attribut.
    public void ajouterTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public void retirerTransaction(Transaction transaction) {
        transactions.remove(transaction);
    }

    // Gestion des catégories

    public Set<Categorie> getCategoriesActives() {
        return Collections.unmodifiableSet(categoriesActives);
    }

    // Indique si au moins une catégorie active correspond à ce type. Utilisé par Menu
    // pour vérifier la précondition avant de proposer d'ajouter une dépense/un revenu.
    public boolean aCategorieActiveDeType(TypeTransaction type) {
        for (Categorie categorie : categoriesActives) {
            if (categorie.getType() == type) {
                return true;
            }
        }
        return false;
    }

    // Catégories de la liste complète (l'énumération) qui ne sont pas encore actives
    public List<Categorie> getCategoriesDisponibles() {
        List<Categorie> disponibles = new ArrayList<>();
        for (Categorie categorie : Categorie.values()) {
            if (!categoriesActives.contains(categorie)) {
                disponibles.add(categorie);
            }
        }
        return disponibles;
    }

    public void activerCategorie(Categorie categorie) {
        categoriesActives.add(categorie);
    }

    // La désactivation retire seulement la catégorie des choix proposés :
    // les transactions déjà enregistrées avec cette catégorie ne sont jamais modifiées.
    public void desactiverCategorie(Categorie categorie) {
        categoriesActives.remove(categorie);
    }

    // Gestion des objectifs d'épargne

    public Epargne creerObjectif(String nom, double montantCible, LocalDate dateLimite) {
        Epargne objectif = new Epargne(prochainIdObjectif, nom, montantCible, dateLimite);
        objectifs.add(objectif);
        prochainIdObjectif++;
        return objectif;
    }

    // L'exception si le retrait dépasse le montant épargné est déjà levée par Epargne.retirer().
    public void retirerObjectif(int idObjectif, double montant, LocalDate date) {
        Epargne objectif = trouverObjectif(idObjectif);
        objectif.retirer(montant, date);
    }

    // Un objectif ne peut être supprimé que s'il est vide : l'utilisateur doit d'abord
    // décider de la destination des sommes qui y étaient placées. Refusé à cause de l'état
    // de l'objectif (pas d'une donnée invalide) : IllegalStateException.
    public void supprimerObjectif(int idObjectif) {
        Epargne objectif = trouverObjectif(idObjectif);
        if (!objectif.estVide()) {
            throw new IllegalStateException("L'objectif n'est pas vide (" + objectif.getMontantActuel()
                    + " FCFA restants), retirez d'abord les sommes épargnées.");
        }
        objectifs.remove(objectif);
    }

    public List<Epargne> getObjectifs() {
        return Collections.unmodifiableList(objectifs);
    }

    // Recherche publique d'un objectif par id, utilisée par Menu pour afficher un
    // récapitulatif (nom, progression...) avant de demander confirmation à l'utilisateur.
    public Epargne getObjectif(int idObjectif) {
        return trouverObjectif(idObjectif);
    }

    // Recherche interne d'un objectif par id, réutilisée par contribuerObjectif, retirerObjectif et supprimerObjectif
    private Epargne trouverObjectif(int idObjectif) {
        for (Epargne objectif : objectifs) {
            if (objectif.getId() == idObjectif) {
                return objectif;
            }
        }
        throw new IllegalArgumentException("Aucun objectif avec l'identifiant " + idObjectif + ".");
    }

    // Statistiques
    // Ces deux méthodes restent ici temporairement (elles migreront vers ServiceStatistique,
    // pas encore construit). filtrerParDate ayant déplacé vers ServiceTransaction, le filtrage
    // par période est réécrit ici en ligne plutôt que de dépendre d'une méthode qui n'existe
    // plus dans l'entité.

    // Total dépensé par catégorie, sur une période donnée
    public Map<Categorie, Double> getTotalParCategorie(LocalDate debut, LocalDate fin) {
        Map<Categorie, Double> totaux = new HashMap<>();

        for (Transaction transaction : transactions) {
            LocalDate date = transaction.getDate();
            if (date.isBefore(debut) || date.isAfter(fin)) {
                continue;
            }
            if (transaction.getType() == TypeTransaction.DEPENSE) {
                Categorie categorie = transaction.getCategorie();
                double totalActuel = totaux.getOrDefault(categorie, 0.0);
                totaux.put(categorie, totalActuel + transaction.getMontant());
            }
        }

        return totaux;
    }

    // Total des revenus et des dépenses sur une période donnée.
    // Index 0 : total des revenus. Index 1 : total des dépenses.
    public double[] getTotalRevenusEtDepenses(LocalDate debut, LocalDate fin) {
        double totalRevenus = 0;
        double totalDepenses = 0;

        for (Transaction transaction : transactions) {
            LocalDate date = transaction.getDate();
            if (date.isBefore(debut) || date.isAfter(fin)) {
                continue;
            }
            if (transaction.getType() == TypeTransaction.REVENU) {
                totalRevenus += transaction.getMontant();
            } else {
                totalDepenses += transaction.getMontant();
            }
        }

        return new double[] { totalRevenus, totalDepenses };
    }
}
