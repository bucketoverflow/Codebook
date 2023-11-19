package SidebarButton;

import com.github.bucketoverflow.codebook.CodebookAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import com.github.bucketoverflow.codebook.*;

import java.awt.*;


public class CodebookButtonBuilder implements ToolWindowFactory{
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Create UI components for your tool window
        JPanel panel = new JPanel();

        JButton yesButton = new JButton("Yes");
        yesButton.addActionListener(e -> replaceFile());
        JLabel label = new JLabel("Replace your current files?");
        JButton noButton = new JButton("No");
        noButton.addActionListener(e -> discardGeneratedFiles(label));







        panel.add(BorderLayout.EAST,yesButton);
        panel.add(BorderLayout.WEST,noButton);
        panel.add(BorderLayout.NORTH,label);

        // Add action listener to the button

        panel.add(BorderLayout.WEST,yesButton);
        panel.add(BorderLayout.EAST,noButton);
        panel.add(BorderLayout.NORTH,label);


        // Create content for the tool window
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);

        }
        private void replaceFile () {

        }
        private void discardGeneratedFiles (JLabel label) {
            label.setText("Files got discarded");
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

