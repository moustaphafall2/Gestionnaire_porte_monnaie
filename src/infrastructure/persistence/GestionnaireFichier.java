package infrastructure.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;

import domain.entity.Epargne;
import domain.entity.MouvementEpargne;
import domain.entity.Portefeuille;
import domain.entity.Transaction;
import domain.enumeration.Categorie;
import domain.enumeration.SensMouvement;
import domain.enumeration.TypeTransaction;
import exception.ErreurChargementException;
import exception.ErreurSauvegardeException;

/*
    * La classe GestionnaireFichier est la seule classe autorisée à lire ou écrire sur le disque,
    * et l'unique implémentation de PortefeuilleRepository. Elle isole toute la logique de
    * sérialisation/désérialisation JSON (via Gson) du reste de l'application : ServicePortefeuille
    * ne connaît que l'interface PortefeuilleRepository, jamais cette classe ni la façon dont les
    * données sont réellement stockées.
    *
    * Gson ne sait pas convertir un LocalDate en JSON par défaut (ce n'est pas un type qu'il
    * reconnaît nativement), il faut donc lui apprendre à le faire avec un adaptateur, enregistré
    * dans le constructeur.
*/
public class GestionnaireFichier implements PortefeuilleRepository {

    private String cheminFichier;
    private Gson gson;

