package metier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import persistance.GestionnaireFichier;

/*
    * La classe Portefeuille est la classe centrale de l'application.
    * Elle regroupe toutes les données de l'utilisateur : les transactions, les catégories actives
    * et les objectifs d'épargne. Elle contient toute la logique métier (les règles de gestion),
    * et ne connaît rien de l'affichage console : c'est la classe Menu qui viendra l'appeler.
    * Le solde disponible et le total épargné ne sont jamais stockés : ils sont toujours recalculés
    * à partir des transactions et des objectifs, ce qui évite toute incohérence.
    *
    * C'est aussi le portefeuille qui déclenche sa propre sauvegarde après chaque opération
    * validée (et non le Menu), pour qu'aucune modification ne puisse être oubliée.
*/
public class Portefeuille {

    // Attributs
    private List<Transaction> transactions;
    private Set<Categorie> categoriesActives;
    private List<Epargne> objectifs;
    private int prochainIdTransaction;
    private int prochainIdObjectif;

    // transient : ce champ ne doit jamais être écrit dans le fichier JSON par Gson.
    // Ce n'est pas une donnée du portefeuille, seulement l'outil qui sait où/comment le sauvegarder.
    private transient GestionnaireFichier gestionnaireFichier;

    // Constructeur : crée un portefeuille vide, sans transaction ni objectif
    public Portefeuille() {
        this.transactions = new ArrayList<>();
        this.categoriesActives = new HashSet<>();
        this.objectifs = new ArrayList<>();
        this.prochainIdTransaction = 1;
        this.prochainIdObjectif = 1;
    }

    // Solde et consultation

    // Solde disponible = total des revenus - total des dépenses - total actuellement épargné.
    // Il n'est jamais stocké, pour ne jamais pouvoir être incohérent avec l'historique.
    public double getSoldeDisponible() {
        double totalRevenus = 0;
        double totalDepenses = 0;

        for (Transaction transaction : transactions) {
            if (transaction.getType() == TypeTransaction.REVENU) {
                totalRevenus += transaction.getMontant();
            } else {
                totalDepenses += transaction.getMontant();
            }
        }

        return totalRevenus - totalDepenses - getTotalEpargne();
    }

    // Solde qu'on obtiendrait si cette dépense était enregistrée, sans l'enregistrer.
    // Utilisé par Menu pour avertir l'utilisateur avant confirmation.
    public double soldeApresDepense(double montant) {
        return getSoldeDisponible() - montant;
    }

    // Somme du montant actuellement épargné sur tous les objectifs
    public double getTotalEpargne() {
        double total = 0;
        for (Epargne objectif : objectifs) {
            total += objectif.getMontantActuel();
        }
        return total;
    }

    // Gestion des transactions 

    public Transaction ajouterDepense(double montant, Categorie categorie, LocalDate date, String description) {
        Transaction depense = new Transaction(prochainIdTransaction, montant, TypeTransaction.DEPENSE, categorie, date, description);
        transactions.add(depense);
        prochainIdTransaction++;
        sauvegarder();
        return depense;
    }

    public Transaction ajouterRevenu(double montant, Categorie categorie, LocalDate date, String description) {
        Transaction revenu = new Transaction(prochainIdTransaction, montant, TypeTransaction.REVENU, categorie, date, description);
        transactions.add(revenu);
        prochainIdTransaction++;
        sauvegarder();
        return revenu;
    }

    public void modifierTransaction(int id, double nouveauMontant, Categorie nouvelleCategorie, LocalDate nouvelleDate, String nouvelleDescription) {
        Transaction transaction = trouverTransaction(id);
        transaction.setMontant(nouveauMontant);
        transaction.setCategorie(nouvelleCategorie);
        transaction.setDate(nouvelleDate);
        transaction.setDescription(nouvelleDescription);
        sauvegarder();
    }

    public void supprimerTransaction(int id) {
        Transaction transaction = trouverTransaction(id);
        transactions.remove(transaction);
        sauvegarder();
    }

    // Historique complet, trié du plus récent au plus ancien
    public List<Transaction> getHistorique() {
        List<Transaction> historique = new ArrayList<>(transactions);
        historique.sort(Comparator.comparing(Transaction::getDate).reversed());
        return historique;
    }

