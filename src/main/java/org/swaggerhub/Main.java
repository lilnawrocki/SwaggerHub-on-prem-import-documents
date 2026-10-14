package org.swaggerhub;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);
        String host;
        String apiKey;

        System.out.println("Enter admin API key");
        apiKey = input.nextLine();
        System.out.println("Enter host with protocol. Example: https://api.swaggerhub.com");
        host = input.nextLine().toLowerCase();
        input.close();

        String currentPath = System.getProperty("user.dir");

        File mainPathFile = new File(currentPath);
        String[] mainDirectories = mainPathFile.list();
        if (!Arrays.toString(mainDirectories).contains("SwaggerHub-Downloads"))
        {
            System.out.println("Couldn't find SwaggerHub-Download directory.");
            return;
        }
        String backupPath = currentPath + "\\SwaggerHub-Downloads";
        File backupPathFile = new File(backupPath);
        String[] backupDirectories = backupPathFile.list();
        assert backupDirectories != null;

        HttpClient httpClient = HttpClient.newHttpClient();

        for (String s : backupDirectories){

            CreateDocument(host, apiKey, backupPath, httpClient, s, "apis");
            CreateDocument(host, apiKey, backupPath, httpClient, s, "domains");
            CreateDocument(host, apiKey, backupPath, httpClient, s, "templates");
        }
    }
    public static void CreateDocument(String host, String apiKey, String backupPath, HttpClient httpClient, String organization, String type) {
        String tempPath = String.format("%s\\%s", backupPath, organization);
        File tempPathFile = new File(tempPath);
        String[] documentTypeDirectories = tempPathFile.list();

        if (Arrays.toString(documentTypeDirectories).contains(type)) {

            tempPath = String.format("%s\\%s\\%s", backupPath, organization, type);
            File documentDirectory = new File(tempPath);
            File[] documentFiles = documentDirectory.listFiles();
            List<String> documentFileNames = new ArrayList<String>();
            List<String> documentNames = new ArrayList<String>();
            List<String> documentVersions = new ArrayList<String>();

            assert documentFiles != null;
            for (File file : documentFiles) {
                documentFileNames.add(file.getName());
                documentNames.add(file.getName().split("-version_")[0]);
                documentVersions.add(file.getName().split("-version_")[1].split(".yaml")[0]);
            }

            for (int i = 0; i < documentFileNames.size(); i++) {
                String URL = String.format("%s/v1/%s/%s/%s?isPrivate=true&version=%s",
                        host, type, organization, documentNames.get(i), documentVersions.get(i));
                Path filePath = Path.of(documentFiles[i].getAbsolutePath());
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(URL))
                            .headers(
                                    "Content-Type", "application/yaml",
                                    "Authorization", apiKey
                            )
                            .POST(HttpRequest.BodyPublishers.ofFile(filePath))
                            .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200 || response.statusCode() == 201)
                    {
                        System.out.println("Successfully created document: "
                                + documentNames.get(i)
                                + " version "
                                + documentVersions.get(i)
                                + " in "
                                + organization
                                + " organization."
                        );
                    }
                    else
                    {
                        System.out.println(response.body());
                    }
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        httpClient.close();
    }
}
