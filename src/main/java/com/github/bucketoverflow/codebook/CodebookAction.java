package com.github.bucketoverflow.codebook;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import com.intellij.openapi.actionSystem.AnAction;

public class CodebookAction extends AnAction {

    private final String apiKey = "insert_key_here";
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

        }

        var completableFuture = CreateOpenAIRequest(currentProject, doc);

//        var message = "Sending request to ChatGPT...";
//        String title = event.getPresentation().getDescription();
//        Messages.showMessageDialog(
//                currentProject,
//                message,
//                title,
//                Messages.getInformationIcon());

        var response = completableFuture.join();

        int statusCode = response.statusCode();
        var responseBody = response.body();

        var responseContent = ProcessResponseBody(responseBody);

        Messages.showMessageDialog(
                currentProject,
                responseContent,
                "ChatGPT Response",
                Messages.getInformationIcon());
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
                "\"gpt-4\", " +
                "\"messages\": [{\"role\": \"system\", \"content\": \"" + requestContent + "}\"}, " +
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
}