    public GestionnaireFichier(String cheminFichier) {
        this.cheminFichier = cheminFichier;

        // Adaptateur pour LocalDate : indique à Gson comment le transformer en texte ("2026-08-10")
        // et comment le relire depuis ce même texte, dans les deux sens.
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (date, type, contexte) ->
                        new JsonPrimitive(date.toString()))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, contexte) ->
                        LocalDate.parse(json.getAsString()))
                .setPrettyPrinting()
                .create();
    }

    // Sérialise le portefeuille en JSON et l'écrit dans le fichier de sauvegarde, de façon
    // atomique : on écrit d'abord dans un fichier temporaire, puis on le renomme à la place du
    // fichier final. Une coupure pendant l'écriture laisse un fichier .tmp incomplet, jamais le
    // fichier existant à moitié écrit — le renommage (Files.move avec ATOMIC_MOVE) est lui-même
    // une opération indivisible du système de fichiers, contrairement à écrire directement dans
    // portefeuille.json.
    public void sauvegarder(Portefeuille portefeuille) {
        Path cheminFinal = Paths.get(cheminFichier);
        Path cheminTemporaire = Paths.get(cheminFichier + ".tmp");

        try (BufferedWriter fichier = Files.newBufferedWriter(cheminTemporaire, StandardCharsets.UTF_8)) {
            gson.toJson(portefeuille, fichier);
        } catch (IOException exception) {
            throw new ErreurSauvegardeException("Impossible d'écrire le fichier de sauvegarde : " + cheminFichier, exception);
        }

        try {
            Files.move(cheminTemporaire, cheminFinal, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            throw new ErreurSauvegardeException("Impossible de finaliser la sauvegarde : " + cheminFichier, exception);
        }
    }

    // Lit le fichier JSON et le désérialise en Portefeuille. Défensif sur quatre cas, pour que
    // cette méthode renvoie toujours un portefeuille exploitable plutôt que de planter au
    // démarrage de l'application :
    //   1. fichier absent (premier lancement) ;
    //   2. fichier vide (Gson ne lève rien dans ce cas, il renvoie null) ;
    //   3. JSON malformé (JsonSyntaxException) ;
    //   4. listes/ensemble à null après désérialisation (Gson contourne le constructeur).
    // Seule une vraie erreur de lecture disque (droits, panne...) lève ErreurChargementException :
    // ce n'est pas un cas qu'on peut raisonnablement réparer en repartant d'un portefeuille vide.
    public Portefeuille charger() {
        Path chemin = Paths.get(cheminFichier);
        if (!fichierExiste()) {
            return new Portefeuille();
        }

        try (BufferedReader fichier = Files.newBufferedReader(chemin, StandardCharsets.UTF_8)) {
            Portefeuille portefeuille = gson.fromJson(fichier, Portefeuille.class);
            if (portefeuille == null) {
                return new Portefeuille();
            }
            reparerApresChargement(portefeuille);
            return portefeuille;
        } catch (JsonSyntaxException exception) {
            return new Portefeuille();
        } catch (IOException exception) {
            throw new ErreurChargementException("Impossible de lire le fichier de sauvegarde : " + cheminFichier, exception);
        }
    }

    private boolean fichierExiste() {
        return Files.exists(Paths.get(cheminFichier));
    }

    // GestionnaireFichier ne détient aucun Portefeuille entre deux appels (contrairement à
    // ServicePortefeuille) : chaque méthode granulaire recharge le fichier, applique la
    // mutation, puis réécrit tout — le même travail que faisait déjà sauvegarder(Portefeuille)
    // avant l'étape 6, simplement déclenché par un appel plus précis. Ce n'est pas la version
    // définitive de PortefeuilleRepository (elle vit dans GestionnairePostgreSQL, une écriture
    // ciblée par l'instruction SQL) : GestionnaireFichier n'a pas besoin de mieux, il sera
    // supprimé une fois la migration terminée.
    public void activerCategorie(Categorie categorie) {
        Portefeuille portefeuille = charger();
        portefeuille.activerCategorie(categorie);
        sauvegarder(portefeuille);
    }

    public void desactiverCategorie(Categorie categorie) {
        Portefeuille portefeuille = charger();
        portefeuille.desactiverCategorie(categorie);
        sauvegarder(portefeuille);
    }

    // L'identifiant vient désormais du repository, plus d'un compteur lu par le service : ici,
    // c'est encore le compteur de Portefeuille (prochainIdTransaction) qui le fournit, puisque
    // GestionnaireFichier n'a pas de SERIAL pour le faire à sa place. Ce compteur disparaîtra de
    // Portefeuille au branchement final, une fois GestionnaireFichier lui-même supprimé.
    public int ajouterTransaction(double montant, TypeTransaction type, Categorie categorie, LocalDate date, String description) {
        Portefeuille portefeuille = charger();
        int id = portefeuille.getProchainIdTransaction();
        portefeuille.setProchainIdTransaction(id + 1);
        portefeuille.ajouterTransaction(new Transaction(id, montant, type, categorie, date, description));
        sauvegarder(portefeuille);
        return id;
    }

    public void modifierTransaction(int id, double montant, Categorie categorie, LocalDate date, String description) {
        Portefeuille portefeuille = charger();
        Transaction transaction = trouverTransaction(portefeuille, id);
        transaction.setMontant(montant);
        transaction.setCategorie(categorie);
        transaction.setDate(date);
        transaction.setDescription(description);
        sauvegarder(portefeuille);
    }

    public void supprimerTransaction(int id) {
        Portefeuille portefeuille = charger();
        Transaction transaction = trouverTransaction(portefeuille, id);
        portefeuille.retirerTransaction(transaction);
        sauvegarder(portefeuille);
    }

    // Ne devrait jamais déclencher son cas d'erreur en usage normal : le fichier reste
    // synchronisé avec la mémoire de ServicePortefeuille, puisque toute mutation passe par une
    // méthode de cette classe. Gardée quand même : chercher un objet par id dans une liste sans
    // vérifier qu'il existe serait une confiance aveugle, contraire à la règle du projet.
    private Transaction trouverTransaction(Portefeuille portefeuille, int id) {
        for (Transaction transaction : portefeuille.getTransactions()) {
            if (transaction.getId() == id) {
                return transaction;
            }
        }
        throw new ErreurSauvegardeException("Aucune transaction avec l'identifiant " + id + " dans le fichier de sauvegarde.", null);
    }

    // Même principe que ajouterTransaction() : le compteur d'objectifs de Portefeuille fournit
    // l'identifiant, en attendant la suppression de GestionnaireFichier.
    public int ajouterObjectif(String nom, double montantCible, LocalDate dateLimite) {
        Portefeuille portefeuille = charger();
        int id = portefeuille.getProchainIdObjectif();
        portefeuille.setProchainIdObjectif(id + 1);
        portefeuille.ajouterObjectif(new Epargne(id, nom, montantCible, dateLimite));
        sauvegarder(portefeuille);
        return id;
    }

    public void ajouterMouvement(int idObjectif, double montant, SensMouvement sens, LocalDate date) {
        Portefeuille portefeuille = charger();
        Epargne objectif = trouverObjectif(portefeuille, idObjectif);
        objectif.ajouterMouvement(new MouvementEpargne(montant, sens, date));
        sauvegarder(portefeuille);
    }

    public void supprimerObjectif(int id) {
        Portefeuille portefeuille = charger();
        Epargne objectif = trouverObjectif(portefeuille, id);
        portefeuille.retirerObjectif(objectif);
        sauvegarder(portefeuille);
    }

    // Même principe et même remarque que trouverTransaction() ci-dessus.
    private Epargne trouverObjectif(Portefeuille portefeuille, int id) {
        for (Epargne objectif : portefeuille.getObjectifs()) {
            if (objectif.getId() == id) {
                return objectif;
            }
        }
        throw new ErreurSauvegardeException("Aucun objectif avec l'identifiant " + id + " dans le fichier de sauvegarde.", null);
    }

    // Gson contourne le constructeur à la désérialisation (il remplit les champs directement) :
    // un champ absent du JSON, ou explicitement "null", reste à null au lieu d'être initialisé
    // à une collection vide. Ce n'est pas une règle du domaine, c'est un contournement d'un
    // comportement de Gson : elle vit ici, dans la classe qui connaît Gson, plutôt que dans
    // Portefeuille. Garantit qu'un Portefeuille rechargé est toujours exploitable
    // (getTransactions()/getCategoriesActives()/getObjectifs() ne renvoient jamais null après
    // cet appel).
    private void reparerApresChargement(Portefeuille portefeuille) {
        if (portefeuille.getTransactions() == null) {
            portefeuille.setTransactions(new ArrayList<>());
        }
        if (portefeuille.getCategoriesActives() == null) {
            portefeuille.setCategoriesActives(new HashSet<>());
        }
        if (portefeuille.getObjectifs() == null) {
            portefeuille.setObjectifs(new ArrayList<>());
        }
    }
}
