package tn.esprit.controllers;

import me.friwi.jcefmaven.CefAppBuilder;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * KinshipPortal manages the embedded Jitsi Meet session natively inside the app using JCEF.
 * We use a native Swing JFrame to bypass all JavaFX SwingNode OpenGL (JOGL) rendering crashes.
 */
public class KinshipPortal {

    private static CefApp cefApp;
    private static CefClient client;
    private JFrame frame;

    public void openPortal(int briefId, String username) {
        String roomName = "Mythoria_Kinship_" + briefId;
        String url = "https://meet.jit.si/" + roomName + "#userInfo.displayName=" + username.replace(" ", "%20") + "&config.disableDeepLinking=true";

        // Must run strictly on the Swing thread
        SwingUtilities.invokeLater(() -> {
            try {
                if (cefApp == null) {
                    CefAppBuilder builder = new CefAppBuilder();
                    // Auto-grant Camera and Microphone permissions (bypasses missing browser popup UI)
                    builder.addJcefArgs("--enable-media-stream", "--use-fake-ui-for-media-stream");
                    
                    // Crucial: Disable OSR to use stable, heavyweight native rendering.
                    // This entirely prevents the com.jogamp.opengl.GLException crashes!
                    builder.getCefSettings().windowless_rendering_enabled = false;
                    cefApp = builder.build();
                    client = cefApp.createClient();
                }

                // Create the browser pointing to Jitsi (false = disable OSR)
                CefBrowser browser = client.createBrowser(url, false, false);
                Component browserUI = browser.getUIComponent();

                // Create a native Swing Window instead of a JavaFX Stage
                frame = new JFrame("Mythoria Portal - Secure Video Link");
                frame.setSize(1000, 700);
                frame.setLocationRelativeTo(null); // Center on screen
                
                // Dark Fantasy Aesthetic
                frame.getContentPane().setBackground(new Color(17, 17, 17));
                
                // Add a Gold Border Panel to match the Forge theme
                JPanel borderPanel = new JPanel(new BorderLayout());
                borderPanel.setBackground(new Color(17, 17, 17));
                borderPanel.setBorder(BorderFactory.createLineBorder(new Color(212, 175, 55), 4));
                borderPanel.add(browserUI, BorderLayout.CENTER);

                frame.getContentPane().add(borderPanel, BorderLayout.CENTER);
                frame.setVisible(true);

                // Handle cleanup securely when the user closes the window
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        browser.close(true);
                        frame.dispose();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("❌ Failed to initialize Chromium Engine.");
            }
        });
    }
}
