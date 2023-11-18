package com.github.bucketoverflow.codebook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import com.intellij.openapi.actionSystem.AnAction;

public class CodebookAction extends AnAction {

    private final String apiKey = "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";

    private final String guideLines = """
            Comment the code according to the following guidelines:
            1. Each line of code must be commented.
            2. Comments happen only using the /* */ tags. No other type of comments are allowed.
            3. Comments happen only to the right of the code line. Not before, not after.
            4. Comments comment the code in the sense of the logic of the program. They reflect semantics, not syntax. A reader must understand the meaning behind the code, not directly the code itself.""";

    private final String exampleCode = """
            public static void main(String[] args) {
            System.out.println("Hello world!");
            }
            """;

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * This default constructor is used by the IntelliJ Platform framework to instantiate this class based on plugin.xml
     * declarations. Only needed in {@link CodebookAction} class because a second constructor is overridden.
     *
     * @see AnAction#AnAction()
     */
    public CodebookAction() {
        super();
    }

    /**
     * This constructor is used to support dynamically added menu actions.
     * It sets the text, description to be displayed for the menu item.
     * Otherwise, the default AnAction constructor is used by the IntelliJ Platform.
     *
     * @param text        The text to be displayed as a menu item.
     * @param description The description of the menu item.
     * @param icon        The icon to be used with the menu item.
     */
    public CodebookAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project currentProject = event.getProject();

        // Document currentDoc = FileEditorManager.getInstance(currentProject).getSelectedTextEditor().getDocument();
        // VirtualFile currentFile = FileDocumentManager.getInstance().getFile(currentDoc);

        var editor = event.getData(CommonDataKeys.EDITOR);

        String selectedCode, doc = "";
        if(currentProject != null && editor != null )
        {
            selectedCode = editor.getSelectionModel().getSelectedText();
            doc = editor.getDocument().getText();
            var fmanager = FileEditorManager.getInstance(currentProject);


            var completableFuture = CreateOpenAIRequest2(currentProject, doc);

            var project_basePath = currentProject.getBasePath();
            VirtualFile vFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
            String fileName = vFile != null ? vFile.getName() : null;
            System.out.println(project_basePath + " " + fileName);

            completableFuture.thenAccept(res -> OpenVirtualFileInEditor(res, project_basePath, fileName, currentProject));
        }

//        Messages.showMessageDialog(
//                currentProject,
//                responseContent,
//                "ChatGPT Response",
//                Messages.getInformationIcon());
    }

    @Override
    public void update(AnActionEvent e) {
        // Set the availability based on whether a project is open
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

    private CompletableFuture<HttpResponse<String>> CreateOpenAIRequest(Project project, String requestContent)
    {
        // ChatGPT API endpoint
        URI chatGPTApiUri = URI.create("https://api.openai.com/v1/chat/completions");

        // Sample prompt for ChatGPT
        String prompt = "Summaryze this code segment";

        // Build the request body
        String requestBody = "{\"model\": " +
                "\"gpt-3.5-turbo\", " +
                "\"messages\": [{\"role\": \"system\", \"content\": \"" + requestContent + "\"}, " +
                "{\"role\": \"user\", \"content\": \"" + prompt + "\"}]}";

        // Create an HttpClient
        HttpClient httpClient = HttpClient.newHttpClient();

        // Create an HttpRequest
        HttpRequest request = HttpRequest.newBuilder()
                .uri(chatGPTApiUri)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Send the request asynchronously
        CompletableFuture<HttpResponse<String>> responseFuture = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        // Attach a callback to handle the response when it's available
        responseFuture.thenAccept(response -> ResponseCallback(response, project));

        return responseFuture;
    }

    private CompletableFuture<HttpResponse<String>> CreateOpenAIRequest2(Project project, String requestContent)
    {
        System.out.println(requestContent);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode requestJson = objectMapper.createObjectNode()
                .put("model", "gpt-4")
                .set("messages", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode()
                                .put("role", "system")
                                .put("content", guideLines))
                        .add(objectMapper.createObjectNode()
                                .put("role", "user")
                                .put("content", requestContent)));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
                .build();

        return client.sendAsync(request, BodyHandlers.ofString());
                //.thenApply(HttpResponse::body);
    }

    private void ResponseCallback(HttpResponse<String> response, Project project)
    {
        int statusCode = response.statusCode();
        String responseBody = response.body();
        String responseContent = ProcessResponseBody(responseBody);

        // Handle the response as needed
        System.out.println("Status Code: " + statusCode);
        System.out.println("Response Body: " + responseBody);

        Messages.showMessageDialog(
                project,
                responseContent,
                "ChatGPT Response 2",
                Messages.getInformationIcon());
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private String ProcessResponseBody(String httpResponse)
    {
//        var stringreader = new StringReader(httpResponse);
//        var bufferedStringReader = new BufferedReader(new StringReader(httpResponse));
//        var responseContent = new StringBuilder();

        var regex = "\"content\":\\s*\"(.*?)\"";
        var pattern = Pattern.compile(regex);
        var matcher = pattern.matcher(httpResponse);

        if(matcher.find())
        {
            var matchedSubstring = matcher.group(1);
            return matchedSubstring;
        }
        else
            return "no response";

//        int startIndex = httpResponse.indexOf("content");
//        var substring = httpResponse.substring(startIndex+9);
//        startIndex = substring.indexOf("\"\"");
//        substring = substring.substring(0, startIndex);

//        while (true)
//        {
//            try
//            {
//                var line = bufferedStringReader.readLine();
//                bufferedStringReader.
//                var reachedEndOfFile = line == null;
//                if (reachedEndOfFile)
//                    break;
//
//                if (line.contains("content"))
//                {
//                    responseContent.append(line);
//                    bufferedStringReader.close();
//                    break;
//                }
//
//            }
//            catch (IOException exception)
//            {
//                exception.printStackTrace();
//            }
//        }

        // return substring;
    }

    private void OpenVirtualFileInEditor(HttpResponse<String> response, String basePath, String filename, Project currentProject)
    {
        System.out.println("thenAccept: trying to open file");
        var fullFileName = basePath+"\\"+filename;
        System.out.println(fullFileName);
        var responseBody = response.body();
        System.out.println(response);
        var responseContent = ProcessResponseBody(responseBody);
        System.out.println(responseContent);

//        try {
//            BufferedWriter writer = new BufferedWriter(new FileWriter(fullFileName));
//            writer.write(responseContent);
//            writer.close();
//        }
//        catch (IOException exception)
//        {
//            System.out.println(exception);
//        }

        var fileEditorManager = FileEditorManager.getInstance(currentProject);
//        var vFile = FilenameIndex.getVirtualFilesByName(fullFileName, GlobalSearchScope.projectScope(currentProject));
        var psiFile = PsiFileFactory.getInstance(currentProject).createFileFromText(fullFileName, Language.ANY, responseContent );

        var descriptor = new OpenFileDescriptor (currentProject, psiFile.getVirtualFile());
        fileEditorManager.openTextEditor(descriptor, true);
    }
}
