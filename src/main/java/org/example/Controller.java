package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import javax.sound.sampled.UnsupportedAudioFileException;

public class Controller {
    public ObservableList<String> waveFiles = FXCollections.observableArrayList();
    public static List<String> waveFilesAbsPath = new ArrayList<String>();
    FileChooser fileChooser = new FileChooser();
    DirectoryChooser directoryChooser = new DirectoryChooser();
    public enum FileStatus {
        UNPROCESSED,
        PROCESSING,
        PROCESSED
    }

    public Map<String, FileStatus> fileStatusMap = new ConcurrentHashMap<>();
    public static String txtDirPath;
    public String startPath;
    public static double ProgressBarValue;
    public static WavPlayer rec;
    @FXML
    private CheckBox allowPakReverseFrames;
    @FXML
    private Button DecButton;

    @FXML
    private TextArea IsxTA;
    @FXML
    private TextArea originalLangTF;
    @FXML
    private Label LabelLang;

    @FXML
    private Label LabelPervious;

    @FXML
    public ProgressBar ProgressBarMainStage;

    @FXML
    private Menu menuHelp;

    @FXML
    private MenuItem menuOpenDec;

    @FXML
    private Menu menuOpenFile;

    @FXML
    private Button StopButton1;
    @FXML
    private MenuItem menuOpenTable;

    @FXML
    private MenuItem menuSaveResult;

    @FXML
    private AnchorPane myPane;

    @FXML
    private ScrollPane parentPane;

    @FXML
    private SplitPane superParentPane;

    @FXML
    private VBox vbMenu;
    @FXML
    private ListView<String> ListOfFiles;
    @FXML
    private Button RefreshButton;
    @FXML
    private ComboBox<String> cbSelectDevice;

    @FXML
    private ComboBox<String> cbSelectLang;

    @FXML
    private ComboBox<String> cbSelectModelSize;

    @FXML
    private ComboBox<String> cbSelectDurationFilter;
    @FXML
    private Button setDefaultSettingsButton;
    @FXML
    private CheckBox allowServiceMessages;
    @FXML
    private CheckBox allowCopyFile;
    @FXML
    private CheckBox allowDirPipeline;
    public String whisperLanguage;
    public String whisperDevice;
    public String whisperModelSize;
    @FXML
    private CheckBox saveOriginalFlag;
    @FXML
    private CheckBox allowPakReverseBytes;

    public boolean allowCopyWav;
    private List<String> languages = new ArrayList<>();
    private Map<String, String> languageMap = new HashMap<>();
    private boolean decButtonStopFlag = false;
    private Service<Void> service2;
    private Service<Void> service3;

