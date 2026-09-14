package com.silenoids.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.silenoids.control.Microphone;
import com.silenoids.control.Player;
import com.silenoids.control.Recorder;
import com.silenoids.control.Sandglass;
import com.silenoids.utils.FileUtils;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.prefs.Preferences;

public class MainView {

    private Preferences preferences;
    private final Player player;
    private final Recorder recorder;

    public JPanel mainPanel;
    private JButton inputDirBtn;
    private JButton outputDirBtn;
    private JList<String> fileList;
    private JButton playInputBtn;
    private JButton recordOutputButton;
    private JButton playOutputButton;
    private JLabel inputTime;
    private JProgressBar sandglassBar;
    private JCheckBox autoplayBox;
    private JButton donateBtn;
    private JButton helpBtn;
    private JComboBox<Microphone> micComboBox;

    private String inputDirPath;
    private String outputDirPath;
    private DefaultListModel<String> inputFileListModel;
    private DefaultComboBoxModel<Microphone> micBoxModel;
    private String copiedOutputFileName;

    public MainView() {
        setupComponents();
        setupHandlers();

        loadPreferences();

        player = new Player();
        recorder = new Recorder();
    }

    private void setupComponents() {
        Sandglass.getInstance(sandglassBar);
        inputFileListModel = new DefaultListModel<>();
        micBoxModel = new DefaultComboBoxModel<>();
        fileList.setModel(inputFileListModel);
        micComboBox.setModel(micBoxModel);
        micComboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel jLabel = new JLabel();
            if (index == -1 && recorder.getMicrophone() == null) {
                jLabel.setText("Default microphone");
            } else {
                // TODO: remove text
                jLabel.setText("Work in progress - " + value.toString());
                jLabel.setToolTipText(String.valueOf(value.getLineInfo()));
            }
            return jLabel;
        });

        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixerInfos) {
            System.out.println("Mixer: " + mixerInfo);
            Mixer currentMixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] lineInfos = currentMixer.getSourceLineInfo();

            for (Line.Info lineInfo : lineInfos) {
                try {
                    System.out.println("\tLine: " + lineInfo);
                    Line line = currentMixer.getLine(lineInfo);
                    micBoxModel.addElement(new Microphone(currentMixer, mixerInfo, line, lineInfo));
                    System.out.println("\t\tInstance: " + line);
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                }
            }
        }

        //TODO: enable
        //        micComboBox.addActionListener(e -> recorder.setMicrophone(micBoxModel.getElementAt(micComboBox.getSelectedIndex())));

        fileList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            Color bgColor = UIManager.getColor("List.dropCellBackground");
            Color fgColor = UIManager.getColor("List.dropCellForeground");
            Color fgExistsColor = UIManager.getColor("Component.linkColor");

            DefaultListCellRenderer renderer = new DefaultListCellRenderer();
            boolean outputFileExists = FileUtils.fileExists(outputDirPath, value);

            renderer.setEnabled(list.isEnabled());
            renderer.setFont(list.getFont());

            Border border = null;
            if (cellHasFocus) {
                if (isSelected) {
                    border = UIManager.getBorder("List.focusSelectedCellHighlightBorder");
                }
                if (border == null) {
                    border = UIManager.getBorder("List.focusCellHighlightBorder");
                }
            } else {
                border = UIManager.getBorder("List.cellNoFocusBorder");
            }
            renderer.setBorder(border);

            if (isSelected) {
                renderer.setBackground(bgColor == null ? list.getSelectionBackground() : bgColor);
                renderer.setForeground(fgColor == null ? list.getSelectionForeground() : fgColor);
            } else {
                renderer.setBackground(list.getBackground());
                renderer.setForeground(list.getForeground());
            }

            if (outputFileExists) {
                renderer.setText(" ■ " + value);
                renderer.setForeground(fgExistsColor);
            } else {
                renderer.setText(value);
            }

            return renderer;
        });
    }

    private void setupHandlers() {
        inputDirBtn.addActionListener((ActionEvent e) -> {
            // Select dir
            JFileChooser fileChooser;
            if (inputDirPath != null && !inputDirPath.equals("Input Directory")) {
                fileChooser = new JFileChooser(inputDirPath);
            } else {
                fileChooser = new JFileChooser();
            }
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.showOpenDialog(mainPanel);
            File selectedDirFile = fileChooser.getSelectedFile();

            inputDirSetup(selectedDirFile);
        });

        outputDirBtn.addActionListener((ActionEvent e) -> {

            JFileChooser fileChooser;
            if (outputDirPath != null && !outputDirPath.equals("Output Directory")) {
                fileChooser = new JFileChooser(outputDirPath);
            } else {
                fileChooser = new JFileChooser();
            }
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.showOpenDialog(mainPanel);
            File selectedDirPath = fileChooser.getSelectedFile();

            outputDirSetup(selectedDirPath);
        });

        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                if (inputDirPath == null || outputDirPath == null) {
                    sendMessage("Both input and output directories have to be selected");
                    return;
                }

                if (player.isPlaying()) {
                    player.stop();
                }
                player.loadAudioFile(inputDirPath, fileList.getSelectedValue());
                inputTime.setText(player.getDurationText());

                playInputBtn.setEnabled(FileUtils.fileExists(inputDirPath, fileList.getSelectedValue()));
                playOutputButton.setEnabled(FileUtils.fileExists(outputDirPath, fileList.getSelectedValue()));
                recordOutputButton.setEnabled(true);

                if (autoplayBox.isSelected()) {
                    player.play();
                }
            }
        });

        fileList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK), "playInputAudio");
        fileList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK), "playOutputAudio");
        fileList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "recordOutputAudio");
        fileList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copyFile");
        fileList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "pasteFiles");

        fileList.getActionMap().put("playInputAudio", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                player.loadAudioFile(inputDirPath, fileList.getSelectedValue());
                player.play();
            }
        });
        fileList.getActionMap().put("playOutputAudio", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!FileUtils.fileExists(outputDirPath, fileList.getSelectedValue())) {
                    sendMessage("No recorded audio found");
                    return;
                }
                player.loadAudioFile(outputDirPath, fileList.getSelectedValue());
                player.play();
            }
        });
        fileList.getActionMap().put("recordOutputAudio", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRecordingStateView(true);
                startRecordingProcess();
                setRecordingStateView(false);
                fileList.requestFocus();
            }
        });
        fileList.getActionMap().put("copyFile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!FileUtils.fileExists(outputDirPath, fileList.getSelectedValue())) {
                    sendMessage("The output recorded file does not exists");
                    return;
                }
                if (fileList.getSelectedValuesList().size() != 1) {
                    sendMessage("You can only copy one single output file to replicate");
                    return;
                }
                copiedOutputFileName = fileList.getSelectedValue();
            }
        });
        fileList.getActionMap().put("pasteFiles", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (copiedOutputFileName == null || copiedOutputFileName.isBlank()) {
                    sendMessage("Before replicating, you must copy an output file");
                    return;
                }
                fileList.getSelectedValuesList().forEach(replicatedFileName -> {
                    try {
                        Files.copy(
                                Path.of(outputDirPath, copiedOutputFileName),
                                Path.of(outputDirPath, replicatedFileName),
                                StandardCopyOption.REPLACE_EXISTING
                        );
                    } catch (IOException ex) {
                        sendMessage("Something went wrong during replication");
                    }
                });
            }
        });

        playOutputButton.addActionListener(e -> {
            player.loadAudioFile(outputDirPath, fileList.getSelectedValue());
            player.play();
        });

        playInputBtn.addActionListener(e -> {
            if (inputDirPath == null || outputDirPath == null) {
                sendMessage("Both input and output directories have to be selected");
                return;
            }
            player.loadAudioFile(inputDirPath, fileList.getSelectedValue());
            player.play();
        });

        recordOutputButton.addActionListener(e -> {
            setRecordingStateView(true);
            startRecordingProcess();
            setRecordingStateView(false);
        });

        autoplayBox.addActionListener(e -> preferences.putBoolean("autoplayEnabled", autoplayBox.isSelected()));

        donateBtn.addActionListener(e -> {
            try {
                Desktop desktop = Desktop.getDesktop();
                desktop.browse(URI.create("https://www.buymeacoffee.com/sileno"));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        helpBtn.addActionListener(e -> {
            InfoDialog dialog = new InfoDialog();
            dialog.setTitle("Help");
            dialog.setSize(new Dimension(600, 700));
            dialog.setLocationRelativeTo(mainPanel);
            dialog.setVisible(true);
            dialog.dispose();
        });

    }

    private void outputDirSetup(File selectedDirPath) {
        if (selectedDirPath != null && !selectedDirPath.getPath().isBlank()) {
            outputDirPath = selectedDirPath.getPath();
            outputDirBtn.setText(outputDirPath);
            preferences.put("outputDir", selectedDirPath.getAbsolutePath());
        }
    }

    private void inputDirSetup(File selectedDirFile) {
        if (selectedDirFile != null && selectedDirFile.exists()) {
            inputDirPath = selectedDirFile.getPath();
            inputDirBtn.setText(inputDirPath);

            File[] inputFiles = selectedDirFile.listFiles();
            if (inputFiles != null) {
                inputFileListModel.clear();
                Arrays.stream(inputFiles)
                        .sorted()
                        .forEach(file -> {
                            if (file.getName().endsWith(".wav")) {
                                inputFileListModel.addElement(file.getName());
                            }
                        });
            }

            preferences.put("inputDir", selectedDirFile.getAbsolutePath());
        }
    }

    private void startRecordingProcess() {
        if (inputDirPath == null || outputDirPath == null) {
            sendMessage("Both input and output directories have to be selected");
            return;
        }

        new Thread(() -> {
            setRecordingStateView(true);
            player.loadAudioFile(inputDirPath, fileList.getSelectedValue());
            recorder.stop();
            Sandglass.getInstance().startSandglass(player.getDurationInMillis());
            recorder.startWithAlias(inputDirPath, fileList.getSelectedValue());
            try {
                Thread.sleep(player.getDurationInMillis() + 200);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            recorder.stop();

            try {
                Thread.sleep(300);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            FileUtils.saveAudioStreamToFile(outputDirPath, fileList.getSelectedValue(), recorder.getAudioInputStream());
            //            printThreads();
            setRecordingStateView(false);

        }, " MainView recording thread").start();

    }

    private void setRecordingStateView(boolean isRecording) {
        fileList.setEnabled(!isRecording);
        playInputBtn.setEnabled(!isRecording);
        recordOutputButton.setEnabled(!isRecording);
        inputDirBtn.setEnabled(!isRecording);
        outputDirBtn.setEnabled(!isRecording);
        recordOutputButton.setEnabled(!isRecording);
        playOutputButton.setEnabled(!isRecording);
    }

    public void dispose() {
        System.out.println("Nothing special to dispose");
    }

    private void sendMessage(String msg) {
        JOptionPane.showMessageDialog(mainPanel, msg);
    }

    // TODO: move to preference class
    private void loadPreferences() {
        preferences = Preferences.userNodeForPackage(MainView.class);
        autoplayBox.setSelected(preferences.getBoolean("autoplayEnabled", true));
        String prefInputDir = preferences.get("inputDir", null);
        if (prefInputDir != null && !prefInputDir.isBlank()) {
            inputDirSetup(new File(prefInputDir));
        }
        String prefOutputDir = preferences.get("outputDir", null);
        if (prefOutputDir != null && !prefOutputDir.isBlank()) {
            outputDirSetup(new File(prefOutputDir));
        }
    }

    {
        // GUI initializer generated by IntelliJ IDEA GUI Designer
        // >>> IMPORTANT!! <<<
        // DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT edit this method OR call it in your
     * code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(12, 12, 12, 12), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2,
                new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
                        0, false));
        outputDirBtn = new JButton();
        outputDirBtn.setText("Output Directory");
        panel2.add(outputDirBtn,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        inputDirBtn = new JButton();
        inputDirBtn.setEnabled(true);
        inputDirBtn.setText("Input Directory");
        panel2.add(inputDirBtn,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1,
                new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null,
                        0, false));
        fileList = new JList();
        fileList.setLayoutOrientation(1);
        fileList.setOpaque(true);
        fileList.setVisible(true);
        fileList.setVisibleRowCount(100);
        scrollPane1.setViewportView(fileList);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        playInputBtn = new JButton();
        playInputBtn.setEnabled(false);
        playInputBtn.setText("Play Input");
        panel3.add(playInputBtn,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1,
                new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        recordOutputButton = new JButton();
        recordOutputButton.setEnabled(false);
        recordOutputButton.setText("Record Output");
        panel3.add(recordOutputButton,
                new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        playOutputButton = new JButton();
        playOutputButton.setEnabled(false);
        playOutputButton.setText("Play Output");
        panel3.add(playOutputButton,
                new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        autoplayBox = new JCheckBox();
        autoplayBox.setSelected(true);
        autoplayBox.setText("Autoplay");
        panel3.add(autoplayBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel4.setEnabled(true);
        panel1.add(panel4, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        sandglassBar = new JProgressBar();
        panel4.add(sandglassBar,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        inputTime = new JLabel();
        inputTime.setText("0:00");
        panel4.add(inputTime, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0,
                false));
        donateBtn = new JButton();
        donateBtn.setIcon(new ImageIcon(getClass().getResource("/donateBtn.png")));
        donateBtn.setText("");
        donateBtn.setToolTipText("I'm sleepy");
        panel5.add(donateBtn,
                new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel5.add(spacer2,
                new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        helpBtn = new JButton();
        helpBtn.setEnabled(true);
        helpBtn.setFocusable(true);
        helpBtn.setIcon(new ImageIcon(getClass().getResource("/information-button.png")));
        helpBtn.setMargin(new Insets(1, 1, 1, 1));
        helpBtn.setOpaque(true);
        helpBtn.setText("");
        panel5.add(helpBtn,
                new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        micComboBox = new JComboBox();
        micComboBox.setEditable(false);
        micComboBox.setEnabled(true);
        micComboBox.setMaximumRowCount(20);
        micComboBox.setName("");
        panel5.add(micComboBox,
                new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                        GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
                        false));
        final JLabel label1 = new JLabel();
        label1.setText("Selected microphone:");
        panel5.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /** @noinspection ALL */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
