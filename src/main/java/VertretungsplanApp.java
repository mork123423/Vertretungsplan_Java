import javafx.application.Application;
import javafx.application.Platform;
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

    private static final Path PROFILE_PATH = resolveProfilePath();
    private static final String PLAN_RESOURCE_URL = "https://bonniweb.de/mod/resource/view.php?id=1323";
    private static final String TIMETABLE_PDF_URL = "https://bonniweb.de/pluginfile.php/2990/mod_resource/content/4/Stufe_Q2.pdf";

    private final ComboBox<String> profileSelect = new ComboBox<>();
    private final TextField profileKeyField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label statusLabel = new Label("");
    private final Button loginBtn = new Button("Login & Kurse laden");

    // UI elements for the Tabs
    private final Label todayTitle = new Label("Heute");
    private final Label tomorrowTitle = new Label("Morgen");
    private final TextArea todayArea = new TextArea();
    private final TextArea tomorrowArea = new TextArea();
    private final ListView<String> coursesList = new ListView<>();
    private final TextArea timetableArea = new TextArea();

    private final Properties props = new Properties();

    private static Path resolveProfilePath() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            return Path.of("profile.properties");
        }
        return Path.of(appData, "Vertretungsplan", "profile.properties");
    }

    private static void ensureProfileDir() {
        try {
            Path dir = PROFILE_PATH.getParent();
            if (dir != null) Files.createDirectories(dir);
        } catch (Exception ignored) {}
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Vertretungsplan - Profil & Übersicht");

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        root.getChildren().add(profileSection());
        root.getChildren().add(buttonBar());
        
        // Add the TabPane and ensure it grows to fill the window
        TabPane resultTabs = resultSection();
        VBox.setVgrow(resultTabs, Priority.ALWAYS);
        root.getChildren().add(resultTabs);
        
        root.getChildren().add(statusLabel);

        loadProfiles();

        Scene scene = new Scene(root, 950, 650);
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
        Button clear = new Button("Neu");
        Button del = new Button("Löschen");

        save.setOnAction(e -> {
            saveProfile();
            statusLabel.setText("Profil gespeichert.");
        });
        reload.setOnAction(e -> loadProfiles());
        loginBtn.setOnAction(e -> loginAndShow());
        clear.setOnAction(e -> newProfile());
        del.setOnAction(e -> deleteProfile());

        HBox box = new HBox(8, save, reload, loginBtn, clear, del);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    private TabPane resultSection() {
        // --- TAB 1: Vertretungen ---
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

        HBox vertretungBox = new HBox(12, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        vertretungBox.setPadding(new Insets(8));

        Tab vertretungTab = new Tab("Vertretungen", vertretungBox);
        vertretungTab.setClosable(false);

        // --- TAB 2: Moodle Kurse ---
        VBox coursesBox = new VBox(6, new Label("Deine erkannten Moodle-Kurse:"), coursesList);
        VBox.setVgrow(coursesList, Priority.ALWAYS);
        coursesBox.setPadding(new Insets(8));
        Tab coursesTab = new Tab("Meine Kurse", coursesBox);
        coursesTab.setClosable(false);

        // --- TAB 3: Stundenplan (PDF Text) ---
        timetableArea.setEditable(false);
        timetableArea.setStyle("-fx-font-family: monospace;"); // Makes tables easier to read
        VBox timetableBox = new VBox(6, new Label("Extrahierter Text aus der Stundenplan-PDF (Stufe Q2):"), timetableArea);
        VBox.setVgrow(timetableArea, Priority.ALWAYS);
        timetableBox.setPadding(new Insets(8));
        Tab timetableTab = new Tab("Stundenplan (PDF)", timetableBox);
        timetableTab.setClosable(false);

        return new TabPane(vertretungTab, coursesTab, timetableTab);
    }

    private void loadProfiles() {
        props.clear();
        if (Files.exists(PROFILE_PATH)) {
            try (FileInputStream in = new FileInputStream(PROFILE_PATH.toFile())) {
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

        ensureProfileDir();
        try (FileOutputStream out = new FileOutputStream(PROFILE_PATH.toFile())) {
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

        ensureProfileDir();
        try (FileOutputStream out = new FileOutputStream(PROFILE_PATH.toFile())) {
            props.store(out, "Vertretungsplan profile");
        } catch (Exception ignored) {}

        profileSelect.getItems().setAll(names);
        newProfile();
        statusLabel.setText("Profil gelöscht.");
    }

    private String formatList(List<EvaMatch> list) {
        if (list == null || list.isEmpty()) return "Keine Vertretung.";
        StringBuilder sb = new StringBuilder();
        for (EvaMatch m : list) {
            PlanEntry e = m.entry;
            String note = m.levelMismatch ? " (möglicherweise falscher GK/LK/ZK)" : "";
            sb.append("Du hast \"").append(e.fach).append("\" in der ")
              .append(e.stunde).append(". Stunde frei.")
              .append(note).append("\n");
        }
        return sb.toString().trim();
    }

    private void loginAndShow() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Benutzername/Passwort fehlt.");
            return;
        }

        loginBtn.setDisable(true);
        statusLabel.setText("Login und Daten werden geladen... Bitte warten.");
        
        // Execute heavy web-scraping on a background thread to prevent UI freezing
        new Thread(() -> {
            try {
                BonniwebClient client = new BonniwebClient();
                if (!client.login(user, pass)) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Login fehlgeschlagen.");
                        loginBtn.setDisable(false);
                    });
                    return;
                }

                // 1. Fetch User Courses
                List<String> courses = client.fetchCourses();
                
                // 2. Fetch User/Grade Timetable PDF
                String timetableText = "";
                String timetableWarning = "";
                try {
                    timetableText = client.fetchPdfText(TIMETABLE_PDF_URL);
                } catch (Exception ignored) {}
                
                if (timetableText == null || timetableText.trim().isEmpty()) {
                    timetableWarning = " Stundenplan konnte nicht gelesen werden.";
                }
                
                // 3. Fetch current Substitution plan (Vertretungsplan)
                String planText = client.fetchPlanTextFromResource(PLAN_RESOURCE_URL);
                if (planText.isEmpty()) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Plan nicht erreichbar.");
                        loginBtn.setDisable(false);
                    });
                    return;
                }

                UntisParser parser = new UntisParser();
                Map<String, List<PlanEntry>> allByDay = parser.parse(planText);
                EvaOverlapService overlapService = new EvaOverlapService();
                LinkedHashMap<String, List<EvaMatch>> filtered = overlapService.filterEva(allByDay, courses, timetableText);

                List<String> days = new ArrayList<>(parser.splitByDay(planText).keySet());
                String dayToday = parser.firstAvailableDay(planText);
                String dayTomorrow = null;
                if (dayToday != null) {
                    int idx = days.indexOf(dayToday);
                    if (idx >= 0 && idx + 1 < days.size()) dayTomorrow = days.get(idx + 1);
                }

                // Finalize data for UI Thread injection
                final String fDayToday = dayToday;
                final String fDayTomorrow = dayTomorrow;
                final String fTimetableWarning = timetableWarning;
                final String fTimetableText = timetableText;
                final List<String> fCourses = courses;

                // Bring results back to the GUI Thread safely
                Platform.runLater(() -> {
                    // Update Vertretungen view
                    todayTitle.setText(fDayToday != null ? "Heute (" + fDayToday + ")" : "Heute");
                    tomorrowTitle.setText(fDayTomorrow != null ? "Morgen (" + fDayTomorrow + ")" : "Morgen");

                    List<EvaMatch> listToday = fDayToday == null
                            ? Collections.emptyList()
                            : filtered.getOrDefault(fDayToday, Collections.emptyList());
                    List<EvaMatch> listTomorrow = fDayTomorrow == null
                            ? Collections.emptyList()
                            : filtered.getOrDefault(fDayTomorrow, Collections.emptyList());

                    todayArea.setText(formatList(listToday));
                    tomorrowArea.setText(formatList(listTomorrow));
                    
                    // Update Courses Tab
                    coursesList.getItems().setAll(fCourses.isEmpty() ? List.of("Keine Kurse gefunden.") : fCourses);
                    
                    // Update Timetable Tab
                    timetableArea.setText(fTimetableText != null && !fTimetableText.trim().isEmpty() 
                                    ? fTimetableText : "Kein Stundenplan gefunden/lesbar.");

                    statusLabel.setText("Erfolgreich geladen." + fTimetableWarning);
                    loginBtn.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("Fehler beim Abrufen der Daten: " + ex.getMessage());
                    loginBtn.setDisable(false);
                });
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