    @FXML
    void initialize() throws IOException, URISyntaxException {
        directoryChooser.setTitle("Выберите папку");
        getListWave("Basic");
        getTxtDirPath();
        setLanguages();
        setFilter();
        DecButton.setDisable(false);
        cbSelectDevice.getItems().add("cpu");
        cbSelectDevice.getItems().add("gpu");
        cbSelectModelSize.getItems().add("large");
        cbSelectModelSize.getItems().add("medium");
        cbSelectModelSize.getItems().add("small");
        cbSelectModelSize.getItems().add("base");
        cbSelectModelSize.getItems().add("tiny");
        cbSelectDurationFilter.setValue("5");
        whisperDevice = "cpu";
        whisperModelSize = "small";
        allowServiceMessages.setSelected(false);

        whisperLanguage = languageMap.entrySet().stream()
                .filter(entry -> "Автоопределение".equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        cbSelectLang.setOnAction(e -> {
            String russianName = cbSelectLang.getValue();
            whisperLanguage = languageMap.entrySet().stream()
                    .filter(entry -> russianName.equals(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(null);
        });
        cbSelectDevice.setOnAction(e -> {
            whisperDevice = cbSelectDevice.getValue();
            DecButton.setDisable(false);
        });
        cbSelectModelSize.setOnAction(e -> {
            whisperModelSize = cbSelectModelSize.getValue();
        });
        RefreshButton.setOnAction(event -> {
            try {
                getListWave("Basic");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
        setDefaultSettingsButton.setOnAction(e -> {
            cbSelectDevice.setValue("cpu");
            cbSelectLang.setValue("Английский");
            cbSelectModelSize.setValue("small");
            cbSelectDurationFilter.setValue("5");
            DecButton.setDisable(false);
        });
        DecButton.setOnAction(event -> {
            if (!decButtonStopFlag) {
                service2 = createNewService(true, allowDirPipeline.isSelected()); // Метод для создания нового сервиса
                service2.start();
                IsxTA.clear();
                if (saveOriginalFlag.isSelected()) {
                    service3 = createNewService(false, allowDirPipeline.isSelected()); // Метод для создания нового сервиса
                    service3.start();
                    originalLangTF.clear();
                }
            } else {
                service2.cancel();
            }
        });


        menuOpenDec.setOnAction(event -> {
            try {
                getListWave("Custom");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

        ListOfFiles.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                    if (mouseEvent.getClickCount() == 2) {
                        rec = new WavPlayer(waveFilesAbsPath.get(ListOfFiles.getSelectionModel().getSelectedIndex()), Controller.this);
                        Riffer rif1 = new Riffer(rec.getDurationInSeconds(), rec, Controller.this);
                        rif1.CreateWindow(waveFilesAbsPath.get(ListOfFiles.getSelectionModel().getSelectedIndex()));
                        rif1.primaryStage.setOnCloseRequest(c -> {
                            rec.close();
                            rec = null;
                            System.gc();
                        });
                    }
                }

            }
        });

        ListOfFiles.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String absPath = waveFilesAbsPath.get(getIndex());
                    String normalizedPath = Paths.get(absPath).toString();
                    FileStatus status = fileStatusMap.getOrDefault(normalizedPath, FileStatus.UNPROCESSED);

                    // Установка цвета текста и статуса
                    switch (status) {
                        case PROCESSING:
                            setTextFill(Color.CYAN);
                            setText(item + " 🔄");
                            break;
                        case PROCESSED:
                            setTextFill(Color.LIME);
                            setText(item + " ✅");
                            break;
                        default:
                            setTextFill(Color.LIGHTGRAY);
                            setText(item);
                            break;
                    }
                }
            }
        });


    }

    private void getListWave(String modify) throws URISyntaxException {
        waveFiles.clear();
        waveFilesAbsPath.clear();
        if (modify.equals("Basic")) {
            String jarPath = GUIStarter.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File jarFile1 = new File(jarPath);
            String wavFolderPath = jarFile1.getParent() + File.separator + "wav";
            File jarFile = new File(wavFolderPath);
            System.out.println(wavFolderPath);
            System.out.println(jarFile.getPath());
            File[] listOfFiles = jarFile.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].getName().endsWith(".wav")) {
                    waveFiles.add(listOfFiles[i].getName());
                    waveFilesAbsPath.add(listOfFiles[i].getAbsolutePath());
                }
            }
        }
        if (modify.equals("Custom")) {
            File selectedDirectory = directoryChooser.showDialog(myPane.getScene().getWindow());

            File[] listOfFiles = selectedDirectory.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                waveFiles.add(listOfFiles[i].getName());
                waveFilesAbsPath.add(listOfFiles[i].getAbsolutePath());
            }
        }
        ListOfFiles.setItems(waveFiles);
    }

    public void pakToWav(boolean reverseBytes, boolean reverseFrames) {
        String currentDir = Paths.get("").toAbsolutePath().toString();
        String rifferPath = Paths.get(currentDir, "riffer2", "Directory9000Converter.exe").toString();
        String voiceConverterPath = Paths.get(currentDir, "riffer2", "Voice7000Converter.exe").toString();
        String inputDirectory = Paths.get(currentDir, "pak").toString();
        String outputDirectory = Paths.get(currentDir, "wav").toString();
        String failedFilesDirectory = Paths.get(currentDir, "backup_pak").toString();
        List<String> command = new ArrayList<>();
        command.add(rifferPath);
        command.add("--input-directory");
        command.add(inputDirectory);
        command.add("--output-directory");
        command.add(outputDirectory);
        command.add("--encoder");
        command.add("pcm");
        command.add("--container");
        command.add("wav");
        command.add("--copy-failed-files-dir");
        command.add(failedFilesDirectory);
        command.add("--mode");
        command.add("onepass");
        // command.add("--voice7000converter");
        // command.add(voiceConverterPath);

        command.add("--save-folder-structure");
        command.add("0");
        command.add("--restore-pause");
        command.add("0");
        command.add("--restore-pause-dpx");
        command.add("0");
        command.add("--limit-pause");
        command.add("0");
        command.add("--restore-pauses-limit");
        command.add("0");
        if (reverseBytes) {
            command.add("-b");
            command.add("pcm;pcm_12;pcm_16;pcm_24;pcm_32;pcm_44;pcm_48;pcm_96;pcm_s;pcm_12_s;pcm_16_s;pcm_24_s;pcm_32_s;pcm_44_s;pcm_48_s;pcm_96_s;cdda;g711_a;g711_mu;g728_0960;g728_1280;g728_16;g728_40;g7231_5333;g7231_6400;g722_48;g722_56;g722_64;g729_1180;g729_0640;g729_08;g729_08_fr11;imbe;nc_sig;nc48;nc56;nc64;nc72;nc80;nc88;nc96;g7221_16_24;g7221_16_32;g7221_32_24;g7221_32_32;g7221_32_48;srn22_32;srn22_48;srn22_64;srn22s_64;srn22s_96;srn22s_128;srn14s_24;srn14s_32;srn14s_48;gsm;tsp;g719_sig;g719_112;g719_120;g719_128;g719_32;g719_36;g719_40;g719_44;g719_48;g719_52;g719_56;g719_60;g719_64;g719_68;g719_72;g719_76;g719_80;g719_84;g719_88;g719_96;g719_s_64;g719_s_96;g719_s_128;g719_s_192;g719_s_224;g719_s_256;g719_s_sig;g7111_r1_a;g7111_r1_u;g7111_r2a_a;g7111_r2a_u;g7111_r2b_a;g7111_r3_a;g7111_r2b_u;g7111_r3_u;g7111d_r3sm_a;g7111d_r4sm_a;g7111d_r4ssm_a;g7111d_r5ssm_a;g7111d_r3sm_u;g7111d_r4sm_u;g7111d_r4ssm_u;g7111d_r5ssm_u;g718_8_8;g718_8_12;g718_16_8;g718_16_12;g718_16_16;g718_16_24;g718_32_16;g718_32_8;g718_32_12;g718_16_32;g718_32_24;g718_32_28;g718_32_32;g718_32_36;g718_32_40;g718_32_48;g7291_sig;g7291_16_08;g7291_16_12;g7291_16_14;g7291_16_16;g7291_16_18;g7291_16_20;g7291_16_22;g7291_16_24;g7291_16_26;g7291_16_28;g7291_16_30;g7291_16_32;g7291_32_36;g7291_32_40;g7291_32_48;g7291_32_56;g7291_32_64;g7110_a_sig;g7110_u;dm12;dm16;dm32;dm24;g7231navy_6400;lpc;g7111f_16_96_a;g7111f_16_128_a;g7111f_32_112_a;g7111f_32_128_a;g7111f_32_144(80)_a;g7111f_32_144(96)_a;g7111f_32_160_a;g7111f_16_96_u;g7111f_16_128_u;g7111f_32_112_u;g7111f_32_128_u;g7111f_32_144(80)_u;g7111f_32_144(96)_u;g7111f_32_160_u;g722b_64;g722b_80;g722b_96;g722d_16_64;g722d_16_80;g722d_32_80;g722d_32_96;g722d_32_112;g722d_32_128;");
        } else {
            command.add("-b \"\"");
        }
        if (reverseFrames) {
            command.add("-f");
            command.add("pcm;pcm_12;pcm_16;pcm_24;pcm_32;pcm_44;pcm_48;pcm_96;pcm_s;pcm_12_s;pcm_16_s;pcm_24_s;pcm_32_s;pcm_44_s;pcm_48_s;pcm_96_s;cdda;g711_a;g711_mu;g728_0960;g728_1280;g728_16;g728_40;g7231_5333;g7231_6400;g722_48;g722_56;g722_64;g729_1180;g729_0640;g729_08;melp;melpe;nc_sig;nc48;nc56;nc64;nc72;nc80;nc88;nc96;g7221_16_24;g7221_16_32;g7221_32_24;g7221_32_32;g7221_32_48;srn22_32;srn22_48;srn22_64;srn22s_64;srn22s_96;srn22s_128;srn14s_24;srn14s_32;srn14s_48;");
        } else {
            command.add("-f \"\"");
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> processFuture = executor.submit(() -> {
            Process process = null;
            try {
                process = builder.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                }
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                if (process != null) process.destroyForcibly();
                throw new RuntimeException(e);
            }
        });

        try {
            processFuture.get(5, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            processFuture.cancel(true);
            System.out.println("Процесс прерван из-за истечения времени ожидания");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
        }
    }

    private void getTxtDirPath() throws URISyntaxException, IOException {
        String jarPath = GUIStarter.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        File jarFile1 = new File(jarPath);
        txtDirPath = jarFile1.getParent() + File.separator + "txt";
    }

    public void setPB(double value) {
        ProgressBarMainStage.setProgress(value / 100);
    }

    public void ClearTextArea() {
        IsxTA.clear();
    }

    public void setToTextArea(String tex) {
        Platform.runLater(() -> {
            IsxTA.appendText(tex);
        });
    }
    public void setToEnglishTextArea(String tex) {
        Platform.runLater(() -> {
            originalLangTF.appendText(tex);
        });
    }
    private void addLanguage(String englishName, String russianName) {
        languages.add(englishName);
        languageMap.put(englishName, russianName);
    }

    private void setLanguages() {
        addLanguage("None", "Автоопределение");
        addLanguage("English", "Английский");
        addLanguage("Russian", "Русский");
        addLanguage("Arabic", "Арабский");
        addLanguage("Korean", "Корейский");
        addLanguage("Vietnamese", "Вьетнамский");
        addLanguage("Japanese", "Японский");
        addLanguage("Spanish", "Испанский");
        addLanguage("Afrikaans", "Африкаанс");
        addLanguage("Albanian", "Албанский");
        addLanguage("Amharic", "Амхарский");
        addLanguage("Armenian", "Армянский");
        addLanguage("Assamese", "Ассамский");
        addLanguage("Awadhi", "Авадхи");
        addLanguage("Azerbaijani", "Азербайджанский");
        addLanguage("Belarusian", "Белорусский");
        addLanguage("Bengali", "Бенгальский");
        addLanguage("Bhili", "Бхили");
        addLanguage("Bhojpuri", "Бходжпури");
        addLanguage("Bosnian", "Боснийский");
        addLanguage("Bulgarian", "Болгарский");
        addLanguage("Burmese", "Бирманский");
        addLanguage("Cantonese", "Кантонский");
        addLanguage("Catalan", "Каталанский");
        addLanguage("Croatian", "Хорватский");
        addLanguage("Czech", "Чешский");
        addLanguage("Danish", "Датский");
        addLanguage("Dutch", "Голландский");
        addLanguage("Dzongkha", "Дзонг-кэ");
        addLanguage("Estonian", "Эстонский");
        addLanguage("Finnish", "Финский");
        addLanguage("French", "Французский");
        addLanguage("Galician", "Галисийский");
        addLanguage("Georgian", "Грузинский");
        addLanguage("German", "Немецкий");
        addLanguage("Greek", "Греческий");
        addLanguage("Gujarati", "Гуджарати");
        addLanguage("Haryanvi", "Харьянви");
        addLanguage("Hausa", "Хауса");
        addLanguage("Hebrew", "Иврит");
        addLanguage("Hindi", "Хинди");
        addLanguage("Hungarian", "Венгерский");
        addLanguage("Igbo", "Игбо");
        addLanguage("Indonesian", "Индонезийский");
        addLanguage("Italian", "Итальянский");
        addLanguage("Kannada", "Каннада");
        addLanguage("Kazakh", "Казахский");
        addLanguage("Khmer", "Кхмерский");
        addLanguage("Kinyarwanda", "Киньяруанда");
        addLanguage("Kurdish", "Курдский");
        addLanguage("Kyrgyz", "Киргизский");
        addLanguage("Lao", "Лаосский");
        addLanguage("Latvian", "Латышский");
        addLanguage("Lithuanian", "Литовский");
        addLanguage("Macedonian", "Македонский");
        addLanguage("Magahi", "Магахи");
        addLanguage("Maithili", "Майтхили");
        addLanguage("Malagasy", "Малагасийский");
        addLanguage("Malay", "Малайский");
        addLanguage("Malayalam", "Малаялам");
        addLanguage("Mandarin", "Мандаринский");
        addLanguage("Marathi", "Маратхи");
        addLanguage("Nepali", "Непальский");
        addLanguage("Newar", "Невар");
        addLanguage("Norwegian", "Норвежский");
        addLanguage("Oriya", "Ория");
        addLanguage("Pashto", "Пушту");
        addLanguage("Persian", "Персидский");
        addLanguage("Polish", "Польский");
        addLanguage("Portuguese", "Португальский");
        addLanguage("Punjabi", "Панджаби");
        addLanguage("Romanian", "Румынский");
        addLanguage("Serbian", "Сербский");
        addLanguage("Shona", "Шона");
        addLanguage("Sinhala", "Сингальский");
        addLanguage("Slovak", "Словацкий");
        addLanguage("Slovenian", "Словенский");
        addLanguage("Somali", "Сомалийский");
        addLanguage("Swahili", "Суахили");
        addLanguage("Swedish", "Шведский");
        addLanguage("Tajik", "Таджикский");
        addLanguage("Tamil", "Тамильский");
        addLanguage("Telugu", "Телугу");
        addLanguage("Thai", "Тайский");
        addLanguage("Tibetan", "Тибетский");
        addLanguage("Turkish", "Турецкий");
        addLanguage("Turkmen", "Туркменский");
        addLanguage("Ukrainian", "Украинский");
        addLanguage("Urdu", "Урду");
        addLanguage("Uzbek", "Узбекский");
        addLanguage("Xhosa", "Коса");
        addLanguage("Yoruba", "Йоруба");
        addLanguage("Zulu", "Зулу");
        for (String language : languages) {
            cbSelectLang.getItems().add(languageMap.get(language));
        }
    }

    public void setFilter() {
        for (int i = 2; i < 30; i++) {
            cbSelectDurationFilter.getItems().add(String.valueOf(i));
        }
    }

    public boolean getServiceMessagesStatus() {
        return allowServiceMessages.isSelected();
    }

    public long getDurationFilter() {
        return Long.parseLong(cbSelectDurationFilter.getValue());
    }

    private Service<Void> createNewService(boolean isTargetLang, boolean _allowDirPipeline) {
        Service<Void> service = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        try {
                            if (isTargetLang) {
                                if (checkForPakPaths()) {
                                    pakToWav(allowPakReverseBytes.isSelected(), allowPakReverseFrames.isSelected());
                                    waveFilesAbsPath.clear();
                                    getListWave("Basic");
                                }
                                StartAutoRecognize SAR = new StartAutoRecognize(Controller.this);
                                SAR.startRec(waveFilesAbsPath, whisperLanguage, whisperDevice, whisperModelSize, allowServiceMessages.isSelected(),
                                        Long.parseLong(cbSelectDurationFilter.getValue()), allowCopyFile.isSelected(), true, _allowDirPipeline);
                            } else {
                                StartAutoRecognize SAR = new StartAutoRecognize(Controller.this);
                                SAR.startRec(waveFilesAbsPath, "Russian", whisperDevice, "small", allowServiceMessages.isSelected(),
                                        Long.parseLong(cbSelectDurationFilter.getValue()), allowCopyFile.isSelected(), false,_allowDirPipeline);
                            }
                        } catch (UnsupportedAudioFileException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                };
            }
        };
        service.setOnRunning(event1 -> {
            DecButton.setText("Стоп");
            decButtonStopFlag = true;
        });
        service.setOnSucceeded(event1 -> {
            DecButton.setText("Старт");
            decButtonStopFlag = false;
        });
        service.setOnCancelled(e -> {
            DecButton.setText("Старт");
            decButtonStopFlag = false;
        });
        return service;
    }

    public boolean checkForPakPaths() {
        String currentDir = Paths.get("").toAbsolutePath().toString();
        Path pakDirectory = Paths.get(currentDir, "pak");

        try (Stream<Path> paths = Files.walk(pakDirectory)) {
            return paths.anyMatch(Files::isRegularFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    public void updateFileStatus(String filePath, FileStatus status) {
        String normalizedPath = Paths.get(filePath).toString();
        fileStatusMap.put(normalizedPath, status);

        Platform.runLater(() -> ListOfFiles.refresh());
    }



}

