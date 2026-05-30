package comunio.nas.objects.helper;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Email {

	/**
	 * Sucht in der Spielerdatenbank (JSONArray) alle Spieler mit
	 * data.transfermarktDoDe.haveToCheck == true und sendet eine E-Mail mit deren
	 * Namen und Links.
	 *
	 * @param spielerDatabase JSONArray mit allen Spielerobjekten (wie im Beispiel)
	 * @param empfaengerMail  Empfänger-Adresse der Benachrichtigung
	 * @param absenderMail    Mailadresse für Absender (SMTP-Versand)
	 * @param smtpHost        SMTP-Host (z.B. smtp.gmail.com)
	 * @param smtpPort        SMTP-Port (z.B. 587)
	 * @param smtpBenutzer    SMTP-Benutzername
	 * @param smtpPasswort    SMTP-Passwort
	 * @throws MessagingException Bei Fehlern beim Mailversand
	 */
	public static void sendCheckMail(JSONArray spielerDatabase, String empfaengerMail, String absenderMail, String smtpHost, String smtpPort, String smtpBenutzer, String smtpPasswort) throws MessagingException {

		List<String> spielerMitCheck = new ArrayList<>();

		// Spieler filtern und sammeln
		for (int i = 0; i < spielerDatabase.length(); i++) {
			JSONObject spieler = spielerDatabase.getJSONObject(i);
			String comunioName = spieler.getString("name");
			String comunioId = spieler.getString("id");
			
			JSONObject data = spieler.optJSONObject("data");
			if (data == null)
				continue;

			JSONObject transfermarkt = data.optJSONObject("transfermarktDoDe");
			if (transfermarkt == null)
				continue;

			boolean haveToCheck = transfermarkt.optBoolean("haveToCheck", false);
			if (haveToCheck) {
				String name = transfermarkt.optString("name", "unbekannt");
				String link = transfermarkt.optString("link", "kein link");
				spielerMitCheck.add("Comunio Spieler: " + comunioName + " (ID: " + comunioId + ") --> Transfermarkt.de: " + name + ": " + link);
			}
		}

		if (spielerMitCheck.isEmpty()) {
			System.out.println("Keine Spieler mit 'haveToCheck' gefunden. Kein Mailversand.");
			return; // nichts zu senden
		}

		// E-Mail-Inhalt erstellen
		StringBuilder mailBody = new StringBuilder();
		mailBody.append("Folgende Spieler wurden gefunden, bei denen der Vorname entfernt wurde und überprüft werden sollten:\n\n");
		for (String s : spielerMitCheck) {
			mailBody.append(s).append("\n");
		}

		// SMTP-Einstellungen
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.port", smtpPort);

		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(smtpBenutzer, smtpPasswort);
			}
		});

		// Nachricht aufbauen
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(absenderMail));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(empfaengerMail));
		message.setSubject("Transfermarkt-Spieler mit entfernten Vornamen - Überprüfung notwendig");
		message.setText(mailBody.toString());

		// Mail versenden
		Transport.send(message);

		System.out.println("E-Mail erfolgreich gesendet an " + empfaengerMail);
	}

}
