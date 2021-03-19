package com.snaplogic.events;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.chat.ChatGetPermalinkResponse;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.reactions.ReactionsAddResponse;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.model.event.ReactionAddedEvent;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class SlackEvents {

    // @Bean
    // public AppConfig loadDefaultConfig() {
    // return AppConfig.builder().build();
    // }

    @Bean
    public AppConfig loadAppConfig() {
        AppConfig config = new AppConfig();
        ClassLoader classLoader = SlackEvents.class.getClassLoader();
        // src/main/resources/appConfig.json
        try (InputStream is = classLoader.getResourceAsStream("appConfig.json");
                InputStreamReader isr = new InputStreamReader(is)) {
            String json = new BufferedReader(isr).lines().collect(joining());
            JsonObject j = new Gson().fromJson(json, JsonElement.class).getAsJsonObject();
            config.setSigningSecret(j.get("signingSecret").getAsString());
            config.setSingleTeamBotToken(j.get("singleTeamBotToken").getAsString());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return config;
    }

    @Bean
    public App initSlackApp(AppConfig config) {
        App app = new App(config);
        app.command("/hello", (req, ctx) -> {
            return ctx.ack(r -> r.text("Thanks!"));
        });

        app.event(ReactionAddedEvent.class, (payload, ctx) -> {
            ReactionAddedEvent event = payload.getEvent();
            if (event.getReaction().equals("white_check_mark")) {
                ChatPostMessageResponse message = ctx.client().chatPostMessage(r -> r
                        .channel(event.getItem().getChannel()).threadTs(event.getItem().getTs())
                        .text("<@" + event.getUser()
                                + "> Thank you! We greatly appreciate your efforts :two_hearts:"));
                if (!message.isOk()) {
                    ctx.logger.error("chat.postMessage failed: {}", message.getError());
                }
            }
            return ctx.ack();
        });

        String notificationChannelId = "D01H19PDDFF";

        // check if the message contains some monitoring keywords
        Pattern sdk = Pattern.compile(".*[(Java SDK)|(Bolt)|(slack\\-java\\-sdk)].*",
                Pattern.CASE_INSENSITIVE);
        app.message(sdk, (payload, ctx) -> {
            MessageEvent event = payload.getEvent();
            payload.getAuthedUsers();
            String text = event.getText();
            MethodsClient client = ctx.client();
            String emailBody = payload.getEvent().getUser()
                    + " has posted " + payload.getEvent().getText() + " to channel "
                    + payload.getEvent().getChannel();
            // Add 👀reacji to the message
            String channelId = event.getChannel();
            String ts = event.getTs();
            ReactionsAddResponse reaction = client
                    .reactionsAdd(r -> r.channel(channelId).timestamp(ts).name("raised_hands"));
            if (!reaction.isOk()) {
                ctx.logger.error("reactions.add failed: {}", reaction.getError());
            }

            // Send the message to the SDK author
            ChatGetPermalinkResponse permalink = client
                    .chatGetPermalink(r -> r.channel(channelId).messageTs(ts));
            if (permalink.isOk()) {
                ChatPostMessageResponse message = client.chatPostMessage(r -> r
                        .channel(notificationChannelId)
                        .text("The Java SDK might be mentioned:\n" + permalink.getPermalink())
                        .unfurlLinks(true));
                sendEmail(emailBody, permalink.getPermalink());
                if (!message.isOk()) {
                    ctx.logger.error("chat.postMessage failed: {}", message.getError());
                }
            } else {
                ctx.logger.error("chat.getPermalink failed: {}", permalink.getError());
            }
            return ctx.ack();
        });
        return app;
    }

    @Autowired
    private JavaMailSender javaMailSender;

    void sendEmail(String emailBody, String subject) {

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("aparikh@snaplogic.com");

        msg.setSubject("SLACK EVENT");
        msg.setText(emailBody + ". Please check URL : " + subject);

        javaMailSender.send(msg);
    }

}
