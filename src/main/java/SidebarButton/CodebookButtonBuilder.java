package SidebarButton;

import com.github.bucketoverflow.codebook.CodebookAction;
import com.github.bucketoverflow.codebook.CodebookIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class CodebookButtonBuilder implements ToolWindowFactory{

    private JButton analyzeButton;
    private JButton yesButton;
    private JButton noButton;
    private ToolWindow classToolWindow;
    private Project currentProject;


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
        classToolWindow = toolWindow;
        currentProject = project;
                JPanel currentPanel = setUpAnalyseButton(project);
                putPanelInToolWindow(currentPanel);


        }

        public JPanel setUpAnalyseButton(Project project) {
            this.classToolWindow.getContentManager().removeAllContents(true);
            JPanel panel = new JPanel(new BorderLayout());
            JPanel ButtonPanel = new JPanel();
            JPanel labelPanel = new JPanel();

            this.analyzeButton = new JButton("Create Codebook");
            this.analyzeButton.addActionListener(e -> {
                putPanelInToolWindow(setUpWaitingLabel());
                analyzeFile(e);
            });
            ButtonPanel.add(this.analyzeButton, BorderLayout.CENTER);

            JLabel analyzeLabel = new JLabel("Click Button to analyze");
            labelPanel.add(analyzeLabel,BorderLayout.CENTER);


            panel.add(ButtonPanel,BorderLayout.CENTER);
            panel.add(labelPanel,BorderLayout.NORTH);


            return panel;
        }
        public JPanel setUpWaitingLabel () {
            this.classToolWindow.getContentManager().removeAllContents(true);
            JPanel panel = new JPanel(new BorderLayout());
            JLabel waiting = new JLabel("Analyzing...");

            panel.add(waiting, BorderLayout.CENTER);

            return panel;

        }
        public JPanel setUpChoiceButtons (String pathToOldFile, String pathToNewFile) {
            // Create UI components for your tool window
            this.classToolWindow.getContentManager().removeAllContents(true);
            JPanel panel = new JPanel(new BorderLayout());
            //panel.setLayout(new BorderLayout(0, 20));
            JPanel ButtonPanel = new JPanel();
            JPanel labelPanel = new JPanel();



            this.yesButton = new JButton("Yes");
            JLabel label = new JLabel("Replace your current files?");
            yesButton.addActionListener(e -> {
                replaceFile(pathToOldFile, pathToNewFile);
                try {
                    discardGeneratedFiles(label,pathToOldFile);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

            this.noButton = new JButton("No");
            noButton.addActionListener(e -> {
                try {
                    discardGeneratedFiles(label, pathToOldFile);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            });
            //JPanel centralPanel = new JPanel();

            labelPanel.add(label,BorderLayout.CENTER);
            panel.add(labelPanel, BorderLayout.NORTH);
            //noButtonPanel.add(yesButton,BorderLayout.CENTER);
            //yesButtonPanel.add(noButton,BorderLayout.CENTER);
            ButtonPanel.add(yesButton,BorderLayout.WEST);
            ButtonPanel.add(noButton,BorderLayout.EAST);
            panel.add(ButtonPanel, BorderLayout.CENTER);

        panel.add(BorderLayout.CENTER,ButtonPanel);
        panel.add(BorderLayout.NORTH,labelPanel);

            return panel;
        }

        public void putPanelInToolWindow (JPanel panel) {
            // Create content for the tool window
            ContentFactory contentFactory = ContentFactory.getInstance();
            Content content = contentFactory.createContent(panel, "", false);
            this.classToolWindow.getContentManager().addContent(content);
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

        private void replaceFile (String pathToOldFile, String pathToNewFile) {
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    // Load the content of the source file

                    String content = FileUtil.loadFile(new File(pathToOldFile));

                    // Overwrite the target file with the content of the source file
                    FileUtil.writeToFile(new File(pathToNewFile), content);

                    // Refresh the Virtual File System to reflect changes in IntelliJ IDEA
                    VirtualFile sourceFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(pathToNewFile));
                    VirtualFile targetFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(pathToOldFile));

                    // Notify the IDE that the files have been modified
                    if (sourceFile != null) {
                        sourceFile.refresh(false, false);
                    }
                    if (targetFile != null) {
                        targetFile.refresh(false, false);
                    }

                    // Save changes to the target file
                    FileDocumentManager.getInstance().saveDocument(FileDocumentManager.getInstance().getDocument(targetFile));

                    // Notify the file system that changes have been made
                    VirtualFileManager.getInstance().syncRefresh();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        private void discardGeneratedFiles (JLabel label, String pathToOldFile) throws IOException {
            label.setText("Files got discarded");

            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(new File(pathToOldFile));
            FileEditorManager.getInstance(currentProject).closeFile(virtualFile);

            // Delete the file
            try {
                Files.delete(Paths.get(pathToOldFile));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Refresh the Virtual File System to reflect changes in IntelliJ IDEA
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(pathToOldFile));

            // Notify the file system that changes have been made
            VirtualFileManager.getInstance().syncRefresh();
            this.putPanelInToolWindow(setUpAnalyseButton(this.currentProject));

        }

        private CodebookAction createCodebookAction()
        {
            return new CodebookAction("Codebook Analysis", "Run Codebook analysis", CodebookIcons.Sdk_default_icon);
        }
    }

