package modele.exception;

/*
    * Levée quand la lecture du fichier de sauvegarde échoue pour une raison que
    * GestionnaireFichier.charger() ne peut pas absorber en repartant simplement d'un
    * portefeuille vide (ex. droits d'accès refusés, erreur disque). À distinguer d'un fichier
    * absent, vide ou au contenu JSON invalide : ces trois cas-là sont défensifs par nature et
    * ne lèvent jamais cette exception, charger() y répond toujours par un portefeuille exploitable.
*/
public class ErreurChargementException extends RuntimeException {
    public ErreurChargementException(String message, Throwable cause) {
        super(message, cause);
    }
}
