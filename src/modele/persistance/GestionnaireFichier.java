package modele.persistance;

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

import modele.entite.Portefeuille;
import modele.exception.ErreurChargementException;
import modele.exception.ErreurSauvegardeException;

/*
    * La classe GestionnaireFichier est la seule classe autorisée à lire ou écrire sur le disque.
    * Elle isole toute la logique de sérialisation/désérialisation JSON (via Gson) du reste de
    * l'application : Portefeuille n'a jamais besoin de savoir comment les données sont stockées.
    *
    * Gson ne sait pas convertir un LocalDate en JSON par défaut (ce n'est pas un type qu'il
    * reconnaît nativement), il faut donc lui apprendre à le faire avec un adaptateur, enregistré
    * dans le constructeur.
*/
public class GestionnaireFichier {

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
            portefeuille.reparerApresChargement();
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
}
