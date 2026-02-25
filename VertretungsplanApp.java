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

    private final Label todayTitle = new Label("Heute");
    private final Label tomorrowTitle = new Label("Morgen");
    private final TextArea todayArea = new TextArea();
    private final TextArea tomorrowArea = new TextArea();

    private final Properties props = new Properties();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Vertretungsplan - Profil");

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        root.getChildren().add(profileSection());
        root.getChildren().add(buttonBar());
        root.getChildren().add(resultSection());
        root.getChildren().add(statusLabel);

        loadProfiles();

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.show();
    }

    private VBox profileSection() {
        Label title = new Label("Profil");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        profileSelect.setPromptText("Profil wählen");
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
        Button del = new Button("Löschen");

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

    private HBox resultSection() {
        todayTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        tomorrowTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        todayArea.setEditable(false);
        todayArea.setWrapText(true);
        tomorrowArea.setEditable(false);
        tomorrowArea.setWrapText(true);

        VBox left = new VBox(6, todayTitle, todayArea);
        VBox right = new VBox(6, tomorrowTitle, tomorrowArea);
        VBox.setVgrow(todayArea, Priority.ALWAYS);
        VBox.setVgrow(tomorrowArea, Priority.ALWAYS);

        HBox box = new HBox(12, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
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
            statusLabel.setText("Kein Profil ausgewählt.");
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
        statusLabel.setText("Profil gelöscht.");
    }

    private String poop() {
        return "  (  )\n (    )\n(      )\n (____)\n  ||||";
    }

    private String formatList(List<Map<String, String>> list) {
        if (list == null || list.isEmpty()) return "Keine Vertretung.";
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> e : list) {
            String fach = e.getOrDefault("fach", "?");
            String stunde = e.getOrDefault("stunde", "?");
            sb.append("Du haßt \"").append(fach).append("\" in der ")
              .append(stunde).append(". Stunde frei.\n");
        }
        return sb.toString().trim();
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
                statusLabel.setText("Login fehlgeschlagen.");
                return;
            }

            List<String> courses = Vertretungsplan.fetchCourses();

            String planText = Vertretungsplan.scrapePlan(
                    "https://bonniweb.de/pluginfile.php/2992/mod_resource/content/11/w00024.htm"
            );
            if (planText.isEmpty()) {
                statusLabel.setText("Plan nicht erreichbar.");
                return;
            }

            Map<String, List<Map<String, String>>> vertretungen =
                    Vertretungsplan.parseUntis(planText);

            LinkedHashMap<String, List<Map<String, String>>> filteredByDay = new LinkedHashMap<>();
            for (Map.Entry<String, List<Map<String, String>>> entry : vertretungen.entrySet()) {
                List<Map<String, String>> filtered = new ArrayList<>();
                for (Map<String, String> e : entry.getValue()) {
                    String info = e.getOrDefault("info", "");
                    String fach = e.getOrDefault("fach", "");
                    String lehrer = e.getOrDefault("lehrer", "");
                    if ("EVA".equalsIgnoreCase(info) && Vertretungsplan.matchesCourse(fach, lehrer, courses)) {
                        filtered.add(e);
                    }
                }
                filteredByDay.put(entry.getKey(), filtered);
            }

            List<String> days = new ArrayList<>(Vertretungsplan.splitByDay(planText).keySet());
            String dayToday = Vertretungsplan.firstAvailableDay(planText);
            String dayTomorrow = null;
            if (dayToday != null) {
                int idx = days.indexOf(dayToday);
                if (idx >= 0 && idx + 1 < days.size()) dayTomorrow = days.get(idx + 1);
            }

            todayTitle.setText(dayToday != null ? "Heute (" + dayToday + ")" : "Heute");
            tomorrowTitle.setText(dayTomorrow != null ? "Morgen (" + dayTomorrow + ")" : "Morgen");

            List<Map<String, String>> listToday = dayToday == null
                    ? Collections.emptyList()
                    : filteredByDay.getOrDefault(dayToday, Collections.emptyList());
            List<Map<String, String>> listTomorrow = dayTomorrow == null
                    ? Collections.emptyList()
                    : filteredByDay.getOrDefault(dayTomorrow, Collections.emptyList());

            String todayText = formatList(listToday);
            String tomorrowText = formatList(listTomorrow);
            todayArea.setText(todayText);
            tomorrowArea.setText(tomorrowText);

            // Terminal output for debugging
            System.out.println("Heute:");
            System.out.println(todayText);
            System.out.println();
            System.out.println("Morgen:");
            System.out.println(tomorrowText);

            statusLabel.setText("Fertig.");
        } catch (Exception ex) {
            statusLabel.setText("Fehler beim Login.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