    public List<Transaction> filtrerParDate(LocalDate debut, LocalDate fin) {
        List<Transaction> resultat = new ArrayList<>();
        for (Transaction transaction : transactions) {
            LocalDate date = transaction.getDate();
            if (!date.isBefore(debut) && !date.isAfter(fin)) {
                resultat.add(transaction);
            }
        }
        return resultat;
    }

    public List<Transaction> filtrerParCategorie(Categorie categorie) {
        List<Transaction> resultat = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (transaction.getCategorie() == categorie) {
                resultat.add(transaction);
            }
        }
        return resultat;
    }

    public List<Transaction> filtrerParType(TypeTransaction type) {
        List<Transaction> resultat = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (transaction.getType() == type) {
                resultat.add(transaction);
            }
        }
        return resultat;
    }

    // Recherche interne d'une transaction par id, réutilisée par modifierTransaction et supprimerTransaction
    private Transaction trouverTransaction(int id) {
        for (Transaction transaction : transactions) {
            if (transaction.getId() == id) {
                return transaction;
            }
        }
        throw new IllegalArgumentException("Aucune transaction avec l'identifiant " + id + ".");
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
        sauvegarder();
    }

    // La désactivation retire seulement la catégorie des choix proposés :
    // les transactions déjà enregistrées avec cette catégorie ne sont jamais modifiées.
    public void desactiverCategorie(Categorie categorie) {
        categoriesActives.remove(categorie);
        sauvegarder();
    }

    // Gestion des objectifs d'épargne

    public Epargne creerObjectif(String nom, double montantCible, LocalDate dateLimite) {
        Epargne objectif = new Epargne(prochainIdObjectif, nom, montantCible, dateLimite);
        objectifs.add(objectif);
        prochainIdObjectif++;
        sauvegarder();
        return objectif;
    }

    // Un objectif d'épargne fonctionne comme un coffre : impossible d'y placer une somme
    // dont on ne dispose pas. La vérification se fait ici, et pas dans Epargne, car Epargne
    // ne connaît pas le solde disponible du portefeuille. Refusé à cause de l'état actuel
    // du portefeuille (pas d'une donnée invalide en soi) : IllegalStateException.
    public void contribuerObjectif(int idObjectif, double montant, LocalDate date) {
        if (montant > getSoldeDisponible()) {
            throw new IllegalStateException("Le montant de la contribution dépasse le solde disponible.");
        }

        Epargne objectif = trouverObjectif(idObjectif);
        objectif.contribuer(montant, date);
        sauvegarder();
    }

    // L'exception si le retrait dépasse le montant épargné est déjà levée par Epargne.retirer().
    public void retirerObjectif(int idObjectif, double montant, LocalDate date) {
        Epargne objectif = trouverObjectif(idObjectif);
        objectif.retirer(montant, date);
        sauvegarder();
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
        sauvegarder();
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

    // Total dépensé par catégorie, sur une période donnée
    public Map<Categorie, Double> getTotalParCategorie(LocalDate debut, LocalDate fin) {
        Map<Categorie, Double> totaux = new HashMap<>();

        for (Transaction transaction : filtrerParDate(debut, fin)) {
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

        for (Transaction transaction : filtrerParDate(debut, fin)) {
            if (transaction.getType() == TypeTransaction.REVENU) {
                totalRevenus += transaction.getMontant();
            } else {
                totalDepenses += transaction.getMontant();
            }
        }

        return new double[] { totalRevenus, totalDepenses };
    }

    // Persistance

    // Déclenche la sauvegarde du portefeuille. Ne fait rien si le portefeuille n'a pas
    // été chargé via charger(GestionnaireFichier) (par exemple dans un test unitaire).
    public void sauvegarder() {
        if (gestionnaireFichier != null) {
            gestionnaireFichier.sauvegarder(this);
        }
    }

    // Charge un portefeuille existant depuis le fichier JSON, ou en crée un nouveau vide
    // si aucun fichier n'existe. Dans les deux cas, le portefeuille retourné connaît son
    // GestionnaireFichier, pour pouvoir se sauvegarder lui-même après chaque opération.
    public static Portefeuille charger(GestionnaireFichier gestionnaireFichier) {
        Portefeuille portefeuille = gestionnaireFichier.charger();
        portefeuille.gestionnaireFichier = gestionnaireFichier;
        return portefeuille;
    }
}
