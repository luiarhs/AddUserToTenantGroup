package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;

import com.microsoft.graph.auth.confidentialClient.ClientCredentialProvider;
import com.microsoft.graph.auth.enums.NationalCloud;
import com.microsoft.graph.models.extensions.DirectoryObject;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;

import com.google.gson.GsonBuilder;

import java.util.List;

/**
 * Azure Functions with Azure Storage Queue trigger.
 */
public class Function {

    ClientCredentialProvider authProvider = new ClientCredentialProvider(System.getenv("clientId"),
            List.of(System.getenv("scope")), System.getenv("secret"), System.getenv("tenantId"), NationalCloud.Global);

    IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();

    @FunctionName("AddUserToGroup")
    public void run(
        @QueueTrigger(name = "message", queueName = "b2cgroupassign", connection = "AzureWebJobsStorage") String message,
        final ExecutionContext context) {

        context.getLogger().info("Function initialized");


        var builder = new GsonBuilder();
        builder.setPrettyPrinting();

        var gson = builder.create();

        final var userId = gson.fromJson(message, Message.class).getUserId();
        final var groupId = gson.fromJson(message, Message.class).getGroupId();

        if (userId == null)
            context.getLogger().warning("Null value for userId");
        else if (graphClient == null)
            context.getLogger().warning("Unable to initialize graph client");
        else {
            context.getLogger().info("Processing user with userId " + userId);

            var directoryObject = new DirectoryObject();
            directoryObject.id = userId;

            var post = graphClient.groups(groupId).members().references().buildRequest()
                    .post(directoryObject);

            context.getLogger().info("Graph client response: " + post.toString());
        }
    }
}

class Message {
    private String userId;
    private String groupId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String id) {
        this.userId = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String id) {
        this.groupId = id;
    }
}
