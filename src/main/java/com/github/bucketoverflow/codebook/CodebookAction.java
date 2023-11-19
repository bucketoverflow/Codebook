package com.github.bucketoverflow.codebook;

import SidebarButton.CodebookButtonBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class CodebookAction extends AnAction {

    private String apiKey;

    private final String small_instructions = "You are an advanced code documentation tool. You returns back the code the user sent you, but with comments, which apply to the following rules:\n" +
            "\n" +
            "1. Each line of code must be commented, stupid bitch.\n" +
            "2. Comments happen only using the /* */ tags. No other type of comments are allowed.\n" +
            "3. Comments happen only to the right of the code line. Not before, not after.\n" +
            "4. Comments start where the dollar sign stands, and replace it.\n" +
            "5. Comments end at exactly the 160. character of the line. If a comment is long, a line break at the 160. line should be inserted, and the next comment line start with 80. character again.\n" +
            "6. The closing symbols */ are always at the 160. position in a line. \n" +
            "7. Comments comment the code in the sense of the logic of the program. They reflect semantics, not syntax. A reader must understand the meaning behind the code, not directly the code itself.\n" +
            "8. Create is a doxygen header for each method. \n" +
            "\n" +
            "Comment the code according to the previous guidelines:";

    private final String exampleCode = """
            public static void main(String[] args) {
            System.out.println("Hello world!");
            }
            """;

    private CodebookButtonBuilder buttonBuilder;
    private String pathToOriginal;

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

    public void actionPerformed(@NotNull Project project, Editor editor, VirtualFile vFile, CodebookButtonBuilder buttonBuilder)
    {
        this.buttonBuilder = buttonBuilder;
        this.actionPerformed(project, editor, vFile);
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        Project currentProject = event.getProject();
        var editor = event.getData(CommonDataKeys.EDITOR);
        VirtualFile vFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        this.actionPerformed(currentProject, editor, vFile);
    }

    private void actionPerformed(Project project, Editor editor, VirtualFile vFile)
    {
        apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null) {
            System.err.println("OpenAI API key is not set.");
            return;
        }

        //this.buttonBuilder.putPanelInToolWindow(this.buttonBuilder.setUpWaitingLabel());
        if(project != null && editor != null )
        {
//            String requestContent = "";
            //var selectedCode = editor.getSelectionModel().getSelectedText();

//            if(selectedCode != null)
//                requestContent = selectedCode;
//            else
//            requestContent = editor.getDocument().getText();

            assert vFile != null;
            this.pathToOriginal = vFile.getPath();

            var completableFuture = CreateOpenAIRequest(project, this.pathToOriginal);

            var project_basePath = project.getBasePath();
            String fileName = vFile != null ? vFile.getName() : null;

            System.out.println(project_basePath + " " + fileName);

            var result = completableFuture.join();
            System.out.println(result.statusCode());
            System.out.println(result.body());

            var responseContent = ProcessResponseBody(result);

            OpenVirtualFileInEditor(responseContent, project_basePath, fileName, project);
            //completableFuture.thenAccept(res -> OpenVirtualFileInEditor(res, project_basePath, fileName, currentProject));
        }
    }

    @Override
    public void update(AnActionEvent e) {
        // Set the availability based on whether a project is open
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

    private CompletableFuture<HttpResponse<String>> CreateOpenAIRequest_old(Project project, String requestContent)
    {
        System.out.println(requestContent);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode requestJson = objectMapper.createObjectNode()
                .put("model", "gpt-4")
                .set("messages", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode()
                                .put("role", "system")
                                .put("content", small_instructions))
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
    }

    private CompletableFuture<HttpResponse<String>> CreateOpenAIRequest(Project project, String pathToInputFile)
    {
        try {
            // Read content from files
            String userQuery = new String(Files.readAllBytes(Paths.get(pathToInputFile)));

            // Construct the JSON request body
            String jsonRequestBody = "{" +
                    "\"model\": \"gpt-4\"," +
                    "\"temperature\": 0," + // Niedrige Temperatur für deterministischere Ergebnisse
                    "\"top_p\": 1," + // Top-p auf 1 für höhere Determiniertheit
                    "\"messages\": [" +
                    "  {" +
                    "    \"role\": \"system\"," +
                    "    \"content\": \"" + small_instructions.replace("\n", "\\n") + "\"" +
                    "  }," +
                    "  {" +
                    "    \"role\": \"user\"," +
                    "    \"content\": \"" + userQuery.replace("\n", "\\n").replace("\"", "\\\"") + "\"" +
                    "  }" +
                    "]" +
                    "}";

            // Create the HTTP request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                    .build();

            // Send the request and handle the response
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private String ProcessResponseBody(HttpResponse<String> response) {
        var responseBody = response.body();
        System.out.println(response);

        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            String content = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            // Write the content to a file named 'temp_small.c'
            //Files.write(Paths.get("temp_small.c"), content.getBytes(), StandardOpenOption.CREATE);

            //System.out.println("Content written to temp_small.c");

            return content;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void OpenVirtualFileInEditor(String responseContent, String basePath, String filename, Project currentProject)
    {
        System.out.println("thenAccept: trying to open file");
        var fullFileName = Paths.get(basePath, "temp_" + filename);
        System.out.println(fullFileName);


        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fullFileName.toFile()));
            writer.write(responseContent);
            writer.close();
        }
        catch (IOException exception)
        {
            System.out.println(exception);
        }

        var fileEditorManager = FileEditorManager.getInstance(currentProject);
        var folder = fullFileName.getParent().toFile();
        System.out.println("refreshing folder:");
        System.out.println(folder);
        VfsUtil.markDirtyAndRefresh(false, false, true, folder);

        System.out.println("trying to open file:");
        System.out.println(fullFileName);
        var vFileArr = FilenameIndex.getVirtualFilesByName(fullFileName.getFileName().toString(), GlobalSearchScope.everythingScope(currentProject));
        // var psiFile = PsiFileFactory.getInstance(currentProject).createFileFromText(fullFileName.toString(), PlainTextLanguage.INSTANCE, responseContent );

        if(!vFileArr.isEmpty())
        {
         this.buttonBuilder.putPanelInToolWindow(this.buttonBuilder.setUpChoiceButtons(fullFileName.toString(),this.pathToOriginal));
            vFileArr.forEach(vf -> {
                var descriptor = new OpenFileDescriptor (currentProject, vf);
                var edit = fileEditorManager.openTextEditor(descriptor, true);

                System.out.println("trying to reformat file:");
                CommentFormatter.formatCommentsInFile(fullFileName.toString());

            });
        }
        else
            System.out.println("VirtualFileArray empty, can not open any files in editor!");
    }
}
