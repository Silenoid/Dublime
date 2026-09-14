package com.silenoids;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.silenoids.utils.HttpClient;
import com.silenoids.view.MainView;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {

    public static void main(String[] args) {
        // Manage app crashes cases
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
        });

        // General properties settings
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        Logger.getLogger("com.goxr3plus.streamplayer.stream.StreamPlayer").setLevel(Level.OFF);

        // Flatlaf specific settings
        FlatLaf.setGlobalExtraDefaults(Map.of(
                "@accentColor", "#ffdd00",
                "@background" , "#2A2E24",
                "@foreground" , "#E0D3DE"
        ));
        FlatDarkLaf.setup();

        // Loading the icon
        URL appIcon = App.class.getClassLoader().getResource("micr.png");
        ImageIcon imageIcon = new ImageIcon(appIcon);

        // Main window frame creation
        SwingUtilities.invokeLater(() -> {
            MainView mainView = new MainView();
            JFrame frame = new JFrame("Dublime");
            frame.setSize(1600, 900);
            frame.setLocationRelativeTo(null);  //center
            frame.setContentPane(mainView.mainPanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setIconImage(imageIcon.getImage());
            frame.setVisible(true);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    mainView.dispose();
                    super.windowClosing(e);
                }
            });
        });
    }

}
