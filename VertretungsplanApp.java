import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class VertretungsplanApp extends Application {

    private static final String PROFILE_FILE = "profile.properties";

    private final ComboBox<String> profileSelect = new ComboBox<>();
    private final TextField profileKeyField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label statusLabel = new Label("");

    private final Properties props = new Properties();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Vertretungsplan - Profil");

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        root.getChildren().add(profileSection());
        root.getChildren().add(buttonBar());
        root.getChildren().add(statusLabel);

        loadProfiles();

        Scene scene = new Scene(root, 600, 320);
        stage.setScene(scene);
        stage.show();
    }

    private VBox profileSection() {
        Label title = new Label("Profil");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        profileSelect.setPromptText("Profil waehlen");
        profileSelect.setOnAction(e -> loadSelectedProfile());

        grid.add(new Label("Profil"), 0, 0);
        grid.add(profileSelect, 1, 0);
        GridPane.setHgrow(profileSelect, Priority.ALWAYS);

        profileKeyField.setPromptText("z.B. Anna");
        grid.add(new Label("Profil-Name"), 0, 1);
        grid.add(profileKeyField, 1, 1);
        GridPane.setHgrow(profileKeyField, Priority.ALWAYS);

        grid.add(new Label("Name"), 0, 2);
        grid.add(nameField, 1, 2);
        GridPane.setHgrow(nameField, Priority.ALWAYS);

        grid.add(new Label("Benutzername"), 0, 3);
        grid.add(usernameField, 1, 3);
        GridPane.setHgrow(usernameField, Priority.ALWAYS);

        grid.add(new Label("Passwort"), 0, 4);
        grid.add(passwordField, 1, 4);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);

        VBox box = new VBox(8, title, grid);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-border-color: #d0d0d0; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        return box;
    }

    private HBox buttonBar() {
        Button save = new Button("Speichern");
        Button reload = new Button("Neu laden");
        Button login = new Button("Login & Kurse laden");
        Button clear = new Button("Neu");
        Button del = new Button("Loeschen");

        save.setOnAction(e -> {
            saveProfile();
            statusLabel.setText("Profil gespeichert.");
        });
        reload.setOnAction(e -> loadProfiles());
        login.setOnAction(e -> loginAndPrint());
        clear.setOnAction(e -> newProfile());
        del.setOnAction(e -> deleteProfile());

        HBox box = new HBox(8, save, reload, login, clear, del);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    private void loadProfiles() {
        props.clear();
        if (Files.exists(Path.of(PROFILE_FILE))) {
            try (FileInputStream in = new FileInputStream(PROFILE_FILE)) {
                props.load(in);
            } catch (Exception ignored) {}
        }

        Set<String> names = new LinkedHashSet<>();
        String list = props.getProperty("profiles", "").trim();
        if (!list.isEmpty()) {
            for (String p : list.split(",")) {
                String s = p.trim();
                if (!s.isEmpty() && !"default".equalsIgnoreCase(s)) names.add(s);
            }
        }

        profileSelect.getItems().setAll(names);
        if (!names.isEmpty()) {
            profileSelect.getSelectionModel().select(0);
            loadSelectedProfile();
        }
    }

    private void loadSelectedProfile() {
        String key = profileSelect.getValue();
        if (key == null || key.isEmpty()) return;
        profileKeyField.setText(key);
        nameField.setText(props.getProperty("profile." + key + ".name", ""));
        usernameField.setText(props.getProperty("profile." + key + ".username", ""));
        passwordField.setText(props.getProperty("profile." + key + ".password", ""));
    }

    private void saveProfile() {
        String key = profileKeyField.getText().trim();
        if (key.isEmpty()) {
            statusLabel.setText("Profil-Name fehlt.");
            return;
        }
        if ("default".equalsIgnoreCase(key)) {
            statusLabel.setText("Profil-Name darf nicht 'Default' sein.");
            return;
        }

        props.setProperty("profile." + key + ".name", nameField.getText().trim());
        props.setProperty("profile." + key + ".username", usernameField.getText().trim());
        props.setProperty("profile." + key + ".password", passwordField.getText());

        Set<String> names = new LinkedHashSet<>(profileSelect.getItems());
        names.add(key);
        props.setProperty("profiles", String.join(",", names));

        try (FileOutputStream out = new FileOutputStream(PROFILE_FILE)) {
            props.store(out, "Vertretungsplan profile");
        } catch (Exception ignored) {}

        profileSelect.getItems().setAll(names);
        profileSelect.getSelectionModel().select(key);
    }

    private void newProfile() {
        profileSelect.getSelectionModel().clearSelection();
        profileKeyField.setText("");
        nameField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        statusLabel.setText("Neues Profil.");
    }

    private void deleteProfile() {
        String key = profileSelect.getValue();
        if (key == null || key.isEmpty()) {
            statusLabel.setText("Kein Profil ausgewaehlt.");
            return;
        }
        props.remove("profile." + key + ".name");
        props.remove("profile." + key + ".username");
        props.remove("profile." + key + ".password");

        Set<String> names = new LinkedHashSet<>(profileSelect.getItems());
        names.remove(key);
        props.setProperty("profiles", String.join(",", names));

        try (FileOutputStream out = new FileOutputStream(PROFILE_FILE)) {
            props.store(out, "Vertretungsplan profile");
        } catch (Exception ignored) {}

        profileSelect.getItems().setAll(names);
        newProfile();
        statusLabel.setText("Profil geloescht.");
    }

    private void loginAndPrint() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Benutzername/Passwort fehlt.");
            return;
        }

        statusLabel.setText("Login... Ausgabe im Terminal.");
        try {
            if (!Vertretungsplan.login(user, pass)) {
                System.out.println("Login fehlgeschlagen.");
                statusLabel.setText("Login fehlgeschlagen.");
                return;
            }

            List<String> courses = Vertretungsplan.fetchCourses();

            String planText = Vertretungsplan.scrapePlan(
                    "https://bonniweb.de/pluginfile.php/2992/mod_resource/content/11/w00024.htm"
            );
            if (planText.isEmpty()) {
                System.out.println("Zugriff auf den Plan fehlgeschlagen (Login-Seite erhalten).");
                statusLabel.setText("Plan nicht erreichbar.");
                return;
            }

            Map<String, List<Map<String, String>>> vertretungen =
                    Vertretungsplan.parseUntis(planText);

            System.out.println("EVA-Ueberschneidungen mit deinen Kursen:");
            int[] count = {0};
            vertretungen.forEach((k, v) -> {
                List<Map<String, String>> filtered = new ArrayList<>();
                for (Map<String, String> e : v) {
                    String info = e.getOrDefault("info", "");
                    String fach = e.getOrDefault("fach", "");
                    String lehrer = e.getOrDefault("lehrer", "");
                    if ("EVA".equalsIgnoreCase(info) && Vertretungsplan.matchesCourse(fach, lehrer, courses)) {
                        filtered.add(e);
                    }
                }
                if (!filtered.isEmpty()) {
                    count[0] += filtered.size();
                    System.out.println(k);
                    for (Map<String, String> e : filtered) {
                        System.out.println("  " + e);
                    }
                }
            });
            if (count[0] == 0) {
                System.out.println("Keine EVA-Ueberschneidungen gefunden.");
            }
            statusLabel.setText("Fertig. Ausgabe im Terminal.");
        } catch (Exception ex) {
            System.out.println("Fehler: " + ex.getMessage());
            statusLabel.setText("Fehler beim Login.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
