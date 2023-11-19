package SidebarButton;

import com.github.bucketoverflow.codebook.CodebookAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import kotlinx.html.B;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import com.github.bucketoverflow.codebook.*;

import java.awt.*;
import java.awt.event.ActionEvent;


public class CodebookButtonBuilder implements ToolWindowFactory{

    private JButton analyzeButton;
    private JButton yesButton;
    private JButton noButton;


    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Create UI components for your tool window


        /*
        JPanel panel = new JPanel();

        analyzeButton = new JButton("Create Codebook");
        yesButton = new JButton("Yes");
        noButton = new JButton("No");
        JLabel label = new JLabel("Replace your current files?");

        yesButton.setEnabled(false);
        noButton.setEnabled(false);

        var codebookAction = createCodebookAction();

        analyzeButton.addActionListener(this::analyzeFile);
        yesButton.addActionListener(e -> replaceFile());
        noButton.addActionListener(e -> discardGeneratedFiles(label));


        panel.add(BorderLayout.EAST,yesButton);
        panel.add(BorderLayout.WEST,noButton);
        panel.add(BorderLayout.NORTH,label);

         */
                JPanel currentPanel = setUpAnalyseButton(project);
                putPanelInToolWindow(currentPanel, toolWindow);


        }

        private JPanel setUpAnalyseButton(Project project) {
            JPanel panel = new JPanel(new BorderLayout());
            JPanel ButtonPanel = new JPanel();
            JPanel labelPanel = new JPanel();

            JButton analayzeButton = new JButton("Create Codebook");
            analayzeButton.addActionListener(this::analyzeFile);
            ButtonPanel.add(analayzeButton, BorderLayout.CENTER);

            JLabel analyzeLabel = new JLabel("Click Button to analyze");
            labelPanel.add(analyzeLabel,BorderLayout.CENTER);


            panel.add(ButtonPanel,BorderLayout.CENTER);
            panel.add(labelPanel,BorderLayout.NORTH);

            return panel;
        }
        private JPanel setUpWaitingLabel () {
            JPanel panel = new JPanel(new BorderLayout());
            JLabel waiting = new JLabel("Analyzing...");

            panel.add(waiting, BorderLayout.CENTER);

            return panel;

        }
        private JPanel setUpChoiceButtons () {
            // Create UI components for your tool window
            JPanel panel = new JPanel(new BorderLayout());
            //panel.setLayout(new BorderLayout(0, 20));
            JPanel ButtonPanel = new JPanel();
            JPanel labelPanel = new JPanel();
            JButton yesButton = new JButton("Yes");
            yesButton.addActionListener(e -> replaceFile());
            JLabel label = new JLabel("Replace your current files?");
            JButton noButton = new JButton("No");
            noButton.addActionListener(e -> discardGeneratedFiles(label));
            //JPanel centralPanel = new JPanel();

            labelPanel.add(label,BorderLayout.CENTER);
            panel.add(labelPanel, BorderLayout.NORTH);
            ButtonPanel.add(yesButton,BorderLayout.WEST);
            ButtonPanel.add(noButton,BorderLayout.EAST);
            panel.add(ButtonPanel, BorderLayout.CENTER);

        panel.add(BorderLayout.WEST,yesButton);
        panel.add(BorderLayout.EAST,noButton);
        panel.add(BorderLayout.NORTH,label);
        panel.add(BorderLayout.SOUTH, analyzeButton);
            return panel;
        }

        private void putPanelInToolWindow (JPanel panel, ToolWindow toolWindow) {
            // Create content for the tool window
            ContentFactory contentFactory = ContentFactory.getInstance();
            Content content = contentFactory.createContent(panel, "", false);
            toolWindow.getContentManager().addContent(content);
        }

        private void analyzeFile(@NotNull ActionEvent event)
        {
            analyzeButton.setEnabled(false);
            var project = ProjectManager.getInstance().getOpenProjects()[0];
            var fileEditor = FileEditorManager.getInstance(project).getSelectedEditor();
            var editor = ((TextEditor) fileEditor );

            assert fileEditor != null;
            var file = fileEditor.getFile();

            var codebookAction = createCodebookAction();
            codebookAction.actionPerformed(project, editor.getEditor(), file, this);
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

        private CodebookAction createCodebookAction()
        {
            return new CodebookAction("Codebook Analysis", "Run Codebook analysis", CodebookIcons.Sdk_default_icon);
        }
    }

