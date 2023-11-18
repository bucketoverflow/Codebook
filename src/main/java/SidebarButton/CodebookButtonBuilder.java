package SidebarButton;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;


public class CodebookButtonBuilder implements ToolWindowFactory{
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Create UI components for your tool window
        JPanel panel = new JPanel();
        JButton myButton = new JButton("[B]");

        // Add action listener to the button
        myButton.addActionListener(e -> {
            // Handle button click actions here
            JOptionPane.showMessageDialog(null, "Button clicked!");
        });

        panel.add(myButton);

        // Create content for the tool window
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }




}
